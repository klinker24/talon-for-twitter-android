package com.klinker.android.twitter_l.services.background_refresh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.services.abstract_services.LimitedRunService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ActivityUtils;
import com.klinker.android.twitter_l.utils.Utils;

public class SecondActivityRefreshService extends SimpleJobService {

    public static void startNow(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job myJob = dispatcher.newJobBuilder()
                .setService(SecondActivityRefreshService.class)
                .setTag("second-activity-refresh-now")
                .setTrigger(Trigger.executionWindow(0,0))
                .build();

        dispatcher.mustSchedule(myJob);
    }

    @Override
    public int onRunJob(JobParameters parameters) {
        AppSettings settings = AppSettings.getInstance(this);
        ActivityUtils utils = new ActivityUtils(this, true);

        if (Utils.getConnectionStatus(this) && !settings.syncMobile) {
            return 0;
        }

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification(ActivityUtils.SECOND_NOTIFICATION_ID);
        }

        return 0;
    }
}
