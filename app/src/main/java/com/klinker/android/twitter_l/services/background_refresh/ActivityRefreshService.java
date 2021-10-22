package com.klinker.android.twitter_l.services.background_refresh;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ActivityUtils;

import java.util.concurrent.TimeUnit;

public class ActivityRefreshService extends Worker {

    private final Context context;
    public ActivityRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "activity-refresh";

    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.activityRefresh / 1000; // convert to seconds

        if (settings.activityRefresh != 0) {
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(ActivityRefreshService.class, refreshInterval, TimeUnit.SECONDS)
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
        AppSettings settings = AppSettings.getInstance(context);
        ActivityUtils utils = new ActivityUtils(context, false);

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification();
        }

        if (settings.syncSecondMentions) {
            SecondActivityRefreshService.startNow(context);
        }

        return Result.success();
    }
}
