package com.klinker.android.twitter.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.services.MarkReadService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.ComposeActivity;
import com.klinker.android.twitter.ui.ComposeDMActivity;
import com.klinker.android.twitter.ui.MainActivity;

import java.net.URL;
import java.util.ArrayList;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

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

        //int[] unreadCounts = new int[] {4, 1, 2}; // for testing
        int[] unreadCounts = getUnreads(context);
        String shortText = getShortText(unreadCounts, context, currentAccount);
        String longText = getLongText(unreadCounts, context, currentAccount);
        // [0] is the full title and [1] is the screenname
        String[] title = getTitle(unreadCounts, context, currentAccount);
        boolean useExpanded = useExp(context);
        boolean addButton = addBtn(unreadCounts);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra("from_notification", true);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        Notification.Builder mBuilder;

        if (useExpanded) {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title[0])
                    .setContentText(HtmlUtils.removeColorHtml(shortText))
                    .setSmallIcon(R.drawable.ic_stat_icon)
                    .setLargeIcon(getIcon(context, unreadCounts, title[1]))
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(Html.fromHtml(longText)));

            if (addButton) { // the reply and read button should be shown
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
            } else { // otherwise, if they can use the expanded notifications, the popup button will be shown
                Intent popup = new Intent(context, RedirectToPopup.class);
                popup.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                popup.putExtra("from_notification", true);

                PendingIntent popupPending = PendingIntent.getActivity(context, 0, popup, 0);

                mBuilder.addAction(R.drawable.ic_popup, context.getResources().getString(R.string.popup), popupPending);
            }
        } else {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title[0])
                    .setContentText(HtmlUtils.removeColorHtml(shortText))
                    .setSmallIcon(R.drawable.ic_stat_icon)
                    .setLargeIcon(getIcon(context, unreadCounts, title[1]))
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

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // if there are unread tweets on the timeline, check them for favorite users
            if (settings.favoriteUserNotifications && unreadCounts[0] > 0) {
                favUsersNotification(currentAccount, context);
            }
        }
    }

    public static boolean addBtn(int[] unreadCount) {
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        return ((mentionsTweets == 1 && dmTweets == 0) || (dmTweets == 1 && mentionsTweets == 0)) && homeTweets == 0;
    }

    public static boolean useExp(Context context) {
        if (context.getResources().getBoolean(R.bool.expNotifications)) {
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

    public static String getLongTextNoHtml(int[] unreadCount, Context context, int currentAccount) {

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
                body += context.getResources().getString(R.string.timeline) + ": " + homeTweets + " " + (homeTweets == 1 ? context.getResources().getString(R.string.new_tweet) : context.getResources().getString(R.string.new_tweets)) + (mentionsTweets > 0 || dmTweets > 0 ? "\n" : "");
            }

            if (mentionsTweets > 0) {
                body += context.getResources().getString(R.string.mentions) + ": " + mentionsTweets + " " + (mentionsTweets == 1 ? context.getResources().getString(R.string.new_mention) : context.getResources().getString(R.string.new_mentions)) + (dmTweets > 0 ? "\n" : "");
            }

            if (dmTweets > 0) {
                body += context.getResources().getString(R.string.direct_messages) + ": " + dmTweets + " " + (dmTweets == 1 ? context.getResources().getString(R.string.new_message) : context.getResources().getString(R.string.new_messages));
            }
        }
        return body;
    }

    public static Bitmap getIcon(Context context, int[] unreadCount, String screenname) {

        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        boolean customPic = (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) ||
                (dmTweets == 1 && homeTweets == 0 && mentionsTweets == 0);

        if (screenname != null && customPic) {
            BitmapLruCache mCache = App.getInstance(context).getBitmapCache();
            Log.v("notifications_talon", "in screenname");
            String url;
            try {
                url = Utils.getTwitter(context, new AppSettings(context)).showUser(screenname).getBiggerProfileImageURL();
                CacheableBitmapDrawable wrapper = mCache.get(url + "_notification");

                Log.v("notifications_talon", "got wrapper");

                if (wrapper == null) {

                    Log.v("notifications_talon", "wrapper null");
                    URL mUrl = new URL(url);
                    Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                    image = ImageUtils.notificationResize(context, image);
                    mCache.put(url + "_notification", image);
                    return image;
                } else {
                    return wrapper.getBitmap();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_icon);
    }

    public static void favUsersNotification(int account, Context context) {

        ArrayList<String[]> tweets = new ArrayList<String[]>();

        HomeDataSource data = new HomeDataSource(context);
        data.open();
        Cursor cursor = data.getUnreadCursor(account);

        FavoriteUsersDataSource favs = new FavoriteUsersDataSource(context);
        favs.open();

        if(cursor.moveToFirst()) {
            do {
                String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));

                if (favs.isFavUser(account, screenname)) {
                    String name = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME));
                    String text = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));

                    tweets.add(new String[] {name, text, screenname});
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        favs.close();
        data.close();

        if (tweets.size() > 0) {
            makeFavsNotification(tweets, context);
        }
    }

    public static void makeFavsNotification(ArrayList<String[]> tweets, Context context) {
        String shortText;
        String longText;
        String title;
        int smallIcon = R.drawable.ic_stat_icon;
        Bitmap largeIcon;

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.putExtra("from_notification", true);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        if (tweets.size() == 1) {
            title = tweets.get(0)[0];
            shortText = tweets.get(0)[1];
            longText = shortText;

            largeIcon = getImage(context, tweets.get(0)[2]);
        } else {
            title = context.getResources().getString(R.string.favorite_users);
            shortText = tweets.size() + " " + context.getResources().getString(R.string.fav_user_tweets);
            longText = "";

            for(String[] s : tweets) {
                longText += "<b>" + s[0] + ":</b> " + s[1] + "<br>";
            }

            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark);
        }

        Notification.Builder mBuilder;

        if (context.getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(shortText))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(Html.fromHtml(longText)));
        } else {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(shortText))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(2, mBuilder.build());
    }

    public static Bitmap getImage(Context context, String screenname) {
        BitmapLruCache mCache = App.getInstance(context).getBitmapCache();
        String url;
        try {
            url = Utils.getTwitter(context, new AppSettings(context)).showUser(screenname).getBiggerProfileImageURL();
            CacheableBitmapDrawable wrapper = mCache.get(url + "_notification");

            if (wrapper == null) {

                URL mUrl = new URL(url);
                Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                image = ImageUtils.notificationResize(context, image);
                mCache.put(url + "_notification", image);
                return image;
            } else {
                return wrapper.getBitmap();
            }
        } catch (Exception e) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark);
        }
    }

    public static void notifySecondMentions(Context context, int secondAccount) {
        MentionsDataSource data = new MentionsDataSource(context);
        data.open();
        int numberNew = 2;//data.getUnreadCount(secondAccount);

        int smallIcon = R.drawable.ic_stat_icon;
        Bitmap largeIcon;

        Intent resultIntent = new Intent(context, SwitchAccountsRedirect.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        Notification.Builder mBuilder;

        String title = context.getResources().getString(R.string.app_name) + " - " + context.getResources().getString(R.string.sec_acc);;
        String name;
        String message;
        String messageLong;

        if (numberNew == 1) {
            name = data.getNewestName(secondAccount);
            message = context.getResources().getString(R.string.mentioned_by) + " @" + name;
            messageLong = "<b>@" + name + "</b>: " + data.getNewestMessage(secondAccount);
            largeIcon = getImage(context, name);
        } else { // more than one mention
            message = numberNew + " " + context.getResources().getString(R.string.new_mentions);
            messageLong = "<b>" + context.getResources().getString(R.string.mentions) + "</b>: " + numberNew + " " + context.getResources().getString(R.string.new_mentions);
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark);
        }

        if (context.getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(message))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(Html.fromHtml(messageLong)));
        } else {
            mBuilder = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(messageLong))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(3, mBuilder.build());

        data.close();
    }
}
