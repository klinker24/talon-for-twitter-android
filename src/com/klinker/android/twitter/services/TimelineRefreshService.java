package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TimelineRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public TimelineRefreshService() {
        super("TimelineRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (MainActivity.canSwitch) {
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            Context context = getApplicationContext();
            int numberNew = 0;

            AppSettings settings = new AppSettings(context);

            // if they have mobile data on and don't want to sync over mobile data
            if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
                return;
            }

            try {
                Twitter twitter = Utils.getTwitter(context);

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                User user = twitter.verifyCredentials();
                long lastId = sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0);
                long secondToLastId = sharedPrefs.getLong("second_last_tweet_id_" + currentAccount, 0);
                List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

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
                            if (id == lastId || id == secondToLastId) {
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

                    numberNew = statuses.size();
                } else {
                    numberNew = 0;
                }

                HomeDataSource dataSource = new HomeDataSource(context);
                dataSource.open();

                for (twitter4j.Status status : statuses) {
                    try {
                        HomeContentProvider.insertTweet(status, currentAccount, context);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

                if (numberNew > 0) {
                    NotificationUtils.refreshNotification(context);
                }

            } catch (TwitterException e) {
                Log.d("Twitter Update Error", e.getMessage());
            }

            context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
        }
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