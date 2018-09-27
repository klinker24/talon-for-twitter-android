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
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.settings.AppSettings;

public class NotificationComposeSecondAcc extends ComposeActivity {

    @Override
    public void setUpReplyText() {
        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        sharedPrefs = AppSettings.getSharedPreferences(this);

        Context context = getApplicationContext();
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        if (currentAccount == 1) {
            currentAccount = 2;
        } else {
            currentAccount = 1;
        }

        useAccOne = false;
        useAccTwo = true;

        ImageView pic = (ImageView) findViewById(R.id.profile_pic);
        FontPrefTextView currentName = (FontPrefTextView) findViewById(R.id.current_name);

        Glide.with(this).load(settings.secondProfilePicUrl).into(pic);

        currentName.setText("@" + settings.secondScreenName);

        MentionsDataSource.getInstance(context).markAllRead(currentAccount);
        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).apply();

        // set up the reply box
        reply.setText(sharedPrefs.getString("from_notification_second", ""));
        reply.setSelection(reply.getText().toString().length());
        notiId = sharedPrefs.getLong("from_notification_long_second", 0);
        replyText = sharedPrefs.getString("from_notification_text_second", "");

        sharedPrefs.edit().putLong("from_notification_id_second", 0).apply();
        sharedPrefs.edit().putString("from_notification_text_second", "").apply();
        sharedPrefs.edit().putString("from_notification_second", "").apply();
        sharedPrefs.edit().putBoolean("from_notification_bool_second", false).apply();

        // try from android wear device
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
