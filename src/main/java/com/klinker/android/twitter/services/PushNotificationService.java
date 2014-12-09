package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.receivers.PushNotificationReceiver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PushNotificationService extends IntentService {

    public PushNotificationService() {
        super("PushNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (extras != null && !extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setContentTitle("GCM from Talon")
                        .setContentText(extras.getString("message"))
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(this);

                notificationManager.notify(17, mBuilder.build());
            }
        }

        PushNotificationReceiver.completeWakefulIntent(intent);
    }
}