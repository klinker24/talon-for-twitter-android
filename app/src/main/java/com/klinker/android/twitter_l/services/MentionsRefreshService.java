package com.klinker.android.twitter_l.services;
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
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class MentionsRefreshService extends SimpleJobService {

    public static final String JOB_TAG = "mention-timeline-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.mentionsRefresh / 1000; // convert to seconds

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (settings.mentionsRefresh != 0) {
            Job myJob = dispatcher.newJobBuilder()
                    .setService(MentionsRefreshService.class)
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

    @Override
    public int onRunJob(JobParameters parameters) {
        sharedPrefs = AppSettings.getSharedPreferences(this);

        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

            long[] lastId = dataSource.getLastIds(currentAccount);
            Paging paging;
            paging = new Paging(1, 200);
            if (lastId[0] > 0) {
                paging.sinceId(lastId[0]);
            }

            List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

            int inserted = MentionsDataSource.getInstance(context).insertTweets(statuses, currentAccount);

            sharedPrefs.edit().putBoolean("refresh_me", true).apply();
            sharedPrefs.edit().putBoolean("refresh_me_mentions", true).apply();

            if (settings.notifications && settings.mentionsNot && inserted > 0) {
                NotificationUtils.refreshNotification(context);
            }

            if (settings.syncSecondMentions) {
                Intent second = new Intent(context, SecondMentionsRefreshService.class);
                startService(second);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }

        return 0;
    }
}