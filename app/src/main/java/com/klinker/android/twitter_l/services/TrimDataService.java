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
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.utils.IOUtils;

import java.util.Calendar;
import java.util.Date;

public class TrimDataService extends SimpleJobService {

    public static final String JOB_TAG = "trim-data";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void scheduleRefresh(Context context) {
        int secondsUntilThreeAm = secondsUntilThreeAm(new Date().getTime());

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job myJob = dispatcher.newJobBuilder()
                .setService(TrimDataService.class)
                .setTag(JOB_TAG)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(secondsUntilThreeAm, 15 * secondsUntilThreeAm))
                .setConstraints(Constraint.DEVICE_CHARGING)
                .setReplaceCurrent(true)
                .build();

        dispatcher.mustSchedule(myJob);
    }

    protected static int secondsUntilThreeAm(long currentTime) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(currentTime));

        // force the calendar to 3 in the morning, on the next day.
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 3);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long threeAm = calendar.getTimeInMillis();

        return (int) (threeAm - currentTime) / 1000;
    }

    @Override
    public int onRunJob(JobParameters parameters) {
        Log.v("trimming_database", "trimming database from service");
        IOUtils.trimDatabase(getApplicationContext(), 1); // trims first account
        IOUtils.trimDatabase(getApplicationContext(), 2); // trims second account

        getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        return 0;
    }
}
