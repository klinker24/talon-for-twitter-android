package com.klinker.android.twitter.ui.compose;

import android.app.NotificationManager;

public class RetryCompose extends ComposeActivity {

    @Override
    public void onStart() {
        super.onStart();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.cancel(5); // failed option
        mNotificationManager.cancel(1); // main notification
    }
}
