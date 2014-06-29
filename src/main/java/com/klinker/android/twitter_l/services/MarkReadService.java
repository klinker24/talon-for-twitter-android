package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import com.klinker.android.twitter_l.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;

public class MarkReadService extends IntentService {

    SharedPreferences sharedPrefs;

    public MarkReadService() {
        super("MarkReadService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        // clear custom light flow broadcast
        Intent lightFlow = new Intent("com.klinker.android.twitter.CLEARED_NOTIFICATION");
        this.sendBroadcast(lightFlow);

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        final Context context = getApplicationContext();
        final int currentAccount = sharedPrefs.getInt("current_account", 1);

        // we can just mark everything as read because it isnt taxing at all and won't do anything in the mentions if there isn't one
        // and the shared prefs are easy.
        // this is only called from the notification and there will only ever be one thing that is unread when this button is availible
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MentionsDataSource.getInstance(context).markAllRead(currentAccount);
            }
        }, 10000);
        InteractionsDataSource.getInstance(context).markAllRead(currentAccount);

        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();
    }

}
