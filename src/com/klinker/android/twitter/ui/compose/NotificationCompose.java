package com.klinker.android.twitter.ui.compose;

public class NotificationCompose extends ComposeActivity {

    @Override
    public void setUpReplyText() {
        reply.setText(sharedPrefs.getString("from_notification", ""));
        reply.setSelection(reply.getText().toString().length());
        notiId = sharedPrefs.getLong("from_notification_id", 0);

        sharedPrefs.edit().putLong("from_notification_id", 0).commit();
        sharedPrefs.edit().putString("from_notification", "").commit();
        sharedPrefs.edit().putBoolean("from_notification_bool", false).commit();
    }
}
