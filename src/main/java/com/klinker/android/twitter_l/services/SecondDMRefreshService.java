package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;
import twitter4j.*;

import java.util.List;

/**
 * Created by luke on 7/6/14.
 */
public class SecondDMRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public SecondDMRefreshService() {
        super("DirectMessageRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        Context context = getApplicationContext();
        AppSettings settings = AppSettings.getInstance(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        boolean update = false;
        int numberNew = 0;

        try {
            Twitter twitter = Utils.getSecondTwitter(context);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            if (currentAccount == 1) {
                currentAccount = 2;
            } else {
                currentAccount = 1;
            }

            User user = twitter.verifyCredentials();
            long lastId = sharedPrefs.getLong("last_direct_message_id_" + currentAccount, 0);
            Paging paging;
            if (lastId != 0) {
                paging = new Paging(1).sinceId(lastId);
            } else {
                paging = new Paging(1, 500);
            }

            List<DirectMessage> dm = twitter.getDirectMessages(paging);
            List<DirectMessage> sent = twitter.getSentDirectMessages(paging);

            if (dm.size() != 0) {
                sharedPrefs.edit().putLong("last_direct_message_id_" + currentAccount, dm.get(0).getId()).commit();
                numberNew = dm.size();
            } else {
                numberNew = 0;
            }

            DMDataSource dataSource = DMDataSource.getInstance(context);
            int inserted = 0;

            for (DirectMessage directMessage : dm) {
                try {
                    dataSource.createDirectMessage(directMessage, currentAccount);
                } catch (Exception e) {
                    dataSource = DMDataSource.getInstance(context);
                    dataSource.createDirectMessage(directMessage, currentAccount);
                }
                inserted++;
            }

            for (DirectMessage directMessage : sent) {
                try {
                    dataSource.createDirectMessage(directMessage, currentAccount);
                } catch (Exception e) {
                    dataSource = DMDataSource.getInstance(context);
                    dataSource.createDirectMessage(directMessage, currentAccount);
                }
            }

            sharedPrefs.edit().putBoolean("refresh_me", true).commit();
            sharedPrefs.edit().putBoolean("refresh_me_dm", true).commit();

            if (settings.notifications && settings.dmsNot && inserted > 0) {
                int currentUnread = sharedPrefs.getInt("dm_unread_" + currentAccount, 0);
                sharedPrefs.edit().putInt("dm_unread_" + currentAccount, numberNew + currentUnread).commit();

                NotificationUtils.notifySecondDMs(context, currentAccount);
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}