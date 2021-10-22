package com.klinker.android.twitter_l.services.background_refresh;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;

public class ListRefreshService extends Worker {

    private final Context context;
    public ListRefreshService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "list-timeline-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(JOB_TAG);
    }

    public static void startNow(Context context) {
        WorkManager.getInstance(context)
                .enqueue(new OneTimeWorkRequest.Builder(ListRefreshService.class).build());
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.listRefresh / 1000; // convert to seconds

        if (settings.listRefresh != 0) {
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(ListRefreshService.class, refreshInterval, TimeUnit.SECONDS)
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
        if (!MainActivity.canSwitch || WidgetRefreshService.isRunning || ListRefreshService.isRunning) {
            return Result.success();
        }

        sharedPrefs = AppSettings.getSharedPreferences(context);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        List<Long> listIds = new ArrayList<>();

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String listIdentifier = "account_" + currentAccount + "_list_" + (i + 1) + "_long";
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);

            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_LIST) {
                listIds.add(sharedPrefs.getLong(listIdentifier, 0L));
            }
        }

        for (Long listId : listIds) {
            if (MainActivity.canSwitch) {
                Log.v("talon_refresh", "refreshing list: " + listId);

                ListRefreshService.isRunning = true;

                Context context = getApplicationContext();
                AppSettings settings = AppSettings.getInstance(context);

                Twitter twitter = Utils.getTwitter(context, settings);

                long[] lastId = ListDataSource.getInstance(context).getLastIds(listId);

                final List<twitter4j.Status> statuses = new ArrayList<>();

                boolean foundStatus = false;

                Paging paging = new Paging(1, 200);

                if (lastId[0] > 0) {
                    paging.setSinceId(lastId[0]);
                }

                for (int i = 0; i < settings.maxTweetsRefresh; i++) {

                    try {
                        if (!foundStatus) {
                            paging.setPage(i + 1);
                            List<Status> list = twitter.getUserListStatuses(listId, paging);

                            statuses.addAll(list);
                        }
                    } catch (Exception e) {
                        // the page doesn't exist
                        foundStatus = true;
                    } catch (OutOfMemoryError o) {
                        // don't know why...
                    }
                }

                ListDataSource dataSource = ListDataSource.getInstance(context);
                dataSource.insertTweets(statuses, listId);

                sharedPrefs.edit().putBoolean("refresh_me_list_" + listId, true).apply();
                context.sendBroadcast(new Intent("com.klinker.android.twitter.LIST_REFRESHED_" + listId));
            }

            ListRefreshService.isRunning = false;
        }

        return Result.success();
    }
}