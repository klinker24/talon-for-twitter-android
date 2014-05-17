package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.scheduled_tweets.ScheduledActivity;

import java.util.ArrayList;

public class SendScheduledTweet extends IntentService {

    SharedPreferences sharedPrefs;

    public SendScheduledTweet() {
        super("ScheduledService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.v("talon_scheduled_tweet", "started service");

        String number = intent.getStringExtra(ScheduledActivity.EXTRA_NUMBER);
        String message = intent.getStringExtra(ScheduledActivity.EXTRA_MESSAGE);

        try {
            for (int i = 0; i < numbers.size(); i++) {
                SendUtils.sendMessage(this, Transaction.NO_THREAD_ID, numbers.get(i), message);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.stat_notify_sms)
                                .setContentTitle("Sending Successful")
                                .setContentText("Scheduled SMS sent successfully.");

                Intent resultIntent = new Intent(this, ScheduledActivity.class);

                PendingIntent resultPendingIntent =
                        PendingIntent.getActivity(
                                this,
                                0,
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                mBuilder.setContentIntent(resultPendingIntent);

                int mNotificationId = 5;

                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.v("scheduled_sms", "error sending");
        }
    }
}
