package com.klinker.android.twitter.ui.compose;

import android.app.NotificationManager;
import android.content.Context;

public class NotificationDMCompose extends ComposeDMActivity {

    @Override
    public void setUpLayout() {
        super.setUpLayout();

        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();

        contactEntry.setText(sharedPrefs.getString("from_notification", "").replace(" ", ""));
        reply.requestFocus();
    }
}
