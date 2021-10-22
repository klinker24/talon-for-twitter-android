package com.klinker.android.twitter_l.services;

import android.content.Context;
import android.net.TrafficStats;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.services.background_refresh.ActivityRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.ListRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.MentionsRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.TimelineRefreshService;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.concurrent.TimeUnit;

public class DataCheckService extends Worker {

    private final Context context;
    public DataCheckService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "data-check-service";

    public  static final int RESTART_INTERVAL = 15 * 60; // 15 mins

    public static final long KB_IN_BYTES = 1024;
    public static final long MB_IN_BYTES = KB_IN_BYTES * 1024;

    public static void scheduleRefresh(Context context) {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(DataCheckService.class, RESTART_INTERVAL, TimeUnit.SECONDS)
                        .setConstraints(new Constraints.Builder()
                                .build())
                        .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        int uid = context.getApplicationInfo().uid;

        long oldMb = App.DATA_USED;

        long sent = TrafficStats.getUidTxBytes(uid) / MB_IN_BYTES;
        long received = TrafficStats.getUidRxBytes(uid) / MB_IN_BYTES;
        long currentMb = sent + received;

        App.DATA_USED = currentMb;

        if (oldMb != 0 && (currentMb - oldMb) > 100) {
            ActivityRefreshService.cancelRefresh(context);
            DirectMessageRefreshService.cancelRefresh(context);
            ListRefreshService.cancelRefresh(context);
            MentionsRefreshService.cancelRefresh(context);
            TimelineRefreshService.cancelRefresh(context);

            android.os.Process.killProcess(android.os.Process.myPid());
        }

        return Result.success();
    }
}
