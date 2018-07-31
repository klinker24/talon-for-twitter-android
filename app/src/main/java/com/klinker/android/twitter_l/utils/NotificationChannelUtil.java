package com.klinker.android.twitter_l.utils;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.StringRes;

import com.klinker.android.twitter_l.R;

@TargetApi(Build.VERSION_CODES.O)
public class NotificationChannelUtil {

    public static final String INTERACTIONS_CHANNEL = "interactions-channel";
    public static final String MENTIONS_CHANNEL = "mentions-channel";
    public static final String DIRECT_MESSAGES_CHANNEL = "direct-messages-channel";
    public static final String BACKGROUND_REFRESH_CHANNEL = "background-refresh-channel";
    public static final String TWEETING_NOTIFICATION_CHANNEL = "tweeting-channel";
    public static final String FAILED_TWEETS_CHANNEL = "failed-tweets-channel";
    public static final String SENDING_SCHEDULED_MESSAGE_CHANNEL = "sending-scheduled-message-channel";
    public static final String MEDIA_DOWNLOAD_CHANNEL = "media-download-channel";
    public static final String WIDGET_REFRESH_CHANNEL = "widget-refresh-channel";
    public static final String FAVORITE_USERS_CHANNEL = "favorite-users-channel";

    public static void createNotificationChannels(Context context) {
        if (!Utils.isAndroidO()) {
            return;
        }

        createChannel(context, INTERACTIONS_CHANNEL, R.string.interactions_channel, NotificationManager.IMPORTANCE_HIGH);
        createChannel(context, MENTIONS_CHANNEL, R.string.mentions_channel, NotificationManager.IMPORTANCE_HIGH);
        createChannel(context, DIRECT_MESSAGES_CHANNEL, R.string.direct_messages_channel, NotificationManager.IMPORTANCE_HIGH);
        createChannel(context, BACKGROUND_REFRESH_CHANNEL, R.string.background_refresh_channel, NotificationManager.IMPORTANCE_DEFAULT);
        createChannel(context, TWEETING_NOTIFICATION_CHANNEL, R.string.tweeting_notifications_channel, NotificationManager.IMPORTANCE_LOW);
        createChannel(context, FAILED_TWEETS_CHANNEL, R.string.tweet_failure_channel, NotificationManager.IMPORTANCE_HIGH);
        createChannel(context, SENDING_SCHEDULED_MESSAGE_CHANNEL, R.string.scheduled_messages_channel, NotificationManager.IMPORTANCE_MIN);
        createChannel(context, MEDIA_DOWNLOAD_CHANNEL, R.string.media_downloads_channel, NotificationManager.IMPORTANCE_DEFAULT);
        createChannel(context, WIDGET_REFRESH_CHANNEL, R.string.widget_refresh_channel, NotificationManager.IMPORTANCE_MIN);
        createChannel(context, FAVORITE_USERS_CHANNEL, R.string.favorite_user_channel, NotificationManager.IMPORTANCE_DEFAULT);
    }

    private static void createChannel(Context context, String channelId, @StringRes int title, int priority) {
        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel testChannel = new NotificationChannel(channelId, context.getString(title), priority);
        manager.createNotificationChannel(testChannel);
    }
}
