package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;

/**
 * Created by luke on 3/21/14.
 */
public class MarkReadSecondAccService extends IntentService {

    SharedPreferences sharedPrefs;

    public MarkReadSecondAccService() {
        super("MarkReadSecAccService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        // clear custom light flow broadcast
        Intent lightFlow = new Intent("com.klinker.android.twitter.CLEARED_NOTIFICATION");
        this.sendBroadcast(lightFlow);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Context context = getApplicationContext();
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        if (currentAccount == 1) {
            currentAccount = 2;
        } else {
            currentAccount = 1;
        }

        // we can just mark everything as read because it isnt taxing at all and won't do anything in the mentions if there isn't one
        // and the shared prefs are easy.
        // this is only called from the notification and there will only ever be one thing that is unread when this button is availible
        MentionsDataSource.getInstance(context).markAllRead(currentAccount);
        InteractionsDataSource.getInstance(context).markAllRead(currentAccount);

        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();
    }

}
