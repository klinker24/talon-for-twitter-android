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

import com.bumptech.glide.Glide;
import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.services.background_refresh.TimelineRefreshService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Calendar;

public class PreCacheService extends SimpleJobService {

    public static final String JOB_TAG = "pre-cache-service";
    public static boolean isRunning = false;

    @Override
    public int onRunJob(JobParameters params) {
        cache(this);
        return 0;
    }

    public static void cancelCache(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (settings.preCacheImages) {
            Job myJob = dispatcher.newJobBuilder()
                    .setService(PreCacheService.class)
                    .setTag(JOB_TAG)
                    .setRecurring(false)
                    .setConstraints(AppSettings.getSharedPreferences(context)
                            .getBoolean("pre_cache_wifi_only", false) ? Constraint.ON_UNMETERED_NETWORK : Constraint.ON_ANY_NETWORK)
                    .setTrigger(Trigger.executionWindow(0,0))
                    .setReplaceCurrent(true)
                    .build();

            dispatcher.mustSchedule(myJob);
        } else {
            dispatcher.cancel(JOB_TAG);
        }
    }

    private static final boolean DEBUG = false;

    public static void cache(Context context) {

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
