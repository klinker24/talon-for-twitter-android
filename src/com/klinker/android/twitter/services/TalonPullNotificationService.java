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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.utils.NotificationUtils;
import com.klinker.android.twitter.utils.RedirectToPopup;
import com.klinker.android.twitter.utils.Utils;


import java.util.ArrayList;
import java.util.Arrays;

import twitter4j.DirectMessage;
import twitter4j.IDs;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
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
    public HomeDataSource home;
    public MentionsDataSource mentions;
    public FavoriteUsersDataSource favs;

    public NotificationCompat.Builder mBuilder;

    public static boolean shuttingDown = false;

    public ArrayList<Long> ids;

    @Override
    public void onCreate() {
        super.onCreate();

        settings = new AppSettings(this);
        home = new HomeDataSource(this);
        home.open();

        favs = new FavoriteUsersDataSource(this);
        favs.open();

        mentions = new MentionsDataSource(this);
        mentions.open();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stop = new Intent(this, StopPull.class);
        PendingIntent stopPending = PendingIntent.getService(this, 0, stop, 0);

        Intent popup = new Intent(this, RedirectToPopup.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("from_notification", true);
        PendingIntent popupPending = PendingIntent.getActivity(this, 0, popup, 0);


        /*RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
        remoteView.setOnClickPendingIntent(R.id.popup_button, stopPending);
        remoteView.setImageViewResource(R.id.icon, R.drawable.ic_stat_icon);*/

        String text;

        if (settings.liveStreaming && settings.timelineNot) {
            text = getResources().getString(R.string.new_tweets_upper) + ": " + pullUnread;
        } else {
            text = getResources().getString(R.string.listening_for_mentions) + "...";
        }

        mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.color.transparent)
                        .setContentTitle(getResources().getString(R.string.talon_pull))
                        .setContentText(text)
                        .setOngoing(true)
                        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_stat_icon));


        if (getApplicationContext().getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.addAction(R.drawable.ic_cancel_dark, getApplicationContext().getResources().getString(R.string.stop), stopPending);
            mBuilder.addAction(R.drawable.ic_popup, getResources().getString(R.string.popup), popupPending);
        }

        try {
            mBuilder.setWhen(0);
        } catch (Exception e) { }

        mBuilder.setContentIntent(pendingIntent);

        // priority flag is only available on api level 16 and above
        if (getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.setPriority(Notification.PRIORITY_MIN);
        }

        startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());

        mContext = getApplicationContext();
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

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.CLEAR_PULL_UNREAD");
        registerReceiver(clearPullUnread, filter);

        if (settings.liveStreaming && settings.timelineNot) {
            filter = new IntentFilter();
            filter.addAction("com.klinker.android.twitter.UPDATE_NOTIF");
            registerReceiver(updateNotification, filter);

            filter = new IntentFilter();
            filter.addAction("com.klinker.android.twitter.NEW_TWEET");
            registerReceiver(updateNotification, filter);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                // get the ids of everyone you follow
                try {
                    Log.v("getting_ids", "started getting ids, mine: " + settings.myId);
                    Twitter twitter = Utils.getTwitter(mContext, settings);
                    long currCursor = -1;
                    IDs idObject;
                    int rep = 0;

                    do {
                        idObject = twitter.getFriendsIDs(settings.myId, currCursor);
                        long[] lIds = idObject.getIDs();
                        ids = new ArrayList<Long>();
                        for (int i = 0; i < lIds.length; i++) {
                            Log.v("getting_ids", i + ": " + lIds[i]);
                            ids.add(lIds[i]);
                        }

                        rep++;
                    } while ((currCursor = idObject.getNextCursor()) != 0 && rep < 3);

                    ids.add(settings.myId);

                    idsLoaded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        mContext.sendBroadcast(new Intent("com.klinker.android.twitter.START_PUSH"));

    }

    public boolean idsLoaded = false;

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
            unregisterReceiver(updateNotification);
        } catch (Exception e) { }
        try {
            unregisterReceiver(clearPullUnread);
        } catch (Exception e) { }

        try {
            interactions.close();
        } catch (Exception e) { }

        try {
            home.close();
        } catch (Exception e) { }

        try {
            favs.close();
        } catch (Exception e) { }

        super.onDestroy();
    }

    public BroadcastReceiver stopPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        pushStream.cleanUp();
                        pushStream.shutdown();
                        Log.v("twitter_stream_push", "stopping push notifications");
                    } catch (Exception e) {
                        // it isn't running
                    }
                }
            });

            try {
                interactions.close();
            } catch (Exception e) { }
        }
    };

    public int pullUnread = 0;

    public BroadcastReceiver updateNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v("twitter_stream_push", "updating notification");

            mBuilder.setContentText(getResources().getString(R.string.new_tweets_upper) + ": " + pullUnread);

            startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());
        }
    };

    public BroadcastReceiver clearPullUnread = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            pullUnread = 0;

            mBuilder.setContentText(getResources().getString(R.string.new_tweets_upper) + ": " + pullUnread);

            startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());
        }
    };

    public BroadcastReceiver stopService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    TalonPullNotificationService.shuttingDown = true;
                    try {
                        pushStream.cleanUp();
                        pushStream.shutdown();
                        Log.v("twitter_stream_push", "stopping push notifications");
                    } catch (Exception e) {
                        // it isn't running

                    }
                    TalonPullNotificationService.shuttingDown = false;
                }
            }).start();

            stopSelf();
        }
    };

    public BroadcastReceiver startPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (TalonPullNotificationService.shuttingDown) {
                        try {
                            Thread.sleep(1500);
                        } catch (Exception e) {

                        }
                    }

                    try {
                        interactions.open();
                    } catch (Exception e) { }

                    settings = new AppSettings(mContext);
                    pushStream = Utils.getStreamingTwitter(mContext, settings);

                    String myName = settings.myScreenName;
                    Log.v("twitter_stream_push", "my id: " + myName + "");

                    pushStream.addListener(userStream);
                    //pushStream.filter(new FilterQuery().track(new String[]{myName}));
                    pushStream.user(new String[] {myName});
                    Log.v("twitter_stream_push", "started push notifications");
                }
            }).start();

        }
    };

    public UserStreamListener userStream = new UserStreamListener() {
        @Override
        public void onStatus(Status status) {
            if(status.getText().toLowerCase().contains("@" + settings.myScreenName.toLowerCase())) {
                Log.v("twitter_stream_push", "onStatus @" + status.getUser().getScreenName() + " - " + status.getText());

                if (!status.isRetweet()) { // it is a normal mention
                    if (!mentions.tweetExists(status.getId(), sharedPreferences.getInt("current_account", 1))) {
                        mentions.createTweet(status, sharedPreferences.getInt("current_account", 1));
                    }
                    interactions.createMention(mContext, status, sharedPreferences.getInt("current_account", 1));
                    sharedPreferences.edit().putBoolean("new_notification", true).commit();
                    sharedPreferences.edit().putBoolean("refresh_me_mentions", true).commit();

                    if(settings.notifications && settings.mentionsNot) {
                        NotificationUtils.refreshNotification(mContext);
                    }

                } else { // it is a retweet
                    if (!status.getUser().getScreenName().equals(settings.myScreenName) && status.getRetweetedStatus().getUser().getScreenName().equals(settings.myScreenName)) {
                        if (settings.retweetNot) {
                            int newRetweets = sharedPreferences.getInt("new_retweets", 0);
                            newRetweets++;
                            sharedPreferences.edit().putInt("new_retweets", newRetweets).commit();
                        }

                        interactions.updateInteraction(mContext, status.getUser(), status, sharedPreferences.getInt("current_account", 1), InteractionsDataSource.TYPE_RETWEET);
                        sharedPreferences.edit().putBoolean("new_notification", true).commit();

                        if(settings.notifications && settings.retweetNot) {
                            NotificationUtils.newInteractions(status.getUser(), mContext, sharedPreferences, " " + getResources().getString(R.string.retweeted));
                        }
                    }
                }
            }

            if (settings.liveStreaming && idsLoaded) {
                Long mId = status.getUser().getId();
                if (!(status.isRetweet() && home.tweetExists(status.getRetweetedStatus().getId(), sharedPreferences.getInt("current_account", 1))) &&
                        ids.contains(mId)) {
                    home.createTweet(status, sharedPreferences.getInt("current_account", 1));

                    pullUnread++;
                    mContext.sendBroadcast(new Intent("com.klinker.android.twitter.NEW_TWEET"));
                    mContext.sendBroadcast(new Intent("com.klinker.android.twitter.UPDATE_NOTIF"));
                    mContext.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));

                    sharedPreferences.edit().putBoolean("refresh_me", true).commit();
                    sharedPreferences.edit().putLong("second_last_tweet_id_" + sharedPreferences.getInt("current_account", 1), home.getLastIds(sharedPreferences.getInt("current_account", 1))[0]);

                    if (favs.isFavUser(sharedPreferences.getInt("current_account", 1), status.getUser().getScreenName()) && settings.favoriteUserNotifications && settings.notifications) {
                        NotificationUtils.favUsersNotification(sharedPreferences.getInt("current_account", 1), mContext);
                        interactions.createFavoriteUserInter(mContext, status, sharedPreferences.getInt("current_account", 1));
                        sharedPreferences.edit().putBoolean("new_notification", true).commit();
                    }
                }
            }
        }

        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            try {
                home.deleteTweet(statusDeletionNotice.getStatusId());
                sharedPreferences.edit().putBoolean("refresh_me", true).commit();
            } catch (Exception e) { }
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

                interactions.updateInteraction(mContext, source, favoritedStatus, sharedPreferences.getInt("current_account", 1), InteractionsDataSource.TYPE_FAVORITE);
                sharedPreferences.edit().putBoolean("new_notification", true).commit();

                if (settings.favoritesNot) {
                    int newFavs = sharedPreferences.getInt("new_favorites", 0);
                    newFavs++;
                    sharedPreferences.edit().putInt("new_favorites", newFavs).commit();

                    if(settings.notifications) {
                        NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.favorited));
                    }
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

                interactions.createInteraction(mContext, source, null, sharedPreferences.getInt("current_account", 1), InteractionsDataSource.TYPE_FOLLOWER);
                sharedPreferences.edit().putBoolean("new_notification", true).commit();

                if (settings.followersNot) {
                    int newFollows = sharedPreferences.getInt("new_follows", 0);
                    newFollows++;
                    sharedPreferences.edit().putInt("new_follows", newFollows).commit();

                    if (settings.notifications) {
                        NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.followed));
                    }
                }
            }
        }

        @Override
        public void onDirectMessage(DirectMessage directMessage) {
            if (!directMessage.getSender().getScreenName().equals(settings.myScreenName)) {
                Log.v("twitter_stream_push", "onDirectMessage text:"
                        + directMessage.getText());

                AppSettings settings = new AppSettings(mContext);

                DMDataSource dataSource = new DMDataSource(mContext);
                dataSource.open();
                dataSource.createDirectMessage(directMessage, sharedPreferences.getInt("current_account", 1));

                int numUnread = sharedPreferences.getInt("dm_unread_" + sharedPreferences.getInt("current_account", 1), 0);
                numUnread++;
                sharedPreferences.edit().putInt("dm_unread_" + sharedPreferences.getInt("current_account", 1), numUnread).commit();
                sharedPreferences.edit().putBoolean("refresh_me_dm", true).commit();


                sharedPreferences.edit().putLong("last_direct_message_id_" + sharedPreferences.getInt("current_account", 1), directMessage.getId()).commit();

                if (settings.notifications && settings.dmsNot) {
                    NotificationUtils.refreshNotification(mContext);
                }

                dataSource.close();
            }
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