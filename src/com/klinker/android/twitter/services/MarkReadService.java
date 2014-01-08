package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.ui.compose.RetryCompose;

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

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Context context = getApplicationContext();
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        // we can just mark everything as read because it isnt taxing at all and won't do anything in the mentions if there isn't one
        // and the shared prefs are easy.
        // this is only called from the notification and there will only ever be one thing that is unread when this button is availible
        MentionsDataSource data = new MentionsDataSource(context);
        data.open();
        data.markAllRead(currentAccount);
        data.close();

        InteractionsDataSource dataSource = new InteractionsDataSource(context);
        dataSource.open();
        dataSource.markAllRead(currentAccount);
        dataSource.close();

        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();
    }

}
