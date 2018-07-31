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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.compose.WidgetCompose;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.redirects.RedirectToPopup;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.widget.WidgetProvider;


import java.util.ArrayList;

import twitter4j.DirectMessage;
import twitter4j.IDs;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterStream;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserMentionEntity;
import twitter4j.UserStreamListener;

public class TalonPullNotificationService extends Service {

    public static void start(Context context) {
        Intent pull = new Intent(context, TalonPullNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(pull);
        } else {
            context.startService(pull);
        }
    }

    public static final int FOREGROUND_SERVICE_ID = 11;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public TwitterStream pushStream;
    public Context mContext;
    public AppSettings settings;
    public SharedPreferences sharedPreferences;

    public Notification.Builder mBuilder;

    public static boolean shuttingDown = false;
    public static boolean isRunning = false;

    public boolean thisInstanceOn = true;

    public boolean showNotification;

    public ArrayList<Long> ids;
    public ArrayList<Long> blockedIds;

    @Override
    public void onCreate() {
        super.onCreate();

        if (TalonPullNotificationService.isRunning) {
            stopSelf();
            return;
        }

        TalonPullNotificationService.isRunning = true;

        settings = AppSettings.getInstance(this);

        sharedPreferences = AppSettings.getSharedPreferences(this);

        showNotification = sharedPreferences.getBoolean("show_pull_notification", true) ||
                Utils.isAndroidO();
        pullUnread = sharedPreferences.getInt("pull_unread", 0);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stop = new Intent(this, StopPull.class);
        PendingIntent stopPending = PendingIntent.getService(this, 0, stop, 0);

        Intent popup = new Intent(this, RedirectToPopup.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("from_notification", true);
        PendingIntent popupPending = PendingIntent.getActivity(this, 0, popup, 0);

        Intent compose = new Intent(this, WidgetCompose.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent composePending = PendingIntent.getActivity(this, 0, compose, 0);

        String text;

        int count = 0;

        if (sharedPreferences.getBoolean("is_logged_in_1", false)) {
            count++;
        }
        if (sharedPreferences.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        boolean multAcc = false;
        if (count == 2) {
            multAcc = true;
        }

        if (settings.liveStreaming && settings.timelineNot) {
            text = getResources().getString(R.string.new_tweets_upper) + ": " + pullUnread;
        } else {
            text = getResources().getString(R.string.listening_for_mentions) + "...";
        }

        mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.talon_pull) + (multAcc ? " - @" + settings.myScreenName : ""))
                        .setContentText(text)
                        .setPriority(Notification.PRIORITY_MIN)
                        .setWhen(0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setVisibility(Notification.VISIBILITY_SECRET);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder.setChannelId(NotificationChannelUtil.TALON_PULL_CHANNEL);
        }

        if (getApplicationContext().getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.addAction(R.drawable.ic_cancel_dark, getApplicationContext().getResources().getString(R.string.stop), stopPending);
            mBuilder.addAction(R.drawable.ic_popup, getResources().getString(R.string.popup), popupPending);
            mBuilder.addAction(R.drawable.ic_send_dark, getResources().getString(R.string.tweet), composePending);
        }

        mBuilder.setContentIntent(pendingIntent);

        if (showNotification)
            startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());

        mContext = getApplicationContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.STOP_PUSH");
        registerReceiver(stopPush, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.START_PUSH");
        registerReceiver(startPush, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.STOP_PUSH_SERVICE");
        registerReceiver(stopService, filter);

        if (settings.liveStreaming && settings.timelineNot) {
            filter = new IntentFilter();
            filter.addAction("com.klinker.android.twitter.UPDATE_NOTIF");
            registerReceiver(updateNotification, filter);

            filter = new IntentFilter();
            filter.addAction("com.klinker.android.twitter.NEW_TWEET");
            registerReceiver(updateNotification, filter);

            filter = new IntentFilter();
            filter.addAction("com.klinker.android.twitter.CLEAR_PULL_UNREAD");
            registerReceiver(clearPullUnread, filter);
        }

        TimeoutThread start = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                // get the ids of everyone you follow
                try {
                    Log.v("getting_ids", "started getting ids, mine: " + settings.myId);
                    Twitter twitter = Utils.getTwitter(mContext, settings);
                    long currCursor = -1;
                    IDs idObject;

                    ids = new ArrayList<Long>();
                    do {
                        idObject = twitter.getFriendsIDs(settings.myId, currCursor);

                        long[] lIds = idObject.getIDs();
                        for (int i = 0; i < lIds.length; i++) {
                            ids.add(lIds[i]);
                        }
                    } while ((currCursor = idObject.getNextCursor()) != 0);
                    ids.add(settings.myId);

                    currCursor = -1;
                    blockedIds = new ArrayList<Long>();
                    do {
                        idObject = twitter.getBlocksIDs(currCursor);

                        long[] lIds = idObject.getIDs();
                        for (int i = 0; i < lIds.length; i++) {
                            blockedIds.add(lIds[i]);
                        }
                    } while ((currCursor = idObject.getNextCursor()) != 0);

                    idsLoaded = true;

                    mContext.sendBroadcast(new Intent("com.klinker.android.twitter.START_PUSH"));
                } catch (Exception e) {
                    e.printStackTrace();
                    TalonPullNotificationService.isRunning = false;

                    pullUnread = 0;

                    TimeoutThread stop = new TimeoutThread(new Runnable() {
                        @Override
                        public void run() {
                            TalonPullNotificationService.shuttingDown = true;
                            try {
                                //pushStream.removeListener(userStream);
                            } catch (Exception x) {

                            }
                            try {
                                pushStream.cleanUp();
                                pushStream.shutdown();
                                Log.v("twitter_stream_push", "stopping push notifications");
                            } catch (Exception e) {
                                // it isn't running
                                e.printStackTrace();
                                // try twice to shut it down i guess
                                try {
                                    Thread.sleep(2000);
                                    pushStream.cleanUp();
                                    pushStream.shutdown();
                                    Log.v("twitter_stream_push", "stopping push notifications");
                                } catch (Exception x) {
                                    // it isn't running
                                    x.printStackTrace();
                                }
                            }

                            TalonPullNotificationService.shuttingDown = false;
                        }
                    });

                    stop.setPriority(Thread.MAX_PRIORITY);
                    stop.start();

                    stopSelf();
                } catch (OutOfMemoryError e) {
                    TalonPullNotificationService.isRunning = false;

                    TimeoutThread stop = new TimeoutThread(new Runnable() {
                        @Override
                        public void run() {
                            TalonPullNotificationService.shuttingDown = true;
                            try {
                                //pushStream.removeListener(userStream);
                            } catch (Exception x) {

                            }
                            try {
                                pushStream.cleanUp();
                                pushStream.shutdown();
                                Log.v("twitter_stream_push", "stopping push notifications");
                            } catch (Exception e) {
                                // it isn't running
                                e.printStackTrace();
                                // try twice to shut it down i guess
                                try {
                                    Thread.sleep(2000);
                                    pushStream.cleanUp();
                                    pushStream.shutdown();
                                    Log.v("twitter_stream_push", "stopping push notifications");
                                } catch (Exception x) {
                                    // it isn't running
                                    x.printStackTrace();
                                }
                            }

                            TalonPullNotificationService.shuttingDown = false;
                        }
                    });

                    stop.setPriority(Thread.MAX_PRIORITY);
                    stop.start();

                    pullUnread = 0;

                    stopSelf();
                }

            }
        });

        start.setPriority(Thread.MAX_PRIORITY - 1);
        start.start();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
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

                    pullUnread = 0;
                }
            });

            thisInstanceOn = false;
        }
    };

    public int pullUnread = 0;

    public BroadcastReceiver updateNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v("twitter_stream_push", "updating notification");

            mBuilder.setContentText(getResources().getString(R.string.new_tweets_upper) + ": " + pullUnread);

            if (showNotification)
                startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());
        }
    };

    public BroadcastReceiver clearPullUnread = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            pullUnread = 0;
            sharedPreferences.edit().putInt("pull_unread", 0).apply();

            mBuilder.setContentText(getResources().getString(R.string.new_tweets_upper) + ": " + pullUnread);

            if (showNotification)
                startForeground(FOREGROUND_SERVICE_ID, mBuilder.build());
        }
    };

    public BroadcastReceiver stopService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            TimeoutThread stop = new TimeoutThread(new Runnable() {
                @Override
                public void run() {
                    TalonPullNotificationService.shuttingDown = true;
                    try {
                        pushStream.cleanUp();
                        pushStream.shutdown();
                        Log.v("twitter_stream_push", "stopping push notifications");
                    } catch (Exception e) {
                        // it isn't running
                        e.printStackTrace();
                        // try twice to shut it down i guess
                        try {
                            Thread.sleep(2000);
                            pushStream.cleanUp();
                            pushStream.shutdown();
                            Log.v("twitter_stream_push", "stopping push notifications");
                        } catch (Exception x) {
                            // it isn't running
                            x.printStackTrace();
                        }
                    }

                    TalonPullNotificationService.shuttingDown = false;
                }
            });

            stop.setPriority(Thread.MAX_PRIORITY);
            stop.start();

            TalonPullNotificationService.isRunning = false;
            thisInstanceOn = false;

            sharedPreferences.edit().putInt("pull_unread", pullUnread).apply();
            pullUnread = 0;

            new TimeoutThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //pushStream.removeListener(userStream);
                    } catch (Exception x) {

                    }
                }
            }).start();
            stopSelf();

        }
    };

    public BroadcastReceiver startPush = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            thisInstanceOn = true;

            new TimeoutThread(new Runnable() {
                @Override
                public void run() {
                    while (TalonPullNotificationService.shuttingDown) {
                        try {
                            Thread.sleep(1500);
                        } catch (Exception e) {

                        }
                    }

                    settings = AppSettings.getInstance(mContext);
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
        public void onStatus(final Status status) {

            if (!thisInstanceOn || isUserBlocked(status.getUser().getId())) {
                return;
            }

            UserMentionEntity[] entities = status.getUserMentionEntities();
            ArrayList<String> names = new ArrayList<String>();
            for (UserMentionEntity e : entities) {
                names.add(e.getScreenName());
            }
            if(names.contains(settings.myScreenName)) {
                Log.v("twitter_stream_push", "onStatus @" + status.getUser().getScreenName() + " - " + status.getText());

                if (!status.isRetweet()) { // it is a normal mention

                    MentionsDataSource mentions = MentionsDataSource.getInstance(mContext);

                    if (!mentions.tweetExists(status.getId(), sharedPreferences.getInt("current_account", 1))) {
                        mentions.createTweet(status, sharedPreferences.getInt("current_account", 1));
                    }
                    InteractionsDataSource.getInstance(mContext).createMention(mContext, status, sharedPreferences.getInt("current_account", 1));
                    sharedPreferences.edit().putBoolean("new_notification", true).apply();
                    sharedPreferences.edit().putBoolean("refresh_me_mentions", true).apply();

                    if(settings.notifications && settings.mentionsNot && !sharedPreferences.getString("muted_users", "").contains(status.getUser().getScreenName())) {
                        NotificationUtils.refreshNotification(mContext);
                    }

                    mContext.sendBroadcast(new Intent("com.klinker.android.twitter.NEW_MENTION"));

                } else { // it is a retweet
                    if (!status.getUser().getScreenName().equals(settings.myScreenName) && status.getRetweetedStatus().getUser().getScreenName().equals(settings.myScreenName)) {
                        if (settings.retweetNot) {
                            int newRetweets = sharedPreferences.getInt("new_retweets", 0);
                            newRetweets++;
                            sharedPreferences.edit().putInt("new_retweets", newRetweets).apply();
                        }

                        InteractionsDataSource.getInstance(mContext).updateInteraction(mContext, status.getUser(), status, sharedPreferences.getInt("current_account", 1), InteractionsDataSource.TYPE_RETWEET);
                        sharedPreferences.edit().putBoolean("new_notification", true).apply();

                        if(settings.notifications && settings.retweetNot) {
                            NotificationUtils.newInteractions(status.getUser(), mContext, sharedPreferences, " " + getResources().getString(R.string.retweeted));
                        }
                    }
                }
            }

            if (settings.liveStreaming && idsLoaded) {
                Long mId = status.getUser().getId();
                if (ids.contains(mId)) {
                    int currentAccount = sharedPreferences.getInt("current_account", 1);
                    HomeDataSource home = HomeDataSource.getInstance(mContext);
                    if (!home.tweetExists(status.getId(), currentAccount)) {
                        //HomeContentProvider.insertTweet(status, currentAccount, mContext);
                        home.createTweet(status, currentAccount);
                        sharedPreferences.edit().putLong("account_" + currentAccount + "_lastid", status.getId()).apply();
                        getContentResolver().notifyChange(HomeContentProvider.STREAM_NOTI, null);
                        getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);
                    }

                    pullUnread++;
                    sharedPreferences.edit().putInt("pull_unread", pullUnread).apply();
                    mContext.sendBroadcast(new Intent("com.klinker.android.twitter.NEW_TWEET"));
                    mContext.sendBroadcast(new Intent("com.klinker.android.twitter.UPDATE_NOTIF"));
                    WidgetProvider.updateWidget(mContext);

                    sharedPreferences.edit().putBoolean("refresh_me", true).apply();

                    boolean favUser = !status.isRetweet() && FavoriteUsersDataSource.getInstance(mContext).isFavUser(status.getUser().getScreenName());
                    if (favUser && settings.favoriteUserNotifications && settings.notifications) {
                        NotificationUtils.favUsersNotification(sharedPreferences.getInt("current_account", 1), mContext, -1);
                    }

                    if (favUser) {
                        InteractionsDataSource.getInstance(mContext).createFavoriteUserInter(mContext, status, sharedPreferences.getInt("current_account", 1));
                        sharedPreferences.edit().putBoolean("new_notification", true).apply();
                    }

                    if (settings.preCacheImages) {
                        new TimeoutThread(new Runnable() {
                            @Override
                            public void run() {
                                downloadImages(status);
                            }
                        }).start();
                    }
                }
            }
        }

        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            /*HomeDataSource.getInstance(mContext)
                    .deleteTweet(statusDeletionNotice.getStatusId());
            sharedPreferences.edit().putBoolean("refresh_me", true).apply();*/
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

            if (!thisInstanceOn || isUserBlocked(source.getId())) {
                return;
            }

            if(!source.getScreenName().equals(settings.myScreenName) && target.getScreenName().equals(settings.myScreenName)) {
                AppSettings settings = new AppSettings(mContext);

                Log.v("twitter_stream_push", "onFavorite source:@"
                        + source.getScreenName() + " target:@"
                        + target.getScreenName() + " @"
                        + favoritedStatus.getUser().getScreenName() + " - "
                        + favoritedStatus.getText());

                InteractionsDataSource.getInstance(mContext).updateInteraction(mContext,
                        source,
                        favoritedStatus,
                        sharedPreferences.getInt("current_account", 1),
                        InteractionsDataSource.TYPE_FAVORITE);

                sharedPreferences.edit().putBoolean("new_notification", true).apply();

                if (settings.favoritesNot) {
                    int newFavs = sharedPreferences.getInt("new_favorites", 0);
                    newFavs++;
                    sharedPreferences.edit().putInt("new_favorites", newFavs).apply();

                    if(settings.notifications) {
                        NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.favorited));
                    }
                }
            }
        }

        @Override
        public void onQuotedTweet(User source, User target, Status status) {
            if (!thisInstanceOn || isUserBlocked(source.getId())) {
                return;
            }

            if(!source.getScreenName().equals(settings.myScreenName) && target.getScreenName().equals(settings.myScreenName)) {
                AppSettings settings = new AppSettings(mContext);

                Log.v("twitter_stream_push", "onQuote source:@"
                        + source.getScreenName() + " target:@"
                        + target.getScreenName() + " @"
                        + status.getUser().getScreenName() + " - "
                        + status.getText());

                InteractionsDataSource.getInstance(mContext).updateInteraction(mContext,
                        source,
                        status,
                        sharedPreferences.getInt("current_account", 1),
                        InteractionsDataSource.TYPE_QUOTED_TWEET);

                sharedPreferences.edit().putBoolean("new_notification", true).apply();

                if (settings.mentionsNot) {
                    int newQuotes = sharedPreferences.getInt("new_quotes", 0);
                    newQuotes++;
                    sharedPreferences.edit().putInt("new_quotes", newQuotes).apply();

                    if(settings.notifications) {
                        NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.quoted));
                    }
                }
            }
        }

        @Override
        public void onUnfavorite(User source, User target, Status unfavoritedStatus) {

        }

        @Override
        public void onFollow(User source, User followedUser) {

            if (!thisInstanceOn) {
                return;
            }

            Log.v("twitter_stream_push", "onFollow source:@"
                    + source.getScreenName() + " target:@"
                    + followedUser.getScreenName());

            if (followedUser.getScreenName().equals(settings.myScreenName)) {

                AppSettings settings = new AppSettings(mContext);

                InteractionsDataSource.getInstance(mContext).createInteraction(mContext,
                        source,
                        null,
                        sharedPreferences.getInt("current_account", 1),
                        InteractionsDataSource.TYPE_FOLLOWER);

                sharedPreferences.edit().putBoolean("new_notification", true).apply();

                if (settings.followersNot) {
                    int newFollows = sharedPreferences.getInt("new_followers", 0);
                    newFollows++;
                    sharedPreferences.edit().putInt("new_followers", newFollows).apply();

                    if (settings.notifications) {
                        NotificationUtils.newInteractions(source, mContext, sharedPreferences, " " + getResources().getString(R.string.followed));
                    }
                }
            }
        }

        @Override
        public void onUnfollow(User user, User user2) {

        }

        @Override
        public void onDirectMessage(DirectMessage directMessage) {

            if (!thisInstanceOn) {
                return;
            }

            Log.v("twitter_stream_push", "onDirectMessage text:"
                    + directMessage.getText());

            AppSettings settings = new AppSettings(mContext);

//            DMDataSource.getInstance(mContext).createDirectMessage(directMessage, sharedPreferences.getInt("current_account", 1));

            int numUnread = sharedPreferences.getInt("dm_unread_" + sharedPreferences.getInt("current_account", 1), 0);
            numUnread++;
            sharedPreferences.edit().putInt("dm_unread_" + sharedPreferences.getInt("current_account", 1), numUnread).apply();
            sharedPreferences.edit().putBoolean("refresh_me_dm", true).apply();


            sharedPreferences.edit().putLong("last_direct_message_id_" + sharedPreferences.getInt("current_account", 1), directMessage.getId()).apply();

            if (!directMessage.getSender().getScreenName().equals(settings.myScreenName) &&
                    settings.notifications &&
                    settings.dmsNot) {

                NotificationUtils.refreshNotification(mContext);
            }

            mContext.sendBroadcast(new Intent("com.klinker.android.twitter.NEW_DIRECT_MESSAGE"));
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
        public void onUserSuspension(long suspendedUser) {

        }

        @Override
        public void onUserDeletion(long deletedUser) {

        }

        @Override
        public void onBlock(User source, User blockedUser) {

        }

        @Override
        public void onUnblock(User source, User unblockedUser) {

        }

        @Override
        public void onRetweetedRetweet(User user, User user1, Status status) {

        }

        @Override
        public void onFavoritedRetweet(User user, User user1, Status status) {

        }

        @Override
        public void onException(Exception ex) {
            ex.printStackTrace();
            Log.v("twitter_stream_push", "onException:" + ex.getMessage());

            // schedule an alarm to try to restart again since this one failed, probably no data connection
            /*AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

            long now = Calendar.getInstance().getTimeInMillis();
            long alarm = now + 300000; // schedule it to begin in 5 mins

            PendingIntent pendingIntent = PendingIntent.getService(mContext, 236, new Intent(mContext, CatchupPull.class), 0);

            am.cancel(pendingIntent); // cancel the old one, then start the new one in 1 min
            am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);*/

            //pushStream.clearListeners();
            //pushStream.shutdown();
            //pushStream.cleanUp();
        }
    };

    public void downloadImages(Status status) {
        String profilePic = status.getUser().getBiggerProfileImageURL();
        String imageUrl = TweetLinkUtils.getLinksInStatus(status)[1];

        Glide.with(this).load(profilePic).downloadOnly(1000, 1000);
        Glide.with(this).load(imageUrl).downloadOnly(1000, 1000);
    }

    public boolean isUserBlocked(Long userId) {
        try {
            return blockedIds.contains(userId);
        } catch (Exception e) {
            return false;
        }
    }
}