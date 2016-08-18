package com.klinker.android.twitter_l.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.ListFragment;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;

public class ListRefreshService extends ScheduledService {

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public ListRefreshService() {
        super("ListRefreshService");
    }

    @Override
    protected ScheduledService.ScheduleInfo getScheduleInfo(AppSettings settings) {
        return new ScheduledService.ScheduleInfo(ListRefreshService.class, ListFragment.LIST_REFRESH_ID, settings.listRefresh);
    }

    @Override
    public void handleIntent(Intent intent) {
        if (!MainActivity.canSwitch || CatchupPull.isRunning || WidgetRefreshService.isRunning || ListRefreshService.isRunning) {
            return;
        }
        sharedPrefs = AppSettings.getSharedPreferences(this);


        int currentAccount = sharedPrefs.getInt("current_account", 1);

        List<Long> listIds = new ArrayList();

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String listIdentifier = "account_" + currentAccount + "_list_" + (i + 1) + "_long";
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);

            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_LIST) {
                listIds.add(sharedPrefs.getLong(listIdentifier, 0l));
            }
        }

        for (Long listId : listIds) {
            if (MainActivity.canSwitch) {
                Log.v("talon_refresh", "refreshing list: " + listId);

                ListRefreshService.isRunning = true;

                Context context = getApplicationContext();
                AppSettings settings = AppSettings.getInstance(context);

                // if they have mobile data on and don't want to sync over mobile data
                if (intent.getBooleanExtra("on_start_refresh", false)) {

                } else if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
                    return;
                }

                Twitter twitter = Utils.getTwitter(context, settings);

                long[] lastId = ListDataSource.getInstance(context).getLastIds(listId);

                final List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

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

                if (!intent.getBooleanExtra("on_start_refresh", false)) {
                    sharedPrefs.edit().putBoolean("refresh_me_list_" + listId, true).apply();
                }

                sendBroadcast(new Intent("com.klinker.android.twitter.LIST_REFRESHED_" + listId));
            }

            ListRefreshService.isRunning = false;
        }
    }
}