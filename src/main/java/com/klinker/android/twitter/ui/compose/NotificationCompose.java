package com.klinker.android.twitter.ui.compose;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.support.v4.app.RemoteInput;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;

public class NotificationCompose extends ComposeActivity {

    @Override
    public void setUpReplyText() {
        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
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
        replyText = sharedPrefs.getString("from_notification_text", "");

        sharedPrefs.edit().putLong("from_notification_id", 0).commit();
        sharedPrefs.edit().putString("from_notification_text", "").commit();
        sharedPrefs.edit().putString("from_notification", "").commit();
        sharedPrefs.edit().putBoolean("from_notification_bool", false).commit();

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
