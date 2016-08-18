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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.ActivityFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class DirectMessageRefreshService extends ScheduledService {

    private SharedPreferences sharedPrefs;

    public DirectMessageRefreshService() {
        super("DirectMessageRefreshService");
    }

    @Override
    protected ScheduledService.ScheduleInfo getScheduleInfo(AppSettings settings) {
        return new ScheduledService.ScheduleInfo(DirectMessageRefreshService.class, DMFragment.DM_REFRESH_ID, settings.dmRefresh);
    }

    @Override
    public void handleIntent(Intent intent) {
        sharedPrefs = AppSettings.getSharedPreferences(this);

        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        boolean update = false;
        int numberNew = 0;

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
                startService(new Intent(context, SecondDMRefreshService.class));
            }

            sendBroadcast(new Intent("com.klinker.android.twitter.NEW_DIRECT_MESSAGE"));

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}