package com.klinker.android.twitter_l.ui.compose;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;

/**
 * Created by lucasklinker on 7/24/14.
 */
public class NotificationComposeSecondAcc extends ComposeActivity {

    @Override
    public void setUpReplyText() {
        // mark the messages as read here
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
        mNotificationManager.cancel(9);

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        Context context = getApplicationContext();
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        if (currentAccount == 1) {
            currentAccount = 2;
        } else {
            currentAccount = 1;
        }

        useAccOne = false;
        useAccTwo = true;

        NetworkedCacheableImageView pic = (NetworkedCacheableImageView) findViewById(R.id.profile_pic);
        HoloTextView currentName = (HoloTextView) findViewById(R.id.current_name);

        if (settings.roundContactImages) {
            pic.loadImage(settings.secondProfilePicUrl, false, null, NetworkedCacheableImageView.CIRCLE);
        } else {
            pic.loadImage(settings.secondProfilePicUrl, false, null);
        }

        currentName.setText("@" + settings.secondScreenName);

        MentionsDataSource.getInstance(context).markAllRead(currentAccount);
        sharedPrefs.edit().putInt("dm_unread_" + currentAccount, 0).commit();

        // set up the reply box
        reply.setText(sharedPrefs.getString("from_notification_second", ""));
        reply.setSelection(reply.getText().toString().length());
        notiId = sharedPrefs.getLong("from_notification_long_second", 0);
        replyText = sharedPrefs.getString("from_notification_text_second", "");

        sharedPrefs.edit().putLong("from_notification_id_second", 0).commit();
        sharedPrefs.edit().putString("from_notification_text_second", "").commit();
        sharedPrefs.edit().putString("from_notification_second", "").commit();
        sharedPrefs.edit().putBoolean("from_notification_bool_second", false).commit();

        // try from android wear device
        CharSequence voiceReply = getVoiceReply(getIntent());
        if (voiceReply != null) {
            if (!voiceReply.equals("")) {
                // set the text
                reply.append(voiceReply);

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
