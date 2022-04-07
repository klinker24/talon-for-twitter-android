package com.klinker.android.twitter_l.services;
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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.util.Log;
import android.util.Patterns;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.compose.RetryCompose;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitLongerHelper;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import twitter4j.Twitter;

public class SendQueueService extends Worker {

    private final Context context;
    public SendQueueService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    public static final String JOB_TAG = "send-queue-service";

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        int refreshInterval = 60 * 30; // 30 minutes, as seconds

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(SendQueueService.class, refreshInterval, TimeUnit.SECONDS)
                        .setConstraints(new Constraints.Builder()
                                .setRequiredNetworkType(settings.syncMobile ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                                .build())
                        .build();
//        WorkManager.getInstance(context)
//                .enqueueUniquePeriodicWork(JOB_TAG, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.v("talon_queued", "starting to send queued tweets");

        final AppSettings settings = AppSettings.getInstance(context);

        final String[] queued = QueuedDataSource.getInstance(context)
                .getQueuedTweets(AppSettings.getInstance(context).currentAccount);

        for (String s : queued) {
            sendingNotification();
            boolean sent = sendTweet(settings, context, s);

            if (sent) {
                finishedTweetingNotification();
                QueuedDataSource.getInstance(context).deleteQueuedTweet(s);
            } else {
                makeFailedNotification(s, settings);
            }
        }

        return Result.success();
    }

    public boolean sendTweet(AppSettings settings, Context context, String message) {
        try {
            Twitter twitter =  Utils.getTwitter(context, settings);

            int size = getCount(message);

            Log.v("talon_queued", "sending: " + message);

            if (size > AppSettings.getInstance(context).tweetCharacterCount && settings.twitlonger) {
                // twitlonger goes here
                TwitLongerHelper helper = new TwitLongerHelper(message, twitter, context);

                return helper.createPost() != 0;
            } else if (size <= AppSettings.getInstance(context).tweetCharacterCount) {
                twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(message);
                twitter.updateStatus(reply);
            } else {
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getCount(String text) {
        if (!text.contains("http")) { // no links, normal tweet
            return text.length();
        } else {
            int count = text.length();
            Matcher m = Patterns.WEB_URL.matcher(text);

            while(m.find()) {
                String url = m.group();
                count -= url.length(); // take out the length of the url
                count += 23; // add 23 for the shortened url
            }

            return count;
        }
    }

    public void sendingNotification() {
        // first we will make a notification to let the user know we are tweeting
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NotificationChannelUtil.TWEETING_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(context.getResources().getString(R.string.sending_tweet))
                                //.setTicker(getResources().getString(R.string.sending_tweet))
                        .setOngoing(true)
                        .setProgress(100, 0, true);

        Intent resultIntent = new Intent(context, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT)
                );

        mBuilder.setContentIntent(resultPendingIntent);
    }

    public void makeFailedNotification(String text, AppSettings settings) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, NotificationChannelUtil.FAILED_TWEETS_CHANNEL)
                            .setSmallIcon(R.drawable.ic_stat_icon)
                            .setContentTitle(context.getResources().getString(R.string.tweet_failed))
                            .setContentText(context.getResources().getString(R.string.tap_to_retry));

            Intent resultIntent = new Intent(context, RetryCompose.class);
            QueuedDataSource.getInstance(context).createDraft(text, settings.currentAccount);
            resultIntent.setAction(Intent.ACTION_SEND);
            resultIntent.setType("text/plain");
            resultIntent.putExtra(Intent.EXTRA_TEXT, text);
            resultIntent.putExtra("failed_notification", true);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            context,
                            0,
                            resultIntent,
                            Utils.withImmutability(PendingIntent.FLAG_UPDATE_CURRENT)
                    );

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(5, mBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finishedTweetingNotification() {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, NotificationChannelUtil.TWEETING_NOTIFICATION_CHANNEL)
                            .setSmallIcon(R.drawable.ic_stat_icon)
                            .setContentTitle(context.getResources().getString(R.string.tweet_success))
                            .setOngoing(false)
                            .setTicker(context.getResources().getString(R.string.tweet_success));

            if (AppSettings.getInstance(context).vibrate) {
                Log.v("talon_vibrate", "vibrate on compose");
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = { 0, 50, 500 };
                v.vibrate(pattern, -1);
            }

            NotificationManager mNotificationManager =
                    (NotificationManager) MainActivity.sContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(6, mBuilder.build());
            // cancel it immediately, the ticker will just go off
            mNotificationManager.cancel(6);
        } catch (Exception e) {
            // not attached to activity
        }
    }
}
