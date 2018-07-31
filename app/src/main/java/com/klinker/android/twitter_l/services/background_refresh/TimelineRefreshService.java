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
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.services.PreCacheService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.widget.WidgetProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;

public class TimelineRefreshService extends SimpleJobService {

    public static final String JOB_TAG = "home-timeline-refresh";
    public static boolean isRunning = false;

    @Override
    public int onRunJob(JobParameters params) {
        return refresh(this, false);
    }

    public static void cancelRefresh(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.timelineRefresh / 1000; // convert to seconds

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (settings.timelineRefresh != 0) {
            Job myJob = dispatcher.newJobBuilder()
                    .setService(TimelineRefreshService.class)
                    .setTag(JOB_TAG)
                    .setRecurring(true)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(refreshInterval, (5 * 60) + refreshInterval))
                    .setConstraints(settings.syncMobile ? Constraint.ON_ANY_NETWORK : Constraint.ON_UNMETERED_NETWORK)
                    .setReplaceCurrent(true)
                    .build();

            dispatcher.mustSchedule(myJob);
        } else {
            dispatcher.cancel(JOB_TAG);
        }
    }

    public static int refresh(final Context context, boolean onStartRefresh) {

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);
        if (!MainActivity.canSwitch || WidgetRefreshService.isRunning || TimelineRefreshService.isRunning) {
            return 0;
        }

        if (MainActivity.canSwitch) {
            TimelineRefreshService.isRunning = true;

            AppSettings settings = AppSettings.getInstance(context);

            Twitter twitter = Utils.getTwitter(context, settings);

            HomeDataSource dataSource = HomeDataSource.getInstance(context);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            List<twitter4j.Status> statuses = new ArrayList<>();

            boolean foundStatus = false;

            Paging paging = new Paging(1, 200);

            long[] lastId;
            long id;
            try {
                lastId = dataSource.getLastIds(currentAccount);
                id = lastId[1];
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException i) {
                    e.printStackTrace();
                }

                TimelineRefreshService.isRunning = false;
                return 0;
            }

            if (id == 0) {
                id = 1;
            }

            try {
                paging.setSinceId(id);
            } catch (Exception e) {
                paging.setSinceId(1L);
            }

            for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                try {
                    if (!foundStatus) {
                        paging.setPage(i + 1);
                        List<Status> list = twitter.getHomeTimeline(paging);
                        statuses.addAll(list);

                        if (statuses.size() <= 1 || statuses.get(statuses.size() - 1).getId() == lastId[0]) {
                            Log.v("talon_inserting", "found status");
                            foundStatus = true;
                        } else {
                            Log.v("talon_inserting", "haven't found status");
                            foundStatus = false;
                        }

                    }
                } catch (Exception e) {
                    // the page doesn't exist
                    e.printStackTrace();
                    foundStatus = true;
                } catch (OutOfMemoryError o) {
                    // don't know why...
                    o.printStackTrace();
                }
            }

            Log.v("talon_pull", "got statuses, new = " + statuses.size());

            // hash set to check for duplicates I guess
            Set<Status> hs = new HashSet<>();
            hs.addAll(statuses);
            statuses.clear();
            statuses.addAll(hs);

            Log.v("talon_inserting", "tweets after hashset: " + statuses.size());

            lastId = dataSource.getLastIds(currentAccount);

            Long currentTime = Calendar.getInstance().getTimeInMillis();
            if (currentTime - sharedPrefs.getLong("last_timeline_insert", 0L) < 10000) {
                Log.v("talon_refresh", "don't insert the tweets on refresh");
                context.sendBroadcast(new Intent("com.klinker.android.twitter.TIMELINE_REFRESHED").putExtra("number_new", 0));

                TimelineRefreshService.isRunning = false;
                context.getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);
                return 0;
            } else {
                sharedPrefs.edit().putLong("last_timeline_insert", currentTime).apply();
            }

            int inserted = 0;

            try{
                inserted = HomeDataSource.getInstance(context).insertTweets(statuses, currentAccount, lastId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (inserted > 0 && statuses.size() > 0) {
                sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).apply();
            }

            if (!onStartRefresh) {
                sharedPrefs.edit().putBoolean("refresh_me", true).apply();

                if (settings.notifications && (settings.timelineNot || settings.favoriteUserNotifications) && inserted > 0) {
                    NotificationUtils.refreshNotification(context);
                }

                if (settings.preCacheImages) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            PreCacheService.cache(context);
                        }
                    }).start();
                }

                context.sendBroadcast(new Intent("com.klinker.android.twitter.TIMELINE_REFRESHED").putExtra("number_new", inserted));

            } else {
                Log.v("talon_refresh", "sending broadcast to fragment");
                context.sendBroadcast(new Intent("com.klinker.android.twitter.TIMELINE_REFRESHED").putExtra("number_new", inserted));
            }

            WidgetProvider.updateWidget(context);
            context.getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

            TimelineRefreshService.isRunning = false;
        }

        return 0;
    }
}