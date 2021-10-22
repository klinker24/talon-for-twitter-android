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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.services.background_refresh.TimelineRefreshService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class PreCacheService extends Worker {

    private final Context context;
    public PreCacheService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "pre-cache-service";
    public static boolean isRunning = false;

    @NonNull
    @Override
    public Result doWork() {
        cache(context);
        return Result.success();
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = 60 * 60 * 2; // 2 hours, as seconds

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(PreCacheService.class, refreshInterval, TimeUnit.SECONDS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(settings.syncMobile ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                                .build())
                        .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
    }
    private static final boolean DEBUG = false;

    public static void cache(Context context) {
        if (!AppSettings.getInstance(context).preCacheImages) {
            return;
        }

        if (DEBUG) {
            Log.v("talon_pre_cache", "starting the service, current time: " + Calendar.getInstance().getTime().toString());
        }

        AppSettings settings = AppSettings.getInstance(context);
        Cursor cursor = HomeDataSource.getInstance(context).getUnreadCursor(settings.currentAccount);

        try {
            if (cursor.moveToFirst()) {
                if (DEBUG) {
                    Log.v("talon_pre_cache", "found database and moved to first picture. cursor size: " + cursor.getCount());
                }
                boolean cont = true;
                do {
                    String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
                    String imageUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));
                    // image url can contain spaces, which means there are multiple pictures

                    Glide.with(context).load(profilePic).downloadOnly(1000, 1000);
                    Glide.with(context).load(imageUrl).downloadOnly(1000, 1000);

                } while (cursor.moveToNext() && cont);

                if (DEBUG) {
                    Log.v("talon_pre_cache", "done with service. time: " + Calendar.getInstance().getTime().toString());
                }
            }
        } catch (Exception e) {

        }
    }

}
