package com.klinker.android.twitter.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.Utils;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;

public class PushNotificationService extends Service {

    public static final int FOREGROUND_SERVICE_ID = 11;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public TwitterStream pushStream;
    public Context mContext;
    public AppSettings settings;

    @Override
    public void onCreate() {
        super.onCreate();

        Notification notification = new Notification(R.drawable.ic_stat_icon, "Push Notifications",
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, "Talon Push",
                "Listening", pendingIntent);

        // priority flag is only available on api level 16 and above
        notification.priority = Notification.PRIORITY_MIN;

        startForeground(FOREGROUND_SERVICE_ID, notification);

        mContext = getApplicationContext();

        settings = new AppSettings(mContext);

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.STOP_PUSH");
        registerReceiver(stopPush, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.START_PUSH");
        registerReceiver(startPush, filter);

        if (!settings.liveStreaming) {
            mContext.sendBroadcast(new Intent("com.klinker.android.twitter.START_PUSH"));
        }
    }

    public BroadcastReceiver stopPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                pushStream.shutdown();
                Log.v("twitter_stream_push", "stopping push notifications");
            } catch (Exception e) {
                // it isn't running
            }
        }
    };

    public BroadcastReceiver startPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pushStream = Utils.getStreamingTwitter(context, DrawerActivity.settings);
            StatusListener listener = new StatusListener() {
                @Override
                public void onStatus(Status status) {
                    Log.v("twitter_stream_push", "@" + status.getUser().getScreenName() + " - " + status.getText());
                    MentionsDataSource dataSource = new MentionsDataSource(mContext);
                    dataSource.open();
                    dataSource.createTweet(status, settings.currentAccount);

                    NotificationUtils.refreshNotification(mContext);

                    dataSource.close();
                    if (status.isRetweet()) {
                        Log.v("twitter_stream_push", "Retweeted status");
                    }
                }

                @Override
                public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                    Log.v("twitter_stream_push", "Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
                }

                @Override
                public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                    Log.v("twitter_stream_push", "Got track limitation notice:" + numberOfLimitedStatuses);
                }

                @Override
                public void onScrubGeo(long userId, long upToStatusId) {
                    Log.v("twitter_stream_push", "Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
                }

                @Override
                public void onStallWarning(StallWarning warning) {
                    Log.v("twitter_stream_push", "Got stall warning:" + warning);
                }

                @Override
                public void onException(Exception ex) {
                    ex.printStackTrace();
                }
            };

            String myName = settings.myScreenName;
            Log.v("twitter_stream_push", "my id: " + myName + "");

            pushStream.addListener(listener);
            pushStream.filter(new FilterQuery().track(new String[]{myName}));
            Log.v("twitter_stream_push", "started push notifications");
        }
    };
}