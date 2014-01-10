package com.klinker.android.twitter.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.Utils;

import java.util.concurrent.ExecutionException;

import twitter4j.DirectMessage;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;

public class TalonPullNotificationService extends Service {

    public static final int FOREGROUND_SERVICE_ID = 11;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public TwitterStream pushStream;
    public Context mContext;
    public AppSettings settings;
    public SharedPreferences sharedPreferences;
    public InteractionsDataSource interactions;

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stop = new Intent(this, StopPull.class);
        PendingIntent stopPending = PendingIntent.getService(this, 0, stop, 0);

        RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
        remoteView.setOnClickPendingIntent(R.id.popup_button, stopPending);
        remoteView.setImageViewResource(R.id.icon, R.drawable.ic_stat_icon);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.color.transparent)
                        .setContentTitle(getResources().getString(R.string.talon_pull))
                        .setContentText(getResources().getString(R.string.listening_for_mentions) + "...")
                        .setOngoing(true)
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_stat_icon));

        /*NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContent(remoteView);
                        //.setOngoing(true);*/


        if (getApplicationContext().getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.addAction(R.drawable.ic_cancel_dark, getApplicationContext().getResources().getString(R.string.stop), stopPending);
        }

        mBuilder.setContentIntent(pendingIntent);

        // priority flag is only available on api level 16 and above
        if (getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.setPriority(Notification.PRIORITY_MIN);
        }

        startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());

        mContext = getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        settings = new AppSettings(mContext);
        interactions = new InteractionsDataSource(mContext);
        interactions.open();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.STOP_PUSH");
        registerReceiver(stopPush, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.START_PUSH");
        registerReceiver(startPush, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.STOP_PUSH_SERVICE");
        registerReceiver(stopService, filter);

        if (!settings.liveStreaming) {
            mContext.sendBroadcast(new Intent("com.klinker.android.twitter.START_PUSH"));
        }
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(startPush);
        } catch (Exception e) { }
        try {
            unregisterReceiver(stopPush);
        } catch (Exception e) { }
        try {
            unregisterReceiver(stopService);
        } catch (Exception e) { }

        try {
            interactions.close();
        } catch (Exception e) { }

        super.onDestroy();
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

            try {
                interactions.close();
            } catch (Exception e) { }
        }
    };

    public BroadcastReceiver stopService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    public BroadcastReceiver startPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                interactions.open();
            } catch (Exception e) { }

            settings = new AppSettings(context);
            pushStream = Utils.getStreamingTwitter(context, DrawerActivity.settings);

            String myName = settings.myScreenName;
            Log.v("twitter_stream_push", "my id: " + myName + "");

            pushStream.addListener(userStream);
            //pushStream.filter(new FilterQuery().track(new String[]{myName}));
            pushStream.user(new String[] {myName});
            Log.v("twitter_stream_push", "started push notifications");
        }
    };

    public StatusListener listener = new StatusListener() {
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

    public UserStreamListener userStream = new UserStreamListener() {
        @Override
        public void onStatus(Status status) {
            if(status.getText().contains("@" + settings.myScreenName)) {
                Log.v("twitter_stream_push", "onStatus @" + status.getUser().getScreenName() + " - " + status.getText());

                AppSettings settings = new AppSettings(mContext);

                if (!status.isRetweet()) { // it is a normal mention
                    MentionsDataSource dataSource = new MentionsDataSource(mContext);
                    dataSource.open();
                    dataSource.createTweet(status, settings.currentAccount);
                    interactions.createMention(mContext, status, settings.currentAccount);
                    sharedPreferences.edit().putBoolean("new_notification", true).commit();
                    sharedPreferences.edit().putBoolean("refresh_me_mentions", true).commit();

                    if(settings.notifications) {
                        NotificationUtils.refreshNotification(mContext);
                    }

                    dataSource.close();
                } else { // it is a retweet

                    if (!status.getUser().getScreenName().equals(settings.myScreenName) && status.getRetweetedStatus().getUser().getScreenName().equals(settings.myScreenName)) {
                        int newRetweets = sharedPreferences.getInt("new_retweets", 0);
                        newRetweets++;
                        sharedPreferences.edit().putInt("new_retweets", newRetweets).commit();
                        interactions.updateInteraction(mContext, status.getUser(), status, settings.currentAccount, InteractionsDataSource.TYPE_RETWEET);
                        sharedPreferences.edit().putBoolean("new_notification", true).commit();

                        if(settings.notifications) {
                            NotificationUtils.newInteractions(status.getUser(), mContext, sharedPreferences, " " + getResources().getString(R.string.retweeted));
                        }
                    }
                }
            }
        }

        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

        }

        @Override
        public void onDeletionNotice(long directMessageId, long userId) {

        }

        @Override
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {

        }

        @Override
        public void onScrubGeo(long userId, long upToStatusId) {

        }

        @Override
        public void onStallWarning(StallWarning warning) {

        }

        @Override
        public void onFriendList(long[] friendIds) {

        }

        @Override
        public void onFavorite(User source, User target, Status favoritedStatus) {
            if(!source.getScreenName().equals(settings.myScreenName) && target.getScreenName().equals(settings.myScreenName)) {
                AppSettings settings = new AppSettings(mContext);

                Log.v("twitter_stream_push", "onFavorite source:@"
                        + source.getScreenName() + " target:@"
                        + target.getScreenName() + " @"
                        + favoritedStatus.getUser().getScreenName() + " - "
                        + favoritedStatus.getText());

                int newFavs = sharedPreferences.getInt("new_favorites", 0);
                newFavs++;
                sharedPreferences.edit().putInt("new_favorites", newFavs).commit();
                interactions.updateInteraction(mContext, source, favoritedStatus, settings.currentAccount, InteractionsDataSource.TYPE_FAVORITE);
                sharedPreferences.edit().putBoolean("new_notification", true).commit();

                if(settings.notifications) {
                    NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.favorited));
                }
            }
        }

        @Override
        public void onUnfavorite(User source, User target, Status unfavoritedStatus) {

        }

        @Override
        public void onFollow(User source, User followedUser) {
            Log.v("twitter_stream_push", "onFollow source:@"
                    + source.getScreenName() + " target:@"
                    + followedUser.getScreenName());

            if (followedUser.getScreenName().equals(settings.myScreenName)) {

                AppSettings settings = new AppSettings(mContext);

                int newFollows = sharedPreferences.getInt("new_follows", 0);
                newFollows++;
                sharedPreferences.edit().putInt("new_follows", newFollows).commit();
                interactions.createInteraction(mContext, source, null, settings.currentAccount, InteractionsDataSource.TYPE_FOLLOWER);
                sharedPreferences.edit().putBoolean("new_notification", true).commit();

                if (settings.notifications) {
                    NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.followed));
                }
            }
        }

        @Override
        public void onDirectMessage(DirectMessage directMessage) {
            Log.v("twitter_stream_push", "onDirectMessage text:"
                    + directMessage.getText());

            AppSettings settings = new AppSettings(mContext);

            DMDataSource dataSource = new DMDataSource(mContext);
            dataSource.open();
            dataSource.createDirectMessage(directMessage, settings.currentAccount);

            int numUnread = sharedPreferences.getInt("dm_unread_" + settings.currentAccount, 0);
            numUnread++;
            sharedPreferences.edit().putInt("dm_unread_" + settings.currentAccount, numUnread).commit();
            sharedPreferences.edit().putBoolean("refresh_me_dm", true).commit();

            if (settings.notifications) {
                NotificationUtils.refreshNotification(mContext);
            }

            dataSource.close();
        }

        @Override
        public void onUserListMemberAddition(User addedMember, User listOwner, UserList list) {

        }

        @Override
        public void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list) {

        }

        @Override
        public void onUserListSubscription(User subscriber, User listOwner, UserList list) {

        }

        @Override
        public void onUserListUnsubscription(User subscriber, User listOwner, UserList list) {

        }

        @Override
        public void onUserListCreation(User listOwner, UserList list) {

        }

        @Override
        public void onUserListUpdate(User listOwner, UserList list) {

        }

        @Override
        public void onUserListDeletion(User listOwner, UserList list) {

        }

        @Override
        public void onUserProfileUpdate(User updatedUser) {

        }

        @Override
        public void onBlock(User source, User blockedUser) {

        }

        @Override
        public void onUnblock(User source, User unblockedUser) {

        }

        @Override
        public void onException(Exception ex) {
            ex.printStackTrace();
            Log.v("twitter_stream_push", "onException:" + ex.getMessage());
        }
    };
}