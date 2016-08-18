package com.klinker.android.twitter_l.receivers;
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.data.ScheduledTweet;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.services.*;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.ActivityFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.ListFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.MentionsFragment;
import com.klinker.android.twitter_l.activities.scheduled_tweets.ViewScheduledTweets;

import java.util.ArrayList;
import java.util.Date;

public class BootReceiver extends BroadcastReceiver {

    private Context context;
    private SharedPreferences sharedPrefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        AppSettings settings = AppSettings.getInstance(context);

        DataCheckService.scheduleRefresh(context);
        TimelineRefreshService.scheduleRefresh(context);
        TrimDataService.scheduleRefresh(context, 1);
        new MentionsRefreshService().scheduleRefresh(context);
        new DirectMessageRefreshService().scheduleRefresh(context);
        new ListRefreshService().scheduleRefresh(context);
        new ActivityRefreshService().scheduleRefresh(context);

        if (settings.pushNotifications) {
            context.startService(new Intent(context, CatchupPull.class));
        }

        ArrayList<ScheduledTweet> tweets = QueuedDataSource.getInstance(context).getScheduledTweets();

        for (ScheduledTweet s : tweets) {
            Intent serviceIntent = new Intent(context.getApplicationContext(), SendScheduledTweet.class);

            Log.v("talon_scheduled_tweets", "in boot text: " + s.text);

            serviceIntent.putExtra(ViewScheduledTweets.EXTRA_TEXT, s.text);
            serviceIntent.putExtra("account", s.account);
            serviceIntent.putExtra("alarm_id", s.alarmId);

            PendingIntent pi = getDistinctPendingIntent(serviceIntent, s.alarmId);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            am.set(AlarmManager.RTC_WAKEUP,
                    s.time,
                    pi);
        }

    }

    protected PendingIntent getDistinctPendingIntent(Intent intent, int requestId) {
        PendingIntent pi =
                PendingIntent.getService(
                        context,
                        requestId,
                        intent,
                        0);

        return pi;
    }


}
