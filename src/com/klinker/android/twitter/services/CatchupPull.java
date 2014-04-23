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
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
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


public class CatchupPull extends IntentService {

    SharedPreferences sharedPrefs;

    public static boolean isRunning = false;

    public CatchupPull() {
        super("CatchupPullService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (CatchupPull.isRunning || WidgetRefreshService.isRunning || TimelineRefreshService.isRunning || !MainActivity.canSwitch) {
            return;
        }
        CatchupPull.isRunning = true;

        Log.v("talon_pull", "catchup pull started");

        sharedPrefs =  getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        final Context context = getApplicationContext();

        int unreadNow = sharedPrefs.getInt("pull_unread", 0);

        // stop it just in case
        context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

        AppSettings settings = AppSettings.getInstance(context);

        try {
            if (settings.liveStreaming) {
                Log.v("talon_pull", "into the try for catchup service");
                Twitter twitter = Utils.getTwitter(context, settings);

                HomeDataSource dataSource = HomeDataSource.getInstance(context);

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                User user = twitter.verifyCredentials();
                List<Status> statuses = new ArrayList<Status>();

                boolean foundStatus = false;

                Paging paging = new Paging(1, 200);

                long[] lastId;
                long id;

                try {
                    lastId = dataSource.getLastIds(currentAccount);
                    id = lastId[0];
                } catch (Exception e) {
                    context.startService(new Intent(context, TalonPullNotificationService.class));
                    CatchupPull.isRunning = false;
                    return;
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
                        e.printStackTrace();
                    } catch (OutOfMemoryError o) {
                        // don't know why...
                        o.printStackTrace();
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

                int inserted = dataSource.insertTweets(statuses, currentAccount, lastId);

                if (inserted > 0 && statuses.size() > 0) {
                    sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).commit();
                    unreadNow += statuses.size();
                }

                if (settings.preCacheImages) {
                    // delay it 15 seconds so that we can finish checking mentions first
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startService(new Intent(context, PreCacheService.class));
                        }
                    }, 15000);
                }

                sharedPrefs.edit().putBoolean("refresh_me", true).commit();
            }

        } catch (TwitterException e) {
            Log.v("talon_pull", "caught while refreshing the messages in the catchup service");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {

        }

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            User user = twitter.verifyCredentials();
            MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

            long[] lastId = dataSource.getLastIds(currentAccount);
            Paging paging;
            paging = new Paging(1, 200);
            if (lastId[0] > 0) {
                paging.sinceId(lastId[0]);
            }

            List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

            int numNew = dataSource.insertTweets(statuses, currentAccount);

            sharedPrefs.edit().putBoolean("refresh_me", true).commit();
            sharedPrefs.edit().putBoolean("refresh_me_mentions", true).commit();

            if (settings.notifications && settings.mentionsNot && numNew > 0) {
                NotificationUtils.refreshNotification(context);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }

        sharedPrefs.edit().putInt("pull_unread", unreadNow).commit();
        context.startService(new Intent(context, TalonPullNotificationService.class));

        context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
        getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        Log.v("talon_pull", "finished with the catchup service");

        CatchupPull.isRunning = false;
    }
}
