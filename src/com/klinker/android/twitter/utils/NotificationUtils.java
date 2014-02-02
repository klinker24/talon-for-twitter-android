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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
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
import com.klinker.android.twitter.ui.compose.ComposeDMActivity;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.compose.NotificationCompose;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import twitter4j.User;
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

        // if they don't want that type of notification, simply set it to zero
        if (!settings.timelineNot || (settings.pushNotifications && settings.liveStreaming)) {
            unreadCounts[0] = 0;
        }
        if (!settings.mentionsNot) {
            unreadCounts[1] = 0;
        }
        if (!settings.dmsNot) {
            unreadCounts[2] = 0;
        }

        if (unreadCounts[0] == 0 && unreadCounts[1] == 0 && unreadCounts[2] == 0) {

        } else {
            Intent markRead = new Intent(context, MarkReadService.class);
            PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

            String shortText = getShortText(unreadCounts, context, currentAccount);
            String longText = getLongText(unreadCounts, context, currentAccount);
            // [0] is the full title and [1] is the screenname
            String[] title = getTitle(unreadCounts, context, currentAccount);
            boolean useExpanded = useExp(context);
            boolean addButton = addBtn(unreadCounts);

            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.putExtra("from_notification", true);

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

            NotificationCompat.Builder mBuilder;

            if (useExpanded) {
                mBuilder = new NotificationCompat.Builder(context)
                        .setContentTitle(title[0])
                        .setContentText(HtmlUtils.removeColorHtml(shortText, settings))
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setLargeIcon(getIcon(context, unreadCounts, title[1]))
                        .setContentIntent(resultPendingIntent)
                        .setAutoCancel(true)
                        .setTicker(HtmlUtils.removeColorHtml(shortText, settings))
                        .setDeleteIntent(readPending)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ? longText.replaceAll("FF8800", settings.accentColor) : longText)));

                if (addButton) { // the reply and read button should be shown
                    Intent reply;
                    if (unreadCounts[1] == 1) {
                        reply = new Intent(context, NotificationCompose.class);
                    } else {
                        reply = new Intent(context, ComposeDMActivity.class);
                    }

                    Log.v("username_for_noti", title[1]);
                    sharedPrefs.edit().putString("from_notification", "@" + title[1]).commit();
                    MentionsDataSource data = new MentionsDataSource(context);
                    data.open();
                    long id = data.getLastIds(currentAccount)[0];
                    PendingIntent replyPending = PendingIntent.getActivity(context, 0, reply, 0);
                    sharedPrefs.edit().putLong("from_notification_long", id).commit();

                    mBuilder.addAction(R.drawable.ic_action_reply_dark, context.getResources().getString(R.string.noti_reply), replyPending);

                    mBuilder.addAction(R.drawable.ic_action_read_dark, context.getResources().getString(R.string.mark_read), readPending);
                } else { // otherwise, if they can use the expanded notifications, the popup button will be shown
                    Intent popup = new Intent(context, RedirectToPopup.class);
                    popup.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    popup.putExtra("from_notification", true);

                    PendingIntent popupPending = PendingIntent.getActivity(context, 0, popup, 0);

                    mBuilder.addAction(R.drawable.ic_popup, context.getResources().getString(R.string.popup), popupPending);
                }
            } else {
                mBuilder = new NotificationCompat.Builder(context)
                        .setContentTitle(title[0])
                        .setContentText(HtmlUtils.removeColorHtml(shortText, settings))
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setLargeIcon(getIcon(context, unreadCounts, title[1]))
                        .setContentIntent(resultPendingIntent)
                        .setTicker(HtmlUtils.removeColorHtml(shortText, settings))
                        .setDeleteIntent(readPending)
                        .setAutoCancel(true);
            }

            // Pebble notification
            if(sharedPrefs.getBoolean("pebble_notification", false)) {
                Intent pebble = new Intent("com.getpebble.action.SEND_NOTIFICATION");
                Map pebbleData = new HashMap();
                pebbleData.put("title", title[0]);
                pebbleData.put("body", Html.fromHtml(settings.addonTheme ? longText.replaceAll("FF8800", settings.accentColor) : longText));
                JSONObject jsonData = new JSONObject(pebbleData);
                String notificationData = new JSONArray().put(jsonData).toString();
                pebble.putExtra("messageType", "PEBBLE_ALERT");
                pebble.putExtra("sender", context.getResources().getString(R.string.app_name));
                pebble.putExtra("notificationData", notificationData);
                context.sendBroadcast(pebble);
            }

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

                if (settings.vibrate) {
                    mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
                }

                if (settings.sound) {
                    try {
                        mBuilder.setSound(Uri.parse(settings.ringtone));
                    } catch (Exception e) {
                        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    }
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

        NotificationCompat.Builder mBuilder;

        AppSettings settings = new AppSettings(context);

        if (context.getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(shortText, settings))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ? longText.replaceAll("FF8800", settings.accentColor) : longText)));
        } else {
            mBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(shortText, settings))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true);
        }

        if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.led)
            mBuilder.setLights(0xFFFFFF, 1000, 1000);

        if (settings.notifications) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(2, mBuilder.build());

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pebble_notification", false)) {
                Intent pebble = new Intent("com.getpebble.action.SEND_NOTIFICATION");
                Map pebbleData = new HashMap();
                pebbleData.put("title", title);
                pebbleData.put("body", Html.fromHtml(settings.addonTheme ? shortText.replaceAll("FF8800", settings.accentColor) : shortText));
                JSONObject jsonData = new JSONObject(pebbleData);
                String notificationData = new JSONArray().put(jsonData).toString();
                pebble.putExtra("messageType", "PEBBLE_ALERT");
                pebble.putExtra("sender", context.getResources().getString(R.string.app_name));
                pebble.putExtra("notificationData", notificationData);
                context.sendBroadcast(pebble);
            }
        }
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

        NotificationCompat.Builder mBuilder;

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

        AppSettings settings = new AppSettings(context);

        if (context.getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(message, settings))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ? messageLong.replaceAll("FF8800", settings.accentColor) : messageLong)));
        } else {
            mBuilder = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(HtmlUtils.removeColorHtml(messageLong, settings))
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(largeIcon)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true);
        }

        if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.led)
            mBuilder.setLights(0xFFFFFF, 1000, 1000);

        if (settings.notifications) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(3, mBuilder.build());

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("pebble_notification", false)) {
                Intent pebble = new Intent("com.getpebble.action.SEND_NOTIFICATION");
                Map pebbleData = new HashMap();
                pebbleData.put("title", title);
                pebbleData.put("body", Html.fromHtml(settings.addonTheme ? messageLong.replaceAll("FF8800", settings.accentColor) : messageLong));
                JSONObject jsonData = new JSONObject(pebbleData);
                String notificationData = new JSONArray().put(jsonData).toString();
                pebble.putExtra("messageType", "PEBBLE_ALERT");
                pebble.putExtra("sender", context.getResources().getString(R.string.app_name));
                pebble.putExtra("notificationData", notificationData);
                context.sendBroadcast(pebble);
            }

        }



        data.close();
    }

    public static void newFollower(User newFollower, Context context) {

        Intent resultIntent = new Intent(context, MainActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("New Interaction")
                .setContentText("@" + newFollower.getScreenName() + " now follows you")
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark))
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(4, mBuilder.build());
    }

    public static void newFavorite(User favoriter, Context context) {

        Intent resultIntent = new Intent(context, MainActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("New Interaction")
                .setContentText("@" + favoriter.getScreenName() + " favorited your status")
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_important_dark))
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(4, mBuilder.build());
    }

    public static void newRetweet(User favoriter, Context context) {

        Intent resultIntent = new Intent(context, MainActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("New Interaction")
                .setContentText("@" + favoriter.getScreenName() + " retweeted your status")
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_repeat_dark))
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(4, mBuilder.build());
    }

    // type is either " retweeted your status", " favorited your status", or " followed you"
    public static void newInteractions(User interactor, Context context, SharedPreferences sharedPrefs, String type) {
        String title = "";
        String text = "";
        String smallText = "";
        Bitmap icon = null;

        AppSettings settings = new AppSettings(context);

        Intent resultIntent = new Intent(context, MainActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        int newFollowers = sharedPrefs.getInt("new_followers", 0);
        int newRetweets = sharedPrefs.getInt("new_retweets", 0);
        int newFavorites = sharedPrefs.getInt("new_favorites", 0);

        // set title
        if (newFavorites + newRetweets + newFollowers > 1) {
            title = context.getResources().getString(R.string.new_interactions);
        } else {
            title = context.getResources().getString(R.string.new_interaction_upper);
        }

        // set text
        String currText = sharedPrefs.getString("old_interaction_text", "");
        if (!currText.equals("")) {
            currText += "<br>";
        }
        if(settings.displayScreenName) {
            text = currText + "<b>" + interactor.getScreenName() + "</b> " + type;
        } else {
            text = currText + "<b>" + interactor.getName() + "</b> " + type;
        }
        sharedPrefs.edit().putString("old_interaction_text", text).commit();

        // set icon
        int types = 0;
        if (newFavorites > 0) {
            types++;
        }
        if(newFollowers > 0) {
            types++;
        }
        if (newRetweets > 0) {
            types++;
        }

        if (types > 1) {
            icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_icon);
        } else {
            if (newFavorites > 0) {
                icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_important_dark);
            } else if (newRetweets > 0) {
                icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_repeat_dark);
            } else {
                icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark);
            }
        }

        // set shorter text
        int total = newFavorites + newFollowers + newRetweets;
        if (total > 1) {
            smallText = total + " " + context.getResources().getString(R.string.new_interactions_lower);
        } else {
            smallText = text;
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(Html.fromHtml(settings.addonTheme ? smallText.replaceAll("FF8800", settings.accentColor) : smallText))
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setLargeIcon(icon)
                .setContentIntent(resultPendingIntent)
                .setTicker(title)
                .setAutoCancel(true);

        if(context.getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ? text.replaceAll("FF8800", settings.accentColor) : text)));
        }

        if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.led)
            mBuilder.setLights(0xFFFFFF, 1000, 1000);

        if (settings.notifications) {
            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(4, mBuilder.build());

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(sharedPrefs.getBoolean("pebble_notification", false)) {
                Intent pebble = new Intent("com.getpebble.action.SEND_NOTIFICATION");
                Map pebbleData = new HashMap();
                pebbleData.put("title", title);
                pebbleData.put("body", Html.fromHtml(settings.addonTheme ? text.replaceAll("FF8800", settings.accentColor) : text));
                JSONObject jsonData = new JSONObject(pebbleData);
                String notificationData = new JSONArray().put(jsonData).toString();
                pebble.putExtra("messageType", "PEBBLE_ALERT");
                pebble.putExtra("sender", context.getResources().getString(R.string.app_name));
                pebble.putExtra("notificationData", notificationData);
                context.sendBroadcast(pebble);
            }
        }
    }
}
