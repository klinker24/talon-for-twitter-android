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
import androidx.core.app.RemoteInput;

public class NotificationDMCompose extends ComposeDMActivity {

    @Override
    public void setUpLayout() {
        super.setUpLayout();

        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).apply();

        notiId = 1;
        replyText = getIntent().getStringExtra("dm_text");

        contactEntry.setText(getIntent().getStringExtra("reply_to").replace(" ", ""));
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
