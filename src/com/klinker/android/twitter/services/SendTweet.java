package com.klinker.android.twitter.services;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.compose.RetryCompose;
import com.klinker.android.twitter.utils.Utils;
import com.klinker.android.twitter.utils.api_helper.TwitLongerHelper;
import com.klinker.android.twitter.utils.api_helper.TwitPicHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import twitter4j.Twitter;


public class SendTweet extends Service {

    public String message = "";
    public String attachedUri = "";
    public boolean pwiccer = false;
    public long tweetId = 0l;
    public int remainingChars = 0;

    public boolean finished = false;

    @Override
    public IBinder onBind(Intent intent) {

        // set up the tweet from the intent
        message = intent.getStringExtra("message");
        tweetId = intent.getLongExtra("tweet_id", 0l);
        remainingChars = intent.getIntExtra("char_remaining", 0);
        pwiccer = intent.getBooleanExtra("pwiccer", false);
        attachedUri = intent.getStringExtra("attached_uri");

        if (attachedUri == null) {
            attachedUri = "";
        }

        return null;
    }

    @Override
    public void onCreate() {

        final Context context = this;
        final AppSettings settings = AppSettings.getInstance(this);

        sendingNotification();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean sent = sendTweet(settings, context);

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (sent) {
                            finishedTweetingNotification();
                        } else {
                            makeFailedNotification(message);
                        }

                        finished = true;

                        stopSelf();
                    }
                });
            }
        }).start();

        // if it takes longer than 2 mins to preform the sending, then something is wrong and we will just shut it down.
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!finished) {
                    stopForeground(true);
                    makeFailedNotification(message);
                    stopSelf();
                }
            }
        }, 120000);

    }

    public boolean sendTweet(AppSettings settings, Context context) {
        try {
            Twitter twitter =  Utils.getTwitter(context, settings);

            if (remainingChars < 0 && !pwiccer) {
                // twitlonger goes here
                TwitLongerHelper helper = new TwitLongerHelper(message, twitter);
                helper.setInReplyToStatusId(tweetId);

                return helper.createPost() != 0;
            } else {
                twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(message);
                reply.setInReplyToStatusId(tweetId);

                if (!attachedUri.equals("")) {

                    File outputDir = context.getCacheDir(); // context being the Activity pointer
                    File f = File.createTempFile("compose", "picture", outputDir);

                    Bitmap bitmap = getBitmapToSend(Uri.parse(attachedUri), context);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] bitmapdata = bos.toByteArray();

                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(bitmapdata);
                    fos.flush();
                    fos.close();

                    if (!settings.twitpic) {
                        reply.setMedia(f);
                        twitter.updateStatus(reply);
                        return true;
                    } else {
                        TwitPicHelper helper = new TwitPicHelper(twitter, message, f, context);
                        helper.setInReplyToStatusId(tweetId);
                        return helper.createPost() != 0;
                    }
                } else {
                    // no picture
                    twitter.updateStatus(reply);
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Bitmap getBitmapToSend(Uri uri, Context context) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        int reqWidth = 750;
        int reqHeight = 750;

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = input.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void sendingNotification() {
        // first we will make a notification to let the user know we are tweeting
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.sending_tweet))
                                //.setTicker(getResources().getString(R.string.sending_tweet))
                        .setOngoing(true)
                        .setProgress(100, 0, true);

        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        0
                );

        mBuilder.setContentIntent(resultPendingIntent);

        startForeground(6, mBuilder.build());
        //NotificationManager mNotificationManager =
                //(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //mNotificationManager.notify(6, mBuilder.build());
    }

    public void makeFailedNotification(String text) {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.timeline_dark)
                            .setContentTitle(getResources().getString(R.string.tweet_failed))
                            .setContentText(getResources().getString(R.string.tap_to_retry));

            Intent resultIntent = new Intent(this, RetryCompose.class);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("draft", text);
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

    public void finishedTweetingNotification() {
        try {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(MainActivity.sContext)
                            .setSmallIcon(R.drawable.ic_stat_icon)
                            .setContentTitle(getResources().getString(R.string.tweet_success))
                            .setOngoing(false)
                            .setTicker(getResources().getString(R.string.tweet_success));

            stopForeground(true);

            NotificationManager mNotificationManager =
                    (NotificationManager) MainActivity.sContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(6, mBuilder.build());
            // cancel it immediately, the ticker will just go off
            mNotificationManager.cancel(6);
        } catch (IllegalStateException e) {
            // not attached to activity
        }
    }
}
