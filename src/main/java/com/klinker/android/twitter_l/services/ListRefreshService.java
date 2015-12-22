package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;

public class ListRefreshService extends IntentService {

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public ListRefreshService() {
        super("TimelineRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (!MainActivity.canSwitch || CatchupPull.isRunning || WidgetRefreshService.isRunning || ListRefreshService.isRunning) {
            return;
        }
        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        List<Long> listIds = new ArrayList<>();

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
                ListDataSource dataSource = ListDataSource.getInstance(context);
                List<Status> statuses = new ArrayList<Status>();

                boolean foundStatus = false;

                Paging paging = new Paging(1, 200);

                long[] lastId = null;
                long id;
                try {
                    lastId = dataSource.getLastIds(listId);
                    id = lastId[1];
                } catch (Exception e) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException i) {

                    }
                    ListRefreshService.isRunning = false;
                    return;
                }

                if (id == 0) {
                    id = 1;
                }

                try {
                    paging.setSinceId(id);
                } catch (Exception e) {
                    paging.setSinceId(1l);
                }

                for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                    try {
                        if (!foundStatus) {
                            paging.setPage(i + 1);
                            List<Status> list = twitter.getUserListStatuses(listId, paging);
                            statuses.addAll(list);

                            if (statuses.size() <= 1 || statuses.get(statuses.size() - 1).getId() == lastId[0]) {
                                foundStatus = true;
                            } else {
                                foundStatus = false;
                            }

                        }
                    } catch (Exception e) {
                        // the page doesn't exist
                        foundStatus = true;
                    } catch (OutOfMemoryError o) {
                        // don't know why...
                    }
                }

                // hash set to check for duplicates I guess
                HashSet hs = new HashSet();
                hs.addAll(statuses);
                statuses.clear();
                statuses.addAll(hs);

                Long currentTime = Calendar.getInstance().getTimeInMillis();
                if (currentTime - sharedPrefs.getLong("last_list_insert_" + listId, 0l) < 10000) {
                    sendBroadcast(new Intent("com.klinker.android.twitter.LIST_REFRESHED").putExtra("number_new", 0));
                    ListRefreshService.isRunning = false;
                    return;
                } else {
                    sharedPrefs.edit().putLong("last_list_insert_" + listId, currentTime).commit();
                }

                int inserted = ListDataSource.getInstance(context).insertTweets(statuses, listId);

                if (!intent.getBooleanExtra("on_start_refresh", false)) {
                    sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                }

            }

            sendBroadcast(new Intent("com.klinker.android.twitter.LIST_REFRESHED"));
            ListRefreshService.isRunning = false;
        }
    }
}