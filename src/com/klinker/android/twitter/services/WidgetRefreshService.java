package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.klinker.android.twitter.R;
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

public class WidgetRefreshService  extends IntentService {

    SharedPreferences sharedPrefs;
    public static boolean isRunning = false;

    public WidgetRefreshService() {
        super("WidgetRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        // it is refreshing elsewhere, so don't start
        if (WidgetRefreshService.isRunning || TimelineRefreshService.isRunning || CatchupPull.isRunning || !MainActivity.canSwitch) {
            return;
        }
        WidgetRefreshService.isRunning = true;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(getResources().getString(R.string.refreshing) + "...")
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.refreshing_widget) + "...")
                        .setProgress(100, 100, true)
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.drawer_sync_dark));

        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(6, mBuilder.build());

        Context context = getApplicationContext();

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

            long[] lastId;
            long id;
            try {
                lastId = dataSource.getLastIds(currentAccount);
                id = lastId[0];
            } catch (Exception e) {
                id = sharedPrefs.getLong("account_" + currentAccount + "_lastid", 1l);
            }
            if (id == 1l) {
                return;
            }

            paging.setSinceId(id);

            for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                try {
                    if (!foundStatus) {
                        paging.setPage(i + 1);
                        List<Status> list = twitter.getHomeTimeline(paging);

                        if (list.size() > 185) { // close to the 200 lol
                            foundStatus = false;
                        } else {
                            foundStatus = true;
                        }

                        statuses.addAll(list);
                    }
                } catch (Exception e) {
                    // the page doesn't exist
                    foundStatus = true;
                } catch (OutOfMemoryError o) {
                    // don't know why...
                }
            }

            Log.v("talon_pull", "got statuses, new = " + statuses.size());

            // hash set to remove duplicates I guess
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

        } catch (TwitterException e) {
            Log.d("Twitter Update Error", e.getMessage());
        }

        context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
        sharedPrefs.edit().putBoolean("refresh_me", true).commit();

        mNotificationManager.cancel(6);

        WidgetRefreshService.isRunning = false;
    }
}