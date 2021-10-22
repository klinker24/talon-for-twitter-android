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
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class MentionsRefreshService extends Worker {

    private final Context context;
    public MentionsRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "mention-timeline-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(MentionsRefreshService.class).build());
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.mentionsRefresh / 1000; // convert to seconds

        if (settings.mentionsRefresh != 0) {
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(MentionsRefreshService.class, refreshInterval, TimeUnit.SECONDS)
                            .setConstraints(new Constraints.Builder()
                                    .setRequiredNetworkType(settings.syncMobile ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                                    .build())
                            .build();
            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        sharedPrefs = AppSettings.getSharedPreferences(context);

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
                SecondMentionsRefreshService.startNow(context);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        } catch (IllegalArgumentException | SecurityException ignored) {

        }

        return Result.success();
    }
}