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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.services.background_refresh.MentionsRefreshService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.IOUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TrimDataService extends Worker {

    private final Context context;
    public TrimDataService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "trim-data";

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = 60 * 60 * 12; // 12 hours, as seconds

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(TrimDataService.class, refreshInterval, TimeUnit.SECONDS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(settings.syncMobile ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                                .build())
                        .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.v("trimming_database", "trimming database from service");
        IOUtils.trimDatabase(getApplicationContext(), 1); // trims first account
        IOUtils.trimDatabase(getApplicationContext(), 2); // trims second account

        context.getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        return Result.success();
    }
}
