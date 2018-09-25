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

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.util.Patterns;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.compose.RetryCompose;
import com.klinker.android.twitter_l.data.ScheduledTweet;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitLongerHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;

import twitter4j.Twitter;

public class SendScheduledTweet extends BroadcastReceiver {

    public static final String JOB_TAG = "send-scheduled-tweet";

    public static void scheduleNextRun(Context context) {
        ArrayList<ScheduledTweet> tweets = QueuedDataSource.getInstance(context).getScheduledTweets();
        Collections.sort(tweets, (result1, result2) -> Long.compare(result1.time, result2.time));

        Intent intent = new Intent(context, SendScheduledTweet.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        Log.v("talon_scheduled", "scheduling next run");

        if (tweets.size() > 0) {
            long nextTime = 0L;
            for (int i = 0; i < tweets.size(); i++) {
                if (tweets.get(i).time > new Date().getTime()) {
                    nextTime = tweets.get(i).time;
                    break;
                }
            }

            if (nextTime == 0L) {
                return;
            }

            Log.v("talon_scheduled", "scheduling tweet: " + new Date(nextTime).toString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("talon_scheduled_tweet", "started service");

        ArrayList<ScheduledTweet> tweets = QueuedDataSource.getInstance(context).getScheduledTweets();
        if (tweets.size() != 0) {
            ScheduledTweet t = null;
            for (int i = 0; i < tweets.size(); i++) {
                if (tweets.get(i).time > new Date().getTime() - (60 * 60 * 1000)) { // within the hour
                    t = tweets.get(i);
                    break;
                }
            }

            if (t == null) {
                return;
            }

            final ScheduledTweet tweet = t;
            final AppSettings settings = AppSettings.getInstance(context);

            new Thread(() -> {
                sendingNotification(context);
                boolean sent = sendTweet(settings, context, tweet.text, settings.currentAccount);

                if (sent) {
                    finishedTweetingNotification(context);
                    QueuedDataSource.getInstance(context).deleteScheduledTweet(tweet.alarmId);
                } else {
                    makeFailedNotification(context, tweet.text, settings);
                }
            }).start();
        }

        scheduleNextRun(context);
    }

    public boolean sendTweet(AppSettings settings, Context context, String message, int account) {
        try {
            Twitter twitter;
            if (account == settings.currentAccount) {
                twitter = Utils.getTwitter(context, settings);
            } else {
                twitter = Utils.getSecondTwitter(context);
            }

            int size = getCount(message);

            Log.v("talon_scheduled", "sending: " + message);

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

    public void sendingNotification(Context context) {
        // first we will make a notification to let the user know we are tweeting
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NotificationChannelUtil.SENDING_SCHEDULED_MESSAGE_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(context.getResources().getString(R.string.sending_tweet))
                        .setOngoing(true)
                        .setProgress(100, 0, true);

        Intent resultIntent = new Intent(context, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        0
                );

        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) MainActivity.sContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(6, mBuilder.build());
    }

    public void makeFailedNotification(Context context, String text, AppSettings settings) {
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
                            0
                    );

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(6, mBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finishedTweetingNotification(Context context) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, NotificationChannelUtil.SENDING_SCHEDULED_MESSAGE_CHANNEL)
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
