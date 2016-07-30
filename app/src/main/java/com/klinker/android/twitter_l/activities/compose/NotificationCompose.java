package com.klinker.android.twitter_l.activities.compose;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationManager;
import android.content.Context;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;

public class NotificationCompose extends ComposeActivity {

    @Override
    public void setUpReplyText() {
        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        sharedPrefs = AppSettings.getSharedPreferences(this);

        Context context = getApplicationContext();
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        // we can just mark everything as read because it isnt taxing at all and won't do anything in the mentions if there isn't one
        // and the shared prefs are easy.
        // this is only called from the notification and there will only ever be one thing that is unread when this button is available

        MentionsDataSource.getInstance(context).markAllRead(currentAccount);

        // set up the reply box
        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).apply();
        reply.setText(getIntent().getStringExtra("from_noti"));
        reply.setSelection(reply.getText().toString().length());
        notiId = getIntent().getLongExtra("from_noti_long", 1);
        replyText = getIntent().getStringExtra("from_noti_text");

        sharedPrefs.edit().putLong("from_notification_id", 0).apply();
        sharedPrefs.edit().putString("from_notification_text", "").apply();
        sharedPrefs.edit().putString("from_notification", "").apply();
        sharedPrefs.edit().putBoolean("from_notification_bool", false).apply();

        String t = reply.getText().toString();
        if (!android.text.TextUtils.isEmpty(t) && !t.endsWith(" ")) {
            reply.append(" ");
            reply.setSelection(reply.getText().length());
        }

        CharSequence voiceReply = getVoiceReply(getIntent());
        if (voiceReply != null) {
            if (!voiceReply.equals("")) {
                // set the text
                reply.append(" " + voiceReply);

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
