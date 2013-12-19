package com.klinker.android.talon.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import com.klinker.android.talon.R;
import com.klinker.android.talon.services.MarkReadService;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.ComposeActivity;
import com.klinker.android.talon.ui.ComposeDMActivity;
import com.klinker.android.talon.ui.MainActivity;

public class NotificationUtils {

    public static void refreshNotification(Context context) {
        AppSettings settings = new AppSettings(context);

        /*RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
        Intent popup = new Intent(context, MainActivityPopup.class);
        popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("from_notification", true);
        PendingIntent popupPending =
                PendingIntent.getActivity(
                        this,
                        0,
                        popup,
                        0
                );
        remoteView.setOnClickPendingIntent(R.id.popup_button, popupPending);
        remoteView.setTextViewText(R.id.content, numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_tweet) : numberNew + " " + getResources().getString(R.string.new_tweets));

        remoteView.setImageViewResource(R.id.icon, R.drawable.timeline_dark);

        use .setContent(remoteView) to make the notification using this instead*/

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        int[] unreadCounts = getUnreads(context);
        String shortText = getShortText(unreadCounts, context, currentAccount);
        String longText = getLongText(unreadCounts, context, currentAccount);
        // [0] is the full title and [1] is the screenname
        String[] title = getTitle(unreadCounts, context, currentAccount);
        boolean useExpanded = useExp(unreadCounts, context);
        boolean addButton = addBtn(unreadCounts);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.putExtra("from_notification", true);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        0
                );

        Notification.Builder mBuilder;

        if (useExpanded) {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title[0])
                    .setContentText(shortText)
                    .setSmallIcon(R.drawable.timeline_dark)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(Html.fromHtml(longText)));

            if (addButton) {

                Intent reply;
                if (unreadCounts[1] == 1) {
                    reply = new Intent(context, ComposeActivity.class);
                } else {
                    reply = new Intent(context, ComposeDMActivity.class);
                }

                reply.setAction(Intent.ACTION_SEND);
                reply.setType("text/plain");
                reply.putExtra(Intent.EXTRA_TEXT, "@" + title[1] + " ");
                reply.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent replyPending = PendingIntent.getActivity(context, 0, reply, 0);

                mBuilder.addAction(R.drawable.ic_action_reply_dark, context.getResources().getString(R.string.noti_reply), replyPending);

                Intent markRead = new Intent(context, MarkReadService.class);
                PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

                mBuilder.addAction(R.drawable.ic_action_read, context.getResources().getString(R.string.mark_read), readPending);
            }
        } else {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title[0])
                    .setContentText(shortText)
                    .setSmallIcon(R.drawable.timeline_dark)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true);
        }

        int count = 0;

        if (settings.vibrate)
            count++;
        if (settings.sound)
            count++;

        int homeTweets = unreadCounts[0];
        int mentionsTweets = unreadCounts[1];
        int dmTweets = unreadCounts[2];

        int newC = 0;

        if (homeTweets > 0) {
            newC++;
        }
        if (mentionsTweets > 0) {
            newC++;
        }
        if (dmTweets > 0) {
            newC++;
        }

        if (settings.notifications && newC > 0) {
            switch (count) {

                case 2:
                    if (settings.vibrate && settings.sound)
                        mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND);
                    break;
                case 1:
                    if (settings.vibrate)
                        mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
                    else if (settings.sound)
                        mBuilder.setDefaults(Notification.DEFAULT_SOUND);
                    break;

                default:
                    break;
            }

            if (settings.led)
                mBuilder.setLights(0xFFFFFF, 1000, 1000);

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(1, mBuilder.build());

            sharedPrefs.edit().putBoolean("refresh_me", true).commit();

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }
        }
    }

    public static boolean addBtn(int[] unreadCount) {
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        return mentionsTweets == 1 || dmTweets == 1;
    }

    public static boolean useExp(int[] unreadCount, Context context) {
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        int count = 0;

        if (homeTweets > 0) {
            count++;
        }
        if (mentionsTweets > 0) {
            count++;
        }
        if (dmTweets > 0) {
            count++;
        }

        if ((count > 1 || mentionsTweets > 0 || dmTweets > 0) && context.getResources().getBoolean(R.bool.expNotifications)) {
            return true;
        } else {
            return false;
        }
    }

    public static int[] getUnreads(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        HomeDataSource data = new HomeDataSource(context);
        data.open();
        int homeTweets = data.getUnreadCount(currentAccount);
        data.close();

        MentionsDataSource mentions = new MentionsDataSource(context);
        mentions.open();
        int mentionsTweets = mentions.getUnreadCount(currentAccount);
        mentions.close();

        int dmTweets = sharedPrefs.getInt("dm_unread_" + currentAccount, 0);

        return new int[] {homeTweets, mentionsTweets, dmTweets};
    }

    public static String[] getTitle(int[] unreadCount, Context context, int currentAccount) {
        String text = "";
        String name = null;
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        // they only have a new mention
        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) {
            MentionsDataSource mentions = new MentionsDataSource(context);
            mentions.open();
            name = mentions.getNewestName(currentAccount);
            text = context.getResources().getString(R.string.mentioned_by) + " @" + name;
            mentions.close();
        } else if (homeTweets == 0 && mentionsTweets == 0 && dmTweets == 1) { // they have 1 new direct message
            DMDataSource dm = new DMDataSource(context);
            dm.open();
            name = dm.getNewestName(currentAccount);
            text = context.getResources().getString(R.string.message_from) + " @" + name;
            dm.close();
        } else { // other cases we will just put talon
            text = context.getResources().getString(R.string.app_name);
        }

        return new String[] {text, name};
    }

    public static String getShortText(int[] unreadCount, Context context, int currentAccount) {
        String text = "";
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) { // display the new mention
            MentionsDataSource mentions = new MentionsDataSource(context);
            mentions.open();
            text = mentions.getNewestMessage(currentAccount);
            mentions.close();
        } else if (dmTweets == 1 && mentionsTweets == 0 && homeTweets == 0) { // display the new message
            DMDataSource dm = new DMDataSource(context);
            dm.open();
            text = dm.getNewestMessage(currentAccount);
            dm.close();
        } else if (homeTweets > 0 && mentionsTweets == 0 && dmTweets == 0) { // it is just tweets being displayed, so put new out front
            text = homeTweets + " " + (homeTweets == 1 ? context.getResources().getString(R.string.new_tweet) : context.getResources().getString(R.string.new_tweets));
        } else {
            // home tweets
            if(homeTweets > 0) {
                text += homeTweets + " " + (homeTweets == 1 ? context.getResources().getString(R.string.tweet) : context.getResources().getString(R.string.tweets)) +
                        (mentionsTweets > 0 || dmTweets > 0 ? ", " : "");
            }

            // mentions
            if(mentionsTweets > 0) {
                text += mentionsTweets + " " + (mentionsTweets == 1 ? context.getResources().getString(R.string.mention) : context.getResources().getString(R.string.mentions)) +
                        (dmTweets > 0 ? ", " : "");
            }

            // direct messages
            if (dmTweets > 0) {
                text += dmTweets + " " + (dmTweets == 1 ? context.getResources().getString(R.string.message) : context.getResources().getString(R.string.messages));
            }
        }

        return text;
    }

    public static String getLongText(int[] unreadCount, Context context, int currentAccount) {

        String body = "";
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) { // display the new mention
            MentionsDataSource mentions = new MentionsDataSource(context);
            mentions.open();
            body = mentions.getNewestMessage(currentAccount);
            mentions.close();
        } else if (dmTweets == 1 && mentionsTweets == 0 && homeTweets == 0) { // display the new message
            DMDataSource dm = new DMDataSource(context);
            dm.open();
            body = dm.getNewestMessage(currentAccount);
            dm.close();
        } else {
            if (homeTweets > 0) {
                body += "<b>" + context.getResources().getString(R.string.timeline) + ": </b>" + homeTweets + " " + (homeTweets == 1 ? context.getResources().getString(R.string.new_tweet) : context.getResources().getString(R.string.new_tweets)) + (mentionsTweets > 0 || dmTweets > 0 ? "<br>" : "");
            }

            if (mentionsTweets > 0) {
                body += "<b>" + context.getResources().getString(R.string.mentions) + ": </b>" + mentionsTweets + " " + (mentionsTweets == 1 ? context.getResources().getString(R.string.new_mention) : context.getResources().getString(R.string.new_mentions)) + (dmTweets > 0 ? "<br>" : "");
            }

            if (dmTweets > 0) {
                body += "<b>" + context.getResources().getString(R.string.direct_messages) + ": </b>" + dmTweets + " " + (dmTweets == 1 ? context.getResources().getString(R.string.new_message) : context.getResources().getString(R.string.new_messages));
            }
        }
        return body;
    }

    public static Bitmap getIcon() {
        return null;
    }
}
