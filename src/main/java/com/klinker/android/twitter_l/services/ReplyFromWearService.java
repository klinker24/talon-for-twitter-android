package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.compose.RetryCompose;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitLongerHelper;

import twitter4j.Status;
import twitter4j.Twitter;

public class ReplyFromWearService extends IntentService {

    public static final String REPLY_TO_NAME = "reply_to_name";
    public static final String IN_REPLY_TO_ID = "tweet_id";

    public String users = "";
    public String message = "";
    public long tweetId = 0l;

    public boolean finished = false;

    public ReplyFromWearService() {
        super("ReplyFromWear");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        final Context context = this;
        final AppSettings settings = AppSettings.getInstance(this);

        // set up the tweet from the intent
        users = intent.getStringExtra(REPLY_TO_NAME);
        String message = getVoiceReply(intent);
        tweetId = intent.getLongExtra(IN_REPLY_TO_ID, 0l);

        if (message == null) {
            makeFailedNotification("Failed to get the reply.", settings);
            return;
        } else {
            this.message = users + " " + message;
        }

        boolean sent = sendTweet(context);

        if (!sent) {
            makeFailedNotification(ReplyFromWearService.this.message, settings);
        }
    }

    public Twitter getTwitter() {
        return Utils.getTwitter(this, AppSettings.getInstance(this));
    }

    public boolean sendTweet(Context context) {
        try {
            Twitter twitter =  getTwitter();

            if (message.length() > 140) {
                TwitLongerHelper helper = new TwitLongerHelper(message, twitter, context);
                helper.setInReplyToStatusId(tweetId);

                return helper.createPost() != 0;
            } else {
                twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(message);
                reply.setInReplyToStatusId(tweetId);

                // no picture
                Status status = twitter.updateStatus(reply);
                return status != null && status.getId() != 0 && status.getId() != -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void makeFailedNotification(String text, AppSettings settings) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_stat_icon)
                            .setContentTitle(getResources().getString(R.string.tweet_failed))
                            .setContentText(getResources().getString(R.string.tap_to_retry));

            Intent resultIntent = new Intent(this, RetryCompose.class);
            QueuedDataSource.getInstance(this).createDraft(text, settings.currentAccount);
            resultIntent.setAction(Intent.ACTION_SEND);
            resultIntent.setType("text/plain");
            resultIntent.putExtra(Intent.EXTRA_TEXT, text);
            resultIntent.putExtra("failed_notification", true);

            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            0
                    );

            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(5, mBuilder.build());
        } catch (Exception e) {

        }
    }

    public String getVoiceReply(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence(NotificationUtils.EXTRA_VOICE_REPLY).toString();
        }
        return null;
    }
}
