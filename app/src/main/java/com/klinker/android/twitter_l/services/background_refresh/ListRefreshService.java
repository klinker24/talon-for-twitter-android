package com.klinker.android.twitter_l.services.background_refresh;

import android.content.Context;
import android.content.Intent;
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
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;

public class ListRefreshService extends SimpleJobService {

    public static final String JOB_TAG = "list-timeline-refresh";

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public static void cancelRefresh(Context context) {
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.cancel(JOB_TAG);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = (int) settings.listRefresh / 1000; // convert to seconds

        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));

        if (settings.listRefresh != 0) {
            Job myJob = dispatcher.newJobBuilder()
                    .setService(ListRefreshService.class)
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
    public int onRunJob(JobParameters job) {
        if (!MainActivity.canSwitch || WidgetRefreshService.isRunning || ListRefreshService.isRunning) {
            return 0;
        }

        sharedPrefs = AppSettings.getSharedPreferences(this);

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
                sendBroadcast(new Intent("com.klinker.android.twitter.LIST_REFRESHED_" + listId));
            }

            ListRefreshService.isRunning = false;
        }

        return 0;
    }
}