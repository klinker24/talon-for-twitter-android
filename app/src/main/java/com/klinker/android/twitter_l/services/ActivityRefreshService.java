package com.klinker.android.twitter_l.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ActivityUtils;

public class ActivityRefreshService extends SimpleJobService {

    public static final String JOB_TAG = "activity-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.activityRefresh / 1000; // convert to seconds

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (settings.activityRefresh != 0) {
            Job myJob = dispatcher.newJobBuilder()
                    .setService(ActivityRefreshService.class)
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
        AppSettings settings = AppSettings.getInstance(this);
        ActivityUtils utils = new ActivityUtils(this, false);

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification();
        }

        if (settings.syncSecondMentions) {
            Intent second = new Intent(this, SecondActivityRefreshService.class);
            startService(second);
        }

        return 0;
    }
}
