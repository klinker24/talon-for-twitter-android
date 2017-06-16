package com.klinker.android.twitter_l.services;

import android.content.Context;
import android.net.TrafficStats;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.SimpleJobService;
import com.firebase.jobdispatcher.Trigger;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.services.background_refresh.ActivityRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.ListRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.MentionsRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.TimelineRefreshService;

public class DataCheckService extends SimpleJobService {

    public static final String JOB_TAG = "data-check-service";

    public  static final int RESTART_INTERVAL = 15 * 60; // 15 mins

    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;

    public static void scheduleRefresh(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        Job myJob = dispatcher.newJobBuilder()
                .setService(DataCheckService.class)
                .setTag(JOB_TAG)
                .setRecurring(true)
                .setLifetime(Lifetime.FOREVER)
                .setTrigger(Trigger.executionWindow(RESTART_INTERVAL, 2 * RESTART_INTERVAL))
                .setReplaceCurrent(true)
                .build();

        dispatcher.mustSchedule(myJob);
    }

    @Override
    public int onRunJob(JobParameters jobParameters) {
        int uid = getApplicationInfo().uid;

        long oldMb = App.DATA_USED;

        long sent = TrafficStats.getUidTxBytes(uid) / MB_IN_BYTES;
        long received = TrafficStats.getUidRxBytes(uid) / MB_IN_BYTES;
        long currentMb = sent + received;

        App.DATA_USED = currentMb;

        if (oldMb != 0 && (currentMb - oldMb) > 100) {
            ActivityRefreshService.cancelRefresh(this);
            DirectMessageRefreshService.cancelRefresh(this);
            ListRefreshService.cancelRefresh(this);
            MentionsRefreshService.cancelRefresh(this);
            TimelineRefreshService.cancelRefresh(this);

            android.os.Process.killProcess(android.os.Process.myPid());
        }

        return 0;
    }
}
