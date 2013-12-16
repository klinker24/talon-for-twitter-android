package com.klinker.android.talon.ui;

import android.app.NotificationManager;
import android.content.Context;

public class RetryCompose extends ComposeActivity {

    @Override
    public void onStart() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(5); // failed option
    }
}
