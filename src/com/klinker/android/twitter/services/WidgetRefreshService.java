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
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class WidgetRefreshService  extends IntentService {

    SharedPreferences sharedPrefs;

    public WidgetRefreshService() {
        super("WidgetRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
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

        AppSettings settings = new AppSettings(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            HomeDataSource dataSource = HomeDataSource.getInstance(context);

            User user = twitter.verifyCredentials();
            long[] lastId = dataSource.getLastIds(currentAccount);
            List<Status> statuses = new ArrayList<Status>();

            boolean foundStatus = false;
            int lastJ = 0;

            for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                if (foundStatus) {
                    break;
                } else {
                    statuses.addAll(getList(i + 1, twitter));
                }

                try {
                    for (int j = lastJ; j < statuses.size(); j++) {
                        long id = statuses.get(j).getId();
                        if (id == lastId[0] || id == lastId[1] || id == lastId[2] || id == lastId[3] || id == lastId[4]) {
                            statuses = statuses.subList(0, j);
                            foundStatus = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    foundStatus = true;
                }

                lastJ = statuses.size();
            }

            if (statuses.size() != 0) {
                try {
                    sharedPrefs.edit().putLong("second_last_tweet_id_" + currentAccount, statuses.get(1).getId()).commit();
                } catch (Exception e) {
                    sharedPrefs.edit().putLong("second_last_tweet_id_" + currentAccount, sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0)).commit();
                }
                sharedPrefs.edit().putLong("last_tweet_id_" + currentAccount, statuses.get(0).getId()).commit();
            }

            for (twitter4j.Status status : statuses) {
                try {
                    HomeContentProvider.insertTweet(status, currentAccount, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

        } catch (TwitterException e) {
            Log.d("Twitter Update Error", e.getMessage());
        }

        context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
        sharedPrefs.edit().putBoolean("refresh_me", true).commit();

        mNotificationManager.cancel(6);
    }

    public List<twitter4j.Status> getList(int page, Twitter twitter) {
        try {
            return twitter.getHomeTimeline(new Paging(page, 200));
        } catch (Exception e) {
            Log.v("timeline_refreshing", "caught: " + e.getMessage());
            return new ArrayList<twitter4j.Status>();
        }
    }
}