package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TimelineRefreshService extends IntentService {

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public TimelineRefreshService() {
        super("TimelineRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (!MainActivity.canSwitch || CatchupPull.isRunning || WidgetRefreshService.isRunning || TimelineRefreshService.isRunning) {
            return;
        }
        if (MainActivity.canSwitch) {
            TimelineRefreshService.isRunning = true;
            sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

            Context context = getApplicationContext();
            int numberNew = 0;

            AppSettings settings = AppSettings.getInstance(context);

            // if they have mobile data on and don't want to sync over mobile data
            if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
                return;
            }

            try {
                Twitter twitter = Utils.getTwitter(context, settings);

                HomeDataSource dataSource = HomeDataSource.getInstance(context);

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                User user = twitter.verifyCredentials();
                List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

                boolean foundStatus = false;

                Paging paging = new Paging(1, 200);

                long[] lastId = null;
                long id;
                try {
                    lastId = dataSource.getLastIds(currentAccount);
                    id = lastId[1];
                } catch (Exception e) {
                    TimelineRefreshService.isRunning = false;
                    return;
                }

                if (id == 0) {
                    id = 1;
                }

                paging.setSinceId(id);

                for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                    try {
                        if (!foundStatus) {
                            paging.setPage(i + 1);
                            List<Status> list = twitter.getHomeTimeline(paging);
                            statuses.addAll(list);

                            if (statuses.size() <= 1 || statuses.get(statuses.size() - 1).getId() == lastId[0]) {
                                Log.v("talon_inserting", "found status");
                                foundStatus = true;
                            } else {
                                Log.v("talon_inserting", "haven't found status");
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

                Log.v("talon_pull", "got statuses, new = " + statuses.size());

                // hash set to check for duplicates I guess
                HashSet hs = new HashSet();
                hs.addAll(statuses);
                statuses.clear();
                statuses.addAll(hs);

                Log.v("talon_inserting", "tweets after hashset: " + statuses.size());

                lastId = dataSource.getLastIds(currentAccount);

                int inserted = HomeDataSource.getInstance(context).insertTweets(statuses, currentAccount, lastId);

                if (inserted > 0 && statuses.size() > 0) {
                    sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).commit();
                }

                sharedPrefs.edit().putBoolean("refresh_me", true).commit();

                if (settings.notifications && settings.timelineNot && inserted > 0  && !intent.getBooleanExtra("from_launcher", false)) {
                    NotificationUtils.refreshNotification(context);
                }

                if (settings.preCacheImages) {
                    startService(new Intent(this, PreCacheService.class));
                }

            } catch (TwitterException e) {
                Log.d("Twitter Update Error", e.getMessage());
            }

            sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
            getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

            TimelineRefreshService.isRunning = false;
        }
    }
}