package com.klinker.android.twitter.ui.compose;

import android.app.NotificationManager;
import android.content.Context;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;

public class NotificationCompose extends ComposeActivity {

    @Override
    public void setUpReplyText() {
        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Context context = getApplicationContext();
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        // we can just mark everything as read because it isnt taxing at all and won't do anything in the mentions if there isn't one
        // and the shared prefs are easy.
        // this is only called from the notification and there will only ever be one thing that is unread when this button is available

        MentionsDataSource.getInstance(context).markAllRead(currentAccount);

        // set up the reply box
        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();
        reply.setText(sharedPrefs.getString("from_notification", ""));
        reply.setSelection(reply.getText().toString().length());
        notiId = sharedPrefs.getLong("from_notification_long", 0);

        sharedPrefs.edit().putLong("from_notification_id", 0).commit();
        sharedPrefs.edit().putString("from_notification", "").commit();
        sharedPrefs.edit().putBoolean("from_notification_bool", false).commit();
    }
}
