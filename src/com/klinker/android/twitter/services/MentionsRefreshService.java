package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.Utils;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class MentionsRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public MentionsRefreshService() {
        super("MentionsRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Context context = getApplicationContext();
        AppSettings settings = new AppSettings(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        boolean update = false;
        int numberNew = 0;

        try {
            Twitter twitter = Utils.getTwitter(context, settings);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            User user = twitter.verifyCredentials();
            MentionsDataSource dataSource = new MentionsDataSource(context);
            dataSource.open();
            long[] lastId = dataSource.getLastIds(currentAccount);
            Paging paging;
            paging = new Paging(1, 200).sinceId(lastId[0]);

            List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

            /*boolean broken = false;

            // first try to get the top 50 tweets
            for (int i = 0; i < statuses.size(); i++) {
                long id = statuses.get(i).getId();
                if (id == lastId[0] || id == lastId[1]) {
                    statuses = statuses.subList(0, i);
                    broken = true;
                    break;
                }
            }

            // if that doesn't work, then go for the top 150
            if (!broken) {
                Paging paging2 = new Paging(1, 150);
                List<twitter4j.Status> statuses2 = twitter.getMentionsTimeline(paging2);

                for (int i = 0; i < statuses2.size(); i++) {
                    long id = statuses2.get(i).getId();
                    if (id == lastId[0] || id == lastId[1]) {
                        statuses2 = statuses2.subList(0, i);
                        break;
                    }
                }

                statuses = statuses2;
            }*/

            for (twitter4j.Status status : statuses) {
                try {
                    dataSource.createTweet(status, currentAccount);
                } catch (Exception e) {
                    break;
                }
            }

            sharedPrefs.edit().putBoolean("refresh_me", true).commit();
            sharedPrefs.edit().putBoolean("refresh_me_mentions", true).commit();

            if (settings.notifications) {
                NotificationUtils.refreshNotification(context);
            }

            if (settings.syncSecondMentions) {
                startService(new Intent(context, SecondMentionsRefreshService.class));
            }

            dataSource.close();

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}