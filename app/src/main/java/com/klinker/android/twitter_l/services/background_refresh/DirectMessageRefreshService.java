package com.klinker.android.twitter_l.services.background_refresh;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class DirectMessageRefreshService extends SimpleJobService {

    public static final String JOB_TAG = "direct-message-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.dmRefresh / 1000; // convert to seconds

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (settings.dmRefresh != 0) {
            Job myJob = dispatcher.newJobBuilder()
                    .setService(DirectMessageRefreshService.class)
                    .setTag(JOB_TAG)
                    .setRecurring(true)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(refreshInterval, (5 * 60) +  refreshInterval))
                    .setConstraints(settings.syncMobile ? Constraint.ON_ANY_NETWORK : Constraint.ON_UNMETERED_NETWORK)
                    .setReplaceCurrent(true)
                    .build();

            dispatcher.mustSchedule(myJob);
        } else {
            dispatcher.cancel(JOB_TAG);
        }
    }

    public static void startNow(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job myJob = dispatcher.newJobBuilder()
                .setService(DirectMessageRefreshService.class)
                .setTag("dm-refresh-now")
                .setTrigger(Trigger.executionWindow(0,0))
                .build();

        dispatcher.mustSchedule(myJob);
    }

    @Override
    public int onRunJob(JobParameters parameters) {
        sharedPrefs = AppSettings.getSharedPreferences(this);

        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        int numberNew;

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            long lastId = sharedPrefs.getLong("last_direct_message_id_" + currentAccount, 0);
            Paging paging;
            if (lastId != 0) {
                paging = new Paging(1).sinceId(lastId);
            } else {
                paging = new Paging(1, 500);
            }

            List<DirectMessage> dm = twitter.getDirectMessages(paging);
            List<DirectMessage> sent = twitter.getSentDirectMessages(paging);

            if (dm.size() != 0) {
                sharedPrefs.edit().putLong("last_direct_message_id_" + currentAccount, dm.get(0).getId()).apply();
                numberNew = dm.size();
            } else {
                numberNew = 0;
            }

            DMDataSource dataSource = DMDataSource.getInstance(context);
            int inserted = 0;

            for (DirectMessage directMessage : dm) {
                try {
                    dataSource.createDirectMessage(directMessage, currentAccount);
                } catch (Exception e) {
                    dataSource = DMDataSource.getInstance(context);
                    dataSource.createDirectMessage(directMessage, currentAccount);
                }
                inserted++;
            }

            for (DirectMessage directMessage : sent) {
                try {
                    dataSource.createDirectMessage(directMessage, currentAccount);
                } catch (Exception e) {
                    dataSource = DMDataSource.getInstance(context);
                    dataSource.createDirectMessage(directMessage, currentAccount);
                }
            }

            sharedPrefs.edit().putBoolean("refresh_me", true).apply();
            sharedPrefs.edit().putBoolean("refresh_me_dm", true).apply();

            if (settings.notifications && settings.dmsNot && inserted > 0) {
                int currentUnread = sharedPrefs.getInt("dm_unread_" + currentAccount, 0);
                sharedPrefs.edit().putInt("dm_unread_" + currentAccount, numberNew + currentUnread).apply();

                NotificationUtils.refreshNotification(context);
            }

            if (settings.syncSecondMentions) {
                SecondDMRefreshService.startNow(this);
            }

            sendBroadcast(new Intent("com.klinker.android.twitter.NEW_DIRECT_MESSAGE"));

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }

        return 0;
    }
}