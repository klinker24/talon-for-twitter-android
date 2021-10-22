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

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.api_helper.DirectMessageDownload;

import java.util.concurrent.TimeUnit;

public class DirectMessageRefreshService extends Worker {

    private final Context context;
    public DirectMessageRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "direct-message-refresh";
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(DirectMessageRefreshService.class).build());
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.dmRefresh / 1000; // convert to seconds

        if (settings.dmRefresh != 0) {
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(DirectMessageRefreshService.class, refreshInterval, TimeUnit.SECONDS)
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
        DirectMessageDownload.download(context, false, false);
        return Result.success();
    }
}