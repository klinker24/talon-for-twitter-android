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
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * Created by luke on 12/28/13.
 */
public class SecondMentionsRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public SecondMentionsRefreshService() {
        super("SecondMentionsRefreshService");
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
            Twitter twitter = Utils.getSecondTwitter(context);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            if(currentAccount == 1) {
                currentAccount = 2;
            } else {
                currentAccount = 1;
            }

            User user = twitter.verifyCredentials();
            MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

            long lastId = dataSource.getLastIds(currentAccount)[0];
            Paging paging;
            paging = new Paging(1, 200);
            if (lastId != 0) {
                paging.sinceId(lastId);
            }

            List<Status> statuses = twitter.getMentionsTimeline(paging);

            for (twitter4j.Status status : statuses) {
                try {
                    dataSource.createTweet(status, currentAccount);
                } catch (Exception e) {
                    dataSource = MentionsDataSource.getInstance(context);
                    dataSource.createTweet(status, currentAccount);
                }
            }

            if (numberNew > 0) {
                NotificationUtils.notifySecondMentions(context, currentAccount);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}