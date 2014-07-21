package com.klinker.android.twitter.ui.compose;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;

public class NotificationDMCompose extends ComposeDMActivity {

    @Override
    public void setUpLayout() {
        super.setUpLayout();

        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();

        notiId = 1;
        replyText = sharedPrefs.getString("from_notification_text", "");
        sharedPrefs.edit().putString("from_notification_text", "").commit();

        contactEntry.setText(sharedPrefs.getString("from_notification", "").replace(" ", ""));
        reply.requestFocus();

        // try from android wear device
        CharSequence voiceReply = getVoiceReply(getIntent());
        if (voiceReply != null) {
            if (!voiceReply.equals("")) {
                // set the text
                reply.setText(voiceReply);

                // send the message
                doneClick();

                finish();
            }
        }
    }

    public CharSequence getVoiceReply(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("extra_voice_reply");
        }
        return null;
    }
}
