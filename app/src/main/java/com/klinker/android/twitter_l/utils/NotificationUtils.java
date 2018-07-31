package com.klinker.android.twitter_l.utils;
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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.*;
import com.klinker.android.twitter_l.receivers.MarkMentionReadReceiver;
import com.klinker.android.twitter_l.receivers.NotificationDeleteReceiverOne;
import com.klinker.android.twitter_l.receivers.NotificationDeleteReceiverTwo;
import com.klinker.android.twitter_l.services.FavoriteTweetService;
import com.klinker.android.twitter_l.services.MarkReadSecondAccService;
import com.klinker.android.twitter_l.services.MarkReadService;
import com.klinker.android.twitter_l.services.ReadInteractionsService;
import com.klinker.android.twitter_l.services.ReplyFromWearService;
import com.klinker.android.twitter_l.services.ReplySecondAccountFromWearService;
import com.klinker.android.twitter_l.services.RetweetService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.compose.NotificationCompose;
import com.klinker.android.twitter_l.activities.compose.NotificationComposeSecondAcc;
import com.klinker.android.twitter_l.activities.compose.NotificationDMCompose;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.glide.CircleBitmapTransform;
import com.klinker.android.twitter_l.utils.redirects.RedirectToDMs;
import com.klinker.android.twitter_l.utils.redirects.RedirectToDrawer;
import com.klinker.android.twitter_l.utils.redirects.RedirectToFavoriteUsers;
import com.klinker.android.twitter_l.utils.redirects.RedirectToMentions;
import com.klinker.android.twitter_l.utils.redirects.RedirectToPopup;
import com.klinker.android.twitter_l.utils.redirects.RedirectToTweetViewer;
import com.klinker.android.twitter_l.utils.redirects.SwitchAccountsRedirect;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.leolin.shortcutbadger.ShortcutBadger;
import twitter4j.User;

public class NotificationUtils {

    public static final boolean TEST_NOTIFICATION = false;
    public static final int TEST_TIMELINE_NUM = 300;
    public static final int TEST_MENTION_NUM = 0;
    public static final int TEST_DM_NUM = 0;
    public static final int TEST_SECOND_MENTIONS_NUM = 0;

    public static final String SECOND_ACC_MENTIONS_GROUP = "second_account_mentions_group";
    public static final String FIRST_ACCOUNT_GROUP = "first_account_group";
    public static final String FAVORITE_USERS_GROUP = "favorite_users_group";

    // Key for the string that's delivered in the action's intent
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    public static void refreshNotification(Context context) {
        refreshNotification(context, false);
    }

    public static void refreshNotification(Context context, boolean noTimeline) {
        AppSettings.invalidate();
        AppSettings settings = AppSettings.getInstance(context);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        int[] unreadCounts;
        if (TEST_NOTIFICATION) {
            unreadCounts = new int[] {TEST_TIMELINE_NUM, TEST_MENTION_NUM, TEST_DM_NUM}; // for testing
        } else {
            unreadCounts = getUnreads(context);
        }

        int timeline = unreadCounts[0];

        // if there are unread tweets on the timeline, check them for favorite users
        if (settings.favoriteUserNotifications && timeline > 0) {
            favUsersNotification(currentAccount, context, timeline);
        }

        if (!TEST_NOTIFICATION) {
            // if they don't want that type of notification, simply set it to zero
            if (!settings.timelineNot || noTimeline) {
                unreadCounts[0] = 0;
            }
            if (!settings.mentionsNot) {
                unreadCounts[1] = 0;
            }
            if (!settings.dmsNot) {
                unreadCounts[2] = 0;
            }
        }

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        if (unreadCounts[0] == 0 && unreadCounts[1] == 0 && unreadCounts[2] == 0) {

        } else {
            List<NotificationIdentifier> grouped = new ArrayList();
            Intent markRead = new Intent(context, MarkReadService.class);
            PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

            String shortText = getShortText(unreadCounts, context, currentAccount);
            String longText = getLongText(unreadCounts, context, currentAccount);
            // [0] is the full question and [1] is the screenname
            String[] title = getTitle(unreadCounts, context, currentAccount);
            String pictureUrl;
            boolean useExpanded = useExp(context);
            boolean addButton = addBtn(unreadCounts);

            if (title == null) {
                return;
            }

            Intent resultIntent;

            if (unreadCounts[1] != 0 && unreadCounts[0] == 0) {
                // it is a mention notification (could also have a direct message)
                resultIntent = new Intent(context, RedirectToMentions.class);
            } else if (unreadCounts[2] != 0 && unreadCounts[0] == 0 && unreadCounts[1] == 0) {
                // it is a direct message
                resultIntent = new Intent(context, RedirectToDMs.class);
            } else {
                resultIntent = new Intent(context, MainActivity.class);
            }

            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

            NotificationCompat.Builder mBuilder;

            Intent deleteIntent = new Intent(context, NotificationDeleteReceiverOne.class);

            String channelId;

            if (unreadCounts[1] != 0) {
                channelId = NotificationChannelUtil.MENTIONS_CHANNEL;
            } else if (unreadCounts[2] != 0) {
                channelId = NotificationChannelUtil.DIRECT_MESSAGES_CHANNEL;
            } else {
                channelId = NotificationChannelUtil.BACKGROUND_REFRESH_CHANNEL;
            }

            mBuilder = new NotificationCompat.Builder(context, channelId)
                    .setContentTitle(title[0])
                    .setContentText(TweetLinkUtils.removeColorHtml(shortText, settings))
                    .setSmallIcon(R.drawable.ic_stat_icon)
                    .setContentIntent(resultPendingIntent)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setTicker(TweetLinkUtils.removeColorHtml(shortText, settings))
                    .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBuilder.setCategory(Notification.CATEGORY_SOCIAL);
            }

            if (settings.headsUp) {
                mBuilder//.setFullScreenIntent(resultPendingIntent, true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
            }

            Bitmap b = getIcon(context, unreadCounts, title[1]);
            if (b != null) {
                mBuilder.setLargeIcon(b);
            }

            if (unreadCounts[1] > 1 && unreadCounts[0] == 0 && unreadCounts[2] == 0) {
                // inbox style notification for mentions
                mBuilder.setStyle(getMentionsInboxStyle(grouped, FIRST_ACCOUNT_GROUP,
                        unreadCounts[1],
                        currentAccount,
                        context,
                        TweetLinkUtils.removeColorHtml(shortText, settings)));

                for (NotificationIdentifier noti : grouped) {
                    notificationManager.notify(noti.notificationId, noti.notification);
                }
            } else if (unreadCounts[2] > 1 && unreadCounts[0] == 0 && unreadCounts[1] == 0) {
                // inbox style notification for direct messages
                mBuilder.setStyle(getDMInboxStyle(unreadCounts[1],
                        currentAccount,
                        context,
                        TweetLinkUtils.removeColorHtml(shortText, settings)));
            } else  {
                // big text style for an unread count on timeline, mentions, and direct messages
                mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ?
                        longText.replaceAll("FF8800", settings.accentColor) : longText)));

                if (unreadCounts[1] > 1) {
                    // this will group any mention notifications for us
                    getMentionsInboxStyle(grouped, FIRST_ACCOUNT_GROUP,
                            unreadCounts[1],
                            currentAccount,
                            context,
                            TweetLinkUtils.removeColorHtml(shortText, settings));

                    for (NotificationIdentifier noti : grouped) {
                        notificationManager.notify(noti.notificationId, noti.notification);
                    }
                }
            }

            // Pebble notification
            if(sharedPrefs.getBoolean("pebble_notification", false)) {
                sendAlertToPebble(context, title[0], shortText);
            }

            // Light Flow notification
            sendToLightFlow(context, title[0], shortText);

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

            int notificationId = generateRandomId();

            if ((TEST_NOTIFICATION || settings.notifications) && newC > 0) {

                if (settings.vibrate && settings.led) {
                    mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
                } else if (settings.vibrate) {
                    mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
                } else if (settings.led) {
                    mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                }

                if (settings.sound) {
                    try {
                        mBuilder.setSound(Uri.parse(settings.ringtone));
                    } catch (Exception e) {
                        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    }
                }

                if (addButton) { // the reply and read button should be shown

                    Intent reply;
                    PendingIntent replyPending;

                    MentionsDataSource data = MentionsDataSource.getInstance(context);
                    long id = data.getLastIds(currentAccount)[0];

                    if (unreadCounts[2] == 1) {
                        reply = new Intent(context, NotificationDMCompose.class);
                        reply.putExtra("dm_text", "@" + title[1] + ": " + shortText);
                        reply.putExtra("reply_to", "@" + title[1]);
                        replyPending = PendingIntent.getActivity(context, generateRandomId(), reply, 0);

                    } else {
                        if (Utils.isAndroidN()) {
                            reply = new Intent(context, ReplyFromWearService.class);
                            reply.putExtra(ReplyFromWearService.IN_REPLY_TO_ID, id);
                            reply.putExtra(ReplyFromWearService.REPLY_TO_NAME, "@" + title[1] + " " + title[2]);
                            reply.putExtra(ReplyFromWearService.NOTIFICATION_ID, notificationId);

                            replyPending = PendingIntent.getService(context, notificationId, reply, 0);
                        } else {
                            reply = new Intent(context, NotificationCompose.class);
                            reply.putExtra("from_noti", "@" + title[1] + " " + title[2]);
                            reply.putExtra("rom_noti_long", id);
                            reply.putExtra("from_noti_text", "@" + title[1] + ": " + shortText);

                            replyPending = PendingIntent.getActivity(context, notificationId, reply, 0);
                        }
                    }

                    // Create the remote input
                    RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                            .setLabel("@" + title[1] + " ")
                            .build();

                    // Create the notification action
                    NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_action_reply_light,
                            context.getResources().getString(R.string.noti_reply), replyPending)
                            .addRemoteInput(remoteInput)
                            .build();

                    mBuilder.addAction(replyAction);

                    Intent favoriteTweetIntent = FavoriteTweetService.getIntent(context, settings.currentAccount, id, notificationId);
                    Intent retweetIntent = RetweetService.getIntent(context, settings.currentAccount, id, notificationId);

                    if (unreadCounts[1] == 1 && unreadCounts[0] == 0 && unreadCounts[2] == 0) {
                        pictureUrl = data.getNewestPictureUrl(currentAccount);
                        if (pictureUrl != null && !pictureUrl.isEmpty()) {
                            mBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                                    .bigPicture(getPicture(context, pictureUrl))
                                    .setSummaryText(Html.fromHtml(longText))
                                    .setBigContentTitle(Html.fromHtml(title[0]))
                            );
                        }

                        Cursor latest = data.getCursor(currentAccount);
                        if (latest.moveToLast()) {
                            Intent tweet = TweetActivity.getIntent(context, latest, false);
                            Intent contentIntent = new Intent(context, RedirectToTweetViewer.class);
                            contentIntent.putExtras(tweet);
                            contentIntent.putExtra("", latest.getLong(latest.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)));
                            contentIntent.putExtra("notification_id", notificationId);
                            contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            mBuilder.setContentIntent(PendingIntent.getActivity(context, generateRandomId(), contentIntent, 0));
                        }

                        // retweet button
                        mBuilder.addAction(new NotificationCompat.Action.Builder(
                                R.drawable.ic_action_repeat_light,
                                context.getResources().getString(R.string.retweet),
                                PendingIntent.getService(context, generateRandomId(), retweetIntent, 0)
                        ).build());

                        // favorite button
                        mBuilder.addAction(new NotificationCompat.Action.Builder(
                                R.drawable.ic_heart_light,
                                context.getResources().getString(R.string.favorite),
                                PendingIntent.getService(context, generateRandomId(), favoriteTweetIntent, 0)
                        ).build());
                    }

                } else { // otherwise, if they can use the expanded notifications, the popup button will be shown
                    Intent popup = new Intent(context, RedirectToPopup.class);
                    popup.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    popup.putExtra("from_notification", true);

                    PendingIntent popupPending = PendingIntent.getActivity(context, 0, popup, 0);

                    NotificationCompat.Action.Builder action = new NotificationCompat.Action.Builder(
                            R.drawable.ic_popup,
                            context.getResources().getString(R.string.popup), popupPending);

                    mBuilder.addAction(action.build());
                }

                // Build the notification and issues it with notification manager.
                int lastNotificationId = sharedPrefs.getInt("last_notification_id", 1);
                notificationManager.cancel(lastNotificationId);

                if (grouped.size() > 0) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                        notificationManager.notify(1, mBuilder.setGroupSummary(true).setGroup(FIRST_ACCOUNT_GROUP).build());
                        sharedPrefs.edit().putInt("last_notification_id", 1).apply();
                    }
                } else {
                    if (unreadCounts[1] == 1 && unreadCounts[0] == 0 && unreadCounts[2] == 0) {
                        notificationManager.notify(notificationId, mBuilder.build());
                        sharedPrefs.edit().putInt("last_notification_id", notificationId).apply();
                    } else {
                        notificationManager.notify(1, mBuilder.build());
                        sharedPrefs.edit().putInt("last_notification_id", 1).apply();
                    }
                }

                // if we want to wake the screen on a new message
                if (settings.wakeScreen) {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                    wakeLock.acquire(5000);
                }
            }
        }

        if (unreadCounts[1] + unreadCounts[2] == 0) {
            ShortcutBadger.removeCount(context);
        } else {
            ShortcutBadger.applyCount(context, unreadCounts[1] + unreadCounts[2]);
        }

        try {

            ContentValues cv = new ContentValues();

            cv.put("tag", "com.klinker.android.twitter_l/com.klinker.android.twitter_l.ui.MainActivity");

            // add the direct messages and mentions
            cv.put("count", unreadCounts[1] + unreadCounts[2]);

            context.getContentResolver().insert(Uri
                            .parse("content://com.teslacoilsw.notifier/unread_count"),
                    cv);

        } catch (IllegalArgumentException ex) {

            /* Fine, TeslaUnread is not installed. */

        } catch (Exception ex) {

            /* Some other error, possibly because the format
               of the ContentValues are incorrect.

                Log but do not crash over this. */

            ex.printStackTrace();

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
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        HomeDataSource data = HomeDataSource.getInstance(context);
        int homeTweets = data.getUnreadCount(currentAccount);

        MentionsDataSource mentions = MentionsDataSource.getInstance(context);
        int mentionsTweets = mentions.getUnreadCount(currentAccount);

        int dmTweets = sharedPrefs.getInt("dm_unread_" + currentAccount, 0);

        return new int[] {homeTweets, mentionsTweets, dmTweets};
    }

    public static String[] getTitle(int[] unreadCount, Context context, int currentAccount) {
        String text = "";
        String name = null;
        String names = "";
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        String screenName = sharedPrefs.getString("twitter_screen_name_" + currentAccount, "");

        // they only have a new mention
        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) {
            MentionsDataSource mentions = MentionsDataSource.getInstance(context);
            name = mentions.getNewestName(currentAccount);
            String n = mentions.getNewestNames(currentAccount);
            for (String s : n.split("  ")) {
                if (!s.equals("") &&
                        !screenName.equals(s) &&
                        !s.equals(name)) {
                    names += "@" + s + " ";
                }
            }
            text = context.getResources().getString(R.string.mentioned_by) + " @" + name;

            // if they are muted, and you don't want them to show muted mentions
            // then just quit
            if (sharedPrefs.getString("muted_users", "").contains(name) &&
                    !sharedPrefs.getBoolean("show_muted_mentions", false)) {
                return null;
            }
        } else if (homeTweets == 0 && mentionsTweets == 0 && dmTweets == 1) { // they have 1 new direct message
            DMDataSource dm = DMDataSource.getInstance(context);
            name = dm.getNewestName(currentAccount, screenName);
            text = context.getResources().getString(R.string.message_from) + " @" + name;
        } else { // other cases we will just put talon
            text = context.getResources().getString(R.string.app_name);
        }

        return new String[] {text, name, names};
    }

    public static String getShortText(int[] unreadCount, Context context, int currentAccount) {
        String text = "";
        int homeTweets = unreadCount[0];
        int mentionsTweets = unreadCount[1];
        int dmTweets = unreadCount[2];

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        String screenName = sharedPrefs.getString("twitter_screen_name_" + currentAccount, "");


        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) { // display the new mention
            MentionsDataSource mentions = MentionsDataSource.getInstance(context);
            text = mentions.getNewestMessage(currentAccount);
        } else if (dmTweets == 1 && mentionsTweets == 0 && homeTweets == 0) { // display the new message
            DMDataSource dm = DMDataSource.getInstance(context);
            text = dm.getNewestMessage(currentAccount, screenName);
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

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        String screenName = sharedPrefs.getString("twitter_screen_name_" + currentAccount, "");


        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) { // display the new mention
            MentionsDataSource mentions = MentionsDataSource.getInstance(context);
            body = mentions.getNewestMessage(currentAccount);
        } else if (dmTweets == 1 && mentionsTweets == 0 && homeTweets == 0) { // display the new message
            DMDataSource dm = DMDataSource.getInstance(context);
            body = dm.getNewestMessage(currentAccount, screenName);
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

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        String screenName = sharedPrefs.getString("twitter_screen_name_" + currentAccount, "");

        if (mentionsTweets == 1 && homeTweets == 0 && dmTweets == 0) { // display the new mention
            MentionsDataSource mentions = MentionsDataSource.getInstance(context);
            body = mentions.getNewestMessage(currentAccount);
        } else if (dmTweets == 1 && mentionsTweets == 0 && homeTweets == 0) { // display the new message
            DMDataSource dm = DMDataSource.getInstance(context);
            body = dm.getNewestMessage(currentAccount, screenName);
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
            Log.v("notifications_talon", "in screenname");
            String url;
            try {
                return getImage(context, screenname);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;//BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_icon);
    }

    public static void favUsersNotification(int account, Context context, int newOnTimeline) {

        AppSettings settings = AppSettings.getInstance(context);
        List<NotificationIdentifier> tweets = new ArrayList();

        HomeDataSource data = HomeDataSource.getInstance(context);
        Cursor cursor;
        if (newOnTimeline != -1) {
            cursor = data.getCursor(account);
        } else { // -1 is on the talon pull
            cursor = data.getUnreadCursor(account);
        }

        FavoriteUsersDataSource favs = FavoriteUsersDataSource.getInstance(context);

        if (cursor == null) {
            return;
        }

        try {
            if(newOnTimeline != -1 && cursor.move(cursor.getCount() - newOnTimeline)) {
                do {
                    String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
                    String retweeter = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER));
                    if (favs.isFavUser(screenname) || favs.isFavUser(retweeter)) {
                        tweets.add(
                                getNotificationFromCursor(context, cursor, FAVORITE_USERS_GROUP, 1, true,
                                        tweets.size() == 0 && !Utils.isAndroidN(), NotificationChannelUtil.FAVORITE_USERS_CHANNEL) // we only want the alerts to go off for the first one and only if it isn't android N. since that has its own summary notification
                        );
                    }
                } while (cursor.moveToNext());
            } else if (cursor.moveToFirst()) { // talon pull for favorite users
                do {
                    String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
                    String retweeter = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER));
                    if (favs.isFavUser(screenname) || favs.isFavUser(retweeter)) {
                        tweets.add(
                                getNotificationFromCursor(context, cursor, FAVORITE_USERS_GROUP, 1, true,
                                        tweets.size() == 0 && !Utils.isAndroidN(), NotificationChannelUtil.FAVORITE_USERS_CHANNEL) // we only want the alerts to go off for the first one and only if it isn't android N.
                        );
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        SharedPreferences sharedPrefs = AppSettings.getInstance(context).sharedPrefs;

        if (!AppSettings.getInstance(context).notifications) {
            return;
        }

        int notifiedCount = 0;

        try {
            FavoriteUserNotificationDataSource dataSource = new FavoriteUserNotificationDataSource(context);
            dataSource.open();
            for (NotificationIdentifier notification : tweets) {
                try {
                    if (!dataSource.hasShownNotification(Long.parseLong(notification.tweetId))) {
                        if (!Utils.isAndroidN() && notifiedCount == 0 && settings.sound) {
                            try {
                                notification.notification.sound = Uri.parse(settings.ringtone);
                            } catch (Exception e) {
                                notification.notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                            }
                        }

                        notificationManager.notify(notification.notificationId, notification.notification);
                        dataSource.storeShowedNotification(Long.parseLong(notification.tweetId));

                        notifiedCount++;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            dataSource.close();
        } catch (Exception e) {

        }

        // on android N, we want to make the summary notification, for all other version, we just display all
        // the notifications
        if (Utils.isAndroidN() && notifiedCount > 0) {
            NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
            inbox.setBigContentTitle(notifiedCount + " " + context.getResources().getString(R.string.fav_user_tweets));

            if (cursor.move(cursor.getCount() - newOnTimeline)) {
                do {
                    String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
                    if (favs.isFavUser(screenname)) {
                        String tweetText = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
                        inbox.addLine(Html.fromHtml("<b>@" + screenname + ":</b> " + tweetText));
                    }
                } while ((cursor.moveToNext()));
            }

            String shortText = notifiedCount + " " + context.getResources().getString(R.string.fav_user_tweets);
            int smallIcon = R.drawable.ic_stat_icon;

            Intent resultIntent = new Intent(context, RedirectToFavoriteUsers.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, generateRandomId(), resultIntent, 0);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationChannelUtil.FAVORITE_USERS_CHANNEL)
                    .setContentTitle(context.getResources().getString(R.string.favorite_users))
                    .setContentText(shortText)
                    .setSmallIcon(smallIcon)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_SOCIAL)
                    .setGroup(FAVORITE_USERS_GROUP)
                    .setGroupSummary(true)
                    .setStyle(inbox);

            if (settings.headsUp)
                mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

            if (settings.vibrate && settings.led) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
            } else if (settings.vibrate) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else if (settings.led) {
                mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
            }

            if (settings.sound) {
                try {
                    mBuilder.setSound(Uri.parse(settings.ringtone));
                } catch (Exception e) {
                    mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                }
            }

            notificationManager.notify(2, mBuilder.build());

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(AppSettings.getSharedPreferences(context).getBoolean("pebble_notification", false)) {
                sendAlertToPebble(context, context.getResources().getString(R.string.favorite_users), shortText);
            }

            // Light Flow notification
            sendToLightFlow(context, context.getResources().getString(R.string.favorite_users), shortText);
        }

        cursor.close();
    }

    public static Bitmap getImage(Context context, String screenname) {
        String url;
        try {
            url = Utils.getTwitter(context, AppSettings.getInstance(context)).showUser(screenname).getBiggerProfileImageURL();
            return Glide.
                    with(context).
                    load(url).
                    asBitmap().
                    transform(new CircleBitmapTransform(context)).
                    into(1000,1000).
                    get();
        } catch (Exception e) {
            e.printStackTrace();
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_light);
        }
    }

    public static Bitmap getPicture(Context context, String url) {
        try {
            return Glide.
                    with(context).
                    load(url).
                    asBitmap().
                    into(1000,1000).
                    get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void notifySecondDMs(Context context, int secondAccount) {
        AppSettings.invalidate();

        DMDataSource data = DMDataSource.getInstance(context);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        String screenName = sharedPrefs.getString("twitter_screen_name_" + secondAccount, "");

        int numberNew = sharedPrefs.getInt("dm_unread_" + secondAccount, 0);

        int smallIcon = R.drawable.ic_stat_icon;
        Bitmap largeIcon;

        Intent resultIntent = new Intent(context, SwitchAccountsRedirect.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        NotificationCompat.Builder mBuilder;

        String title = context.getResources().getString(R.string.app_name) + " - " + context.getResources().getString(R.string.sec_acc);
        String name;
        String message;
        String messageLong;

        NotificationCompat.InboxStyle inbox = null;
        if (numberNew == 1) {
            name = data.getNewestName(secondAccount, screenName);

            // if they are muted, and you don't want them to show muted mentions
            // then just quit
            if (sharedPrefs.getString("muted_users", "").contains(name) &&
                    !sharedPrefs.getBoolean("show_muted_mentions", false)) {
                return;
            }

            message = context.getResources().getString(R.string.message_from) + " @" + name;
            messageLong = "<b>@" + name + "</b>: " + data.getNewestMessage(secondAccount, screenName);
            largeIcon = getImage(context, name);
        } else { // more than one dm
            message = numberNew + " " + context.getResources().getString(R.string.new_direct_messages);
            messageLong = "<b>" + context.getResources().getString(R.string.direct_messages) + "</b>: " + numberNew + " " + context.getResources().getString(R.string.new_direct_messages);
            largeIcon = null;//BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark);

            inbox = getDMInboxStyle(numberNew, secondAccount, context, message);
        }

        Intent markRead = new Intent(context, MarkReadSecondAccService.class);
        PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

        AppSettings settings = AppSettings.getInstance(context);

        Intent deleteIntent = new Intent(context, NotificationDeleteReceiverTwo.class);

        mBuilder = new NotificationCompat.Builder(context, NotificationChannelUtil.DIRECT_MESSAGES_CHANNEL)
                .setContentTitle(title)
                .setContentText(TweetLinkUtils.removeColorHtml(message, settings))
                .setSmallIcon(smallIcon)
                .setOnlyAlertOnce(true)
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0))
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL);

        if (settings.headsUp) {
            mBuilder//.setFullScreenIntent(resultPendingIntent, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if (largeIcon != null) {
            mBuilder.setLargeIcon(largeIcon);
        }

        if (inbox == null) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ? messageLong.replaceAll("FF8800", settings.accentColor) : messageLong)));
        } else {
            mBuilder.setStyle(inbox);
        }

        if (settings.vibrate && settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        } else if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.notifications) {
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(context);

            notificationManager.notify(9, mBuilder.build());

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(sharedPrefs.getBoolean("pebble_notification", false)) {
                sendAlertToPebble(context, title, messageLong);
            }

            // Light Flow notification
            sendToLightFlow(context, title, messageLong);
        }
    }

    public static void notifySecondMentions(Context context, int secondAccount) {
        AppSettings.invalidate();

        MentionsDataSource data = MentionsDataSource.getInstance(context);
        int numberNew = TEST_NOTIFICATION ? TEST_SECOND_MENTIONS_NUM : data.getUnreadCount(secondAccount);

        int smallIcon = R.drawable.ic_stat_icon;
        Bitmap largeIcon;

        Intent resultIntent = new Intent(context, SwitchAccountsRedirect.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        NotificationCompat.Builder mBuilder;

        String title = context.getResources().getString(R.string.app_name) + " - " + context.getResources().getString(R.string.sec_acc);
        String name = null;
        String message;
        String messageLong;
        String pictureUrl = null;

        String tweetText = null;
        NotificationCompat.Action replyAction = null;
        int notificationId = generateRandomId();

        long id = -1;

        if (numberNew == 1) {
            name = data.getNewestName(secondAccount);

            SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

            // if they are muted, and you don't want them to show muted mentions
            // then just quit
            if (sharedPrefs.getString("muted_users", "").contains(name) &&
                    !sharedPrefs.getBoolean("show_muted_mentions", false)) {
                return;
            }

            message = context.getResources().getString(R.string.mentioned_by) + " @" + name;
            tweetText = data.getNewestMessage(secondAccount);
            pictureUrl = data.getNewestPictureUrl(secondAccount);
            messageLong = "<b>@" + name + "</b>: " + tweetText;
            largeIcon = getImage(context, name);
            id = data.getLastIds(secondAccount)[0];

            Intent reply;
            PendingIntent replyPending;

            if (Utils.isAndroidN()) {
                reply = new Intent(context, ReplySecondAccountFromWearService.class);
                reply.putExtra(ReplyFromWearService.IN_REPLY_TO_ID, id);
                reply.putExtra(ReplyFromWearService.REPLY_TO_NAME, "@" + name);
                reply.putExtra(ReplyFromWearService.NOTIFICATION_ID, notificationId);

                replyPending = PendingIntent.getService(context, notificationId, reply, 0);
            } else {
                reply = new Intent(context, NotificationComposeSecondAcc.class);

                sharedPrefs.edit().putString("from_notification_second", "@" + name).apply();
                sharedPrefs.edit().putLong("from_notification_long_second", id).apply();
                sharedPrefs.edit().putString("from_notification_text_second", "@" + name + ": " + TweetLinkUtils.removeColorHtml(tweetText, AppSettings.getInstance(context))).apply();

                replyPending = PendingIntent.getActivity(context, notificationId, reply, 0);
            }

            // Create the remote input
            RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                    .setLabel("@" + name + " ")
                    .build();

            // Create the notification action
            replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_action_reply_light,
                    context.getResources().getString(R.string.noti_reply), replyPending)
                    .addRemoteInput(remoteInput)
                    .build();

        } else { // more than one mention
            message = numberNew + " " + context.getResources().getString(R.string.new_mentions);
            messageLong = "<b>" + context.getResources().getString(R.string.mentions) + "</b>: " + numberNew + " " + context.getResources().getString(R.string.new_mentions);
            largeIcon = null;//BitmapFactory.decodeResource(context.getResources(), R.drawable.drawer_user_dark);
        }

        Intent markRead = new Intent(context, MarkReadSecondAccService.class);
        PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

        AppSettings settings = AppSettings.getInstance(context);

        Intent deleteIntent = new Intent(context, NotificationDeleteReceiverTwo.class);

        mBuilder = new NotificationCompat.Builder(context, NotificationChannelUtil.MENTIONS_CHANNEL)
                .setContentTitle(title)
                .setContentText(TweetLinkUtils.removeColorHtml(message, settings))
                .setSmallIcon(smallIcon)
                .setOnlyAlertOnce(true)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0));

        if (settings.headsUp) {
            mBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if (largeIcon != null) {
            mBuilder.setLargeIcon(largeIcon);
        }

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        if (numberNew == 1) {
            Cursor latest = data.getCursor(secondAccount);
            if (latest.moveToLast()) {
                Intent tweet = TweetActivity.getIntent(context, latest, true);
                Intent contentIntent = new Intent(context, RedirectToTweetViewer.class);
                contentIntent.putExtras(tweet);
                contentIntent.putExtra("", latest.getLong(latest.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)));
                contentIntent.putExtra("notification_id", notificationId);
                contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                mBuilder.setContentIntent(PendingIntent.getActivity(context, generateRandomId(), contentIntent, 0));

            }

            mBuilder.addAction(replyAction);

            if (pictureUrl != null && !pictureUrl.isEmpty()) {
                mBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(getPicture(context, pictureUrl))
                        .setSummaryText(Html.fromHtml(messageLong))
                        .setBigContentTitle(Html.fromHtml(title))
                );
            } else {
                mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(Html.fromHtml(settings.addonTheme ? messageLong.replaceAll("FF8800", settings.accentColor) : messageLong)));
            }

            Intent favoriteTweetIntent = FavoriteTweetService.getIntent(context, secondAccount, id, notificationId);
            Intent retweetIntent = RetweetService.getIntent(context, secondAccount, id, notificationId);

            // retweet button
            mBuilder.addAction(new NotificationCompat.Action.Builder(
                    R.drawable.ic_action_repeat_light,
                    context.getResources().getString(R.string.retweet),
                    PendingIntent.getService(context, generateRandomId(), retweetIntent, 0)
            ).build());

            // favorite button
            mBuilder.addAction(new NotificationCompat.Action.Builder(
                    R.drawable.ic_heart_light,
                    context.getResources().getString(R.string.favorite),
                    PendingIntent.getService(context, generateRandomId(), favoriteTweetIntent, 0)
            ).build());
        } else {
            List<NotificationIdentifier> grouped = new ArrayList();

            NotificationCompat.InboxStyle inbox = getMentionsInboxStyle(grouped, SECOND_ACC_MENTIONS_GROUP,
                    numberNew,
                    secondAccount,
                    context,
                    TweetLinkUtils.removeColorHtml(message, settings));

            mBuilder.setStyle(inbox);

            for (NotificationIdentifier notification : grouped) {
                notificationManager.notify(notification.notificationId, notification.notification);
            }
        }

        if (settings.vibrate && settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        } else if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.notifications) {

            SharedPreferences sharedPrefs = AppSettings.getInstance(context).sharedPrefs;
            int lastNotificationId = sharedPrefs.getInt("last_second_account_mention_notification_id", 9);
            notificationManager.cancel(lastNotificationId);

            if (numberNew == 1) {
                notificationManager.notify(notificationId, mBuilder.build());
                sharedPrefs.edit().putInt("last_second_account_mention_notification_id", notificationId).apply();
            } else {
                notificationManager.notify(9, mBuilder.setGroup(SECOND_ACC_MENTIONS_GROUP).setGroupSummary(true).build());
                sharedPrefs.edit().putInt("last_second_account_mention_notification_id", 9).apply();
            }

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(AppSettings.getSharedPreferences(context).getBoolean("pebble_notification", false)) {
                sendAlertToPebble(context, title, messageLong);
            }

            // Light Flow notification
            sendToLightFlow(context, title, messageLong);
        }
    }

    private static NotificationCompat.InboxStyle getMentionsInboxStyle(List<NotificationIdentifier> group, String groupString, int numberNew, int accountNumber, Context context, String title) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

        Cursor cursor = MentionsDataSource.getInstance(context).getCursor(accountNumber);
        if (!cursor.moveToLast()) {
            return style;
        }

        AppSettings settings = AppSettings.getInstance(context);

        for (int i = 0; i < numberNew; i++) {
            if (cursor.getInt(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_UNREAD)) == 0) {
                if (i != 0) i--;
                if (cursor.getPosition() == cursor.getCount() - 1) break;
            } else {
                String handle = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_SCREEN_NAME));
                String text = cursor.getString(cursor.getColumnIndex(MentionsSQLiteHelper.COLUMN_TEXT));
                String longText = "<b>@" + handle + "</b>: " + text;

                style.addLine(Html.fromHtml(settings.addonTheme ? longText.replaceAll("FF8800", settings.accentColor) : longText));
                group.add(getNotificationFromCursor(context, cursor, groupString, accountNumber, false, NotificationChannelUtil.MENTIONS_CHANNEL));
            }

            cursor.moveToPrevious();
        }

        style.setSummaryText("New Mentions");
        style.setBigContentTitle(title);

        return style;
    }

    private static NotificationIdentifier getNotificationFromCursor(Context context, Cursor cursor, String group, int accountNumberForTweets, boolean favoriteUser, String channelId) {
        return getNotificationFromCursor(context, cursor, group, accountNumberForTweets, favoriteUser, false, channelId);
    }

    private static NotificationIdentifier getNotificationFromCursor(Context context, Cursor cursor, String group, int accountNumberForTweets, boolean favoriteUser, boolean useAlerts, String channelId) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setGroup(group);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SOCIAL);
        }

        String tweetId = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
        String screenname = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
        String tweetText = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
        String pictureUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));
        int notificationId = (int) Long.parseLong(tweetId);

        long time = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME));

        try {
            builder.setLargeIcon(getImage(context, screenname));
        } catch (Exception e) { }
        builder.setWhen(time);
        builder.setContentTitle(favoriteUser ? "@" + screenname : context.getResources().getString(R.string.mentioned_by) + " @" + screenname);
        builder.setContentText(tweetText);

        if (pictureUrl != null && !pictureUrl.isEmpty()) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .setBigContentTitle(favoriteUser ? "@" + screenname : context.getResources().getString(R.string.mentioned_by) + " @" + screenname)
                    .setSummaryText(Html.fromHtml(tweetText))
                    .bigPicture(getPicture(context, pictureUrl))
            );
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(tweetText));
        }

        boolean isSecondAccount = AppSettings.getInstance(context).currentAccount != accountNumberForTweets;
        Intent deleteIntent = MarkMentionReadReceiver.getIntent(context, Long.parseLong(tweetId));
        Intent contentIntent = TweetActivity.getIntent(context, cursor, isSecondAccount);
        Intent favoriteTweetIntent = FavoriteTweetService.getIntent(context, accountNumberForTweets, Long.parseLong(tweetId), notificationId);
        Intent retweetIntent = RetweetService.getIntent(context, accountNumberForTweets, Long.parseLong(tweetId), notificationId);

        contentIntent.putExtra("forced_tweet_id", Long.parseLong(tweetId));
        contentIntent.putExtra("notification_id", notificationId);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        builder.setContentIntent(PendingIntent.getActivity(context, generateRandomId(), contentIntent, 0));
        builder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0));

        // reply button
        Intent reply;
        PendingIntent replyPending;

        if (Utils.isAndroidN()) {
            reply = new Intent(context, isSecondAccount ? ReplySecondAccountFromWearService.class : ReplyFromWearService.class);
            reply.putExtra(ReplyFromWearService.IN_REPLY_TO_ID, Long.parseLong(tweetId));
            reply.putExtra(ReplyFromWearService.REPLY_TO_NAME, "@" + screenname);
            reply.putExtra(ReplyFromWearService.NOTIFICATION_ID, notificationId);

            replyPending = PendingIntent.getService(context, notificationId, reply, 0);
        } else if (isSecondAccount) {
            reply = new Intent(context, NotificationComposeSecondAcc.class);

            SharedPreferences sharedPrefs = AppSettings.getInstance(context).sharedPrefs;
            sharedPrefs.edit().putString("from_notification_second", "@" + screenname).apply();
            sharedPrefs.edit().putLong("from_notification_long_second", Long.parseLong(tweetId)).apply();
            sharedPrefs.edit().putString("from_notification_text_second", "@" + screenname + ": " + TweetLinkUtils.removeColorHtml(tweetText, AppSettings.getInstance(context))).apply();

            replyPending = PendingIntent.getActivity(context, notificationId, reply, 0);
        } else {
            reply = new Intent(context, NotificationCompose.class);

            reply.putExtra("from_noti", "@" + screenname);
            reply.putExtra("rom_noti_long", Long.parseLong(tweetId));
            reply.putExtra("from_noti_text", "@" + screenname + ": " + TweetLinkUtils.removeColorHtml(tweetText, AppSettings.getInstance(context)));

            replyPending = PendingIntent.getActivity(context, notificationId, reply, 0);
        }

        // Create the remote input
        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                .setLabel("@" + screenname + " ")
                .build();

        // Create the notification action
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_action_reply_light,
                context.getResources().getString(R.string.noti_reply), replyPending)
                .addRemoteInput(remoteInput)
                .build();


        builder.addAction(replyAction);

        // retweet button
        builder.addAction(new NotificationCompat.Action.Builder(
                R.drawable.ic_action_repeat_light,
                context.getResources().getString(R.string.retweet),
                PendingIntent.getService(context, generateRandomId(), retweetIntent, 0)
        ).build());

        // favorite button
        builder.addAction(new NotificationCompat.Action.Builder(
                R.drawable.ic_heart_light,
                context.getResources().getString(R.string.favorite),
                PendingIntent.getService(context, generateRandomId(), favoriteTweetIntent, 0)
        ).build());

        if (useAlerts) {
            AppSettings settings = AppSettings.getInstance(context);
            if (settings.headsUp)
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);

            if (settings.vibrate && settings.led) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
            } else if (settings.vibrate) {
                builder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else if (settings.led) {
                builder.setDefaults(Notification.DEFAULT_LIGHTS);
            }

            if (settings.sound) {
                try {
                    builder.setSound(Uri.parse(settings.ringtone));
                } catch (Exception e) {
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                }
            }
        }

        NotificationIdentifier notification = new NotificationIdentifier();
        notification.notificationId = notificationId;
        notification.notification = builder.build();
        notification.tweetId = tweetId;
        return notification;
    }

    private static NotificationCompat.InboxStyle getDMInboxStyle(int numberNew, int accountNumber, Context context, String title) {
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

        Cursor cursor = DMDataSource.getInstance(context).getCursor(accountNumber);
        if (!cursor.moveToLast()) {
            return style;
        }

        AppSettings settings = AppSettings.getInstance(context);

        for (int i = 0; i <numberNew; i++) {
            String handle = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_SCREEN_NAME));
            String text = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TEXT));
            String longText = "<b>@" + handle + "</b>: " + text;

            style.addLine(Html.fromHtml(settings.addonTheme ? longText.replaceAll("FF8800", settings.accentColor) : longText));

            cursor.moveToPrevious();
        }

        style.setSummaryText(numberNew + " " + context.getString(R.string.new_direct_messages));
        style.setBigContentTitle(title);

        return style;
    }

    // type is either " retweeted your status", " favorited your status", or " followed you"
    public static void newInteractions(User interactor, Context context, SharedPreferences sharedPrefs, String type) {

        AppSettings.invalidate();

        String title = "";
        String text = "";
        String smallText = "";
        int icon;

        AppSettings settings = AppSettings.getInstance(context);

        Intent resultIntent = new Intent(context, RedirectToDrawer.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        int newFollowers = sharedPrefs.getInt("new_followers", 0);
        int newRetweets = sharedPrefs.getInt("new_retweets", 0);
        int newFavorites = sharedPrefs.getInt("new_favorites", 0);
        int newQuotes = sharedPrefs.getInt("new_quotes", 0);

        // set question
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
        sharedPrefs.edit().putString("old_interaction_text", text).apply();

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
        if (newQuotes > 0) {
            types++;
        }

        if (types > 1) {
            icon = R.drawable.ic_stat_icon;
        } else {
            if (newFavorites > 0) {
                icon = R.drawable.ic_heart_dark;
            } else if (newRetweets > 0) {
                icon = R.drawable.ic_action_repeat_dark;
            } else {
                icon = R.drawable.drawer_user_dark;
            }
        }

        // set shorter text
        int total = newFavorites + newFollowers + newRetweets + newQuotes;
        if (total > 1) {
            smallText = total + " " + context.getResources().getString(R.string.new_interactions_lower);
        } else {
            smallText = text;
        }

        Intent markRead = new Intent(context, ReadInteractionsService.class);
        PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

        Intent deleteIntent = new Intent(context, NotificationDeleteReceiverOne.class);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, NotificationChannelUtil.INTERACTIONS_CHANNEL)
                .setContentTitle(title)
                .setContentText(Html.fromHtml(settings.addonTheme ? smallText.replaceAll("FF8800", settings.accentColor) : smallText))
                .setSmallIcon(icon)
                .setOnlyAlertOnce(true)
                .setContentIntent(resultPendingIntent)
                .setTicker(title)
                .setCategory(Notification.CATEGORY_SOCIAL)
                .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0))
                .setAutoCancel(true);

        if (settings.headsUp) {
            mBuilder//.setFullScreenIntent(resultPendingIntent, true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if(context.getResources().getBoolean(R.bool.expNotifications)) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(Html.fromHtml(settings.addonTheme ? text.replaceAll("FF8800", settings.accentColor) : text)));
        }

        if (settings.vibrate && settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        } else if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.notifications) {

            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(context);

            notificationManager.notify(4, mBuilder.build());

            // if we want to wake the screen on a new message
            if (settings.wakeScreen) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                wakeLock.acquire(5000);
            }

            // Pebble notification
            if(sharedPrefs.getBoolean("pebble_notification", false)) {
                sendAlertToPebble(context, title, text);
            }

            // Light Flow notification
            sendToLightFlow(context, title, text);
        }
    }


    public static void sendAlertToPebble(Context context, String title, String body) {
        final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

        final Map data = new HashMap();
        data.put("title", TweetLinkUtils.removeColorHtml(title.replaceAll("<b>", "").replaceAll("</b>", ""), AppSettings.getInstance(context)));
        data.put("body", TweetLinkUtils.removeColorHtml(body.replaceAll("<b>", "").replaceAll("</b>", ""), AppSettings.getInstance(context)));
        final JSONObject jsonData = new JSONObject(data);
        final String notificationData = new JSONArray().put(jsonData).toString();

        i.putExtra("messageType", "PEBBLE_ALERT");
        i.putExtra("sender", "talon_for_twitter");
        i.putExtra("notificationData", notificationData);

        Log.v("talon_pebble", "About to send a modal alert to Pebble: " + notificationData);
        context.sendBroadcast(i);
    }

    public static void sendToLightFlow(Context context, String title, String message) {
        Intent data = new Intent("com.klinker.android.twitter.NEW_NOTIFICATION");
        data.putExtra("title", TweetLinkUtils.removeColorHtml(title.replaceAll("<b>", "").replaceAll("</b>", ""), AppSettings.getInstance(context)));
        data.putExtra("message", TweetLinkUtils.removeColorHtml(message.replaceAll("<b>", "").replaceAll("</b>", ""), AppSettings.getInstance(context)));

        context.sendBroadcast(data);
    }

    public static void sendTestNotification(final Context context) {

        if (!TEST_NOTIFICATION) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                refreshNotification(context);
            }
        }).start();

        if (TEST_SECOND_MENTIONS_NUM != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    notifySecondMentions(context, 2);
                }
            }).start();
        }

        AppSettings settings = AppSettings.getInstance(context);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


        Intent markRead = new Intent(context, MarkReadService.class);
        PendingIntent readPending = PendingIntent.getService(context, 0, markRead, 0);

        String shortText = "Test Talon";
        String longText = "Here is a test for Talon's notifications";

        Intent resultIntent = new Intent(context, RedirectToMentions.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 0 );

        NotificationCompat.Builder mBuilder;

        Intent deleteIntent = new Intent(context, NotificationDeleteReceiverOne.class);

        mBuilder = new NotificationCompat.Builder(context, NotificationChannelUtil.INTERACTIONS_CHANNEL)
                .setContentTitle(shortText)
                .setContentText(longText)
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setTicker(shortText)
                .setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, 0))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Pebble notification
        if(sharedPrefs.getBoolean("pebble_notification", false)) {
            sendAlertToPebble(context, shortText, shortText);
        }

        // Light Flow notification
        sendToLightFlow(context, shortText, shortText);

        if (settings.vibrate && settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        } else if (settings.vibrate) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (settings.led) {
            mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            mBuilder.setColor(settings.themeColors.primaryColor);
        }

        if (settings.sound) {
            try {
                mBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);

        int notificationId = generateRandomId();
        Intent reply;
        PendingIntent replyPending;

        if (Utils.isAndroidN()) {
            reply = new Intent(context, ReplyFromWearService.class);
            reply.putExtra(ReplyFromWearService.IN_REPLY_TO_ID, 1);
            reply.putExtra(ReplyFromWearService.NOTIFICATION_ID, notificationId);
            reply.putExtra(ReplyFromWearService.REPLY_TO_NAME, "@test_for_talon");

            replyPending = PendingIntent.getService(context, notificationId, reply, 0);
        } else {
            reply = new Intent(context, NotificationComposeSecondAcc.class);

            sharedPrefs.edit().putString("from_notification_second", "@test_for_talon").apply();
            sharedPrefs.edit().putLong("from_notification_long_second", 1).apply();
            sharedPrefs.edit().putString("from_notification_text_second", "@test_for_talon" + ": test").apply();

            replyPending = PendingIntent.getActivity(context, notificationId, reply, 0);
        }

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                .setLabel("@" + "test_for_talon" + " ")
                .build();

        // Create the notification action
        NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(R.drawable.ic_action_reply_dark,
                context.getResources().getString(R.string.noti_reply), replyPending)
                .addRemoteInput(remoteInput)
                .build();

        NotificationCompat.Action.Builder action = new NotificationCompat.Action.Builder(
                R.drawable.ic_action_read_dark,
                context.getResources().getString(R.string.mark_read), readPending);

        mBuilder.addAction(replyAction);
        mBuilder.addAction(action.build());


        // Build the notification and issues it with notification manager.
        //notificationManager.notify(notificationId, mBuilder.build());

        // if we want to wake the screen on a new message
        if (settings.wakeScreen) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
            wakeLock.acquire(5000);
        }
    }

    public static void cancelGroupedNotificationWithNoContent(Context context) {
        if (Build.VERSION.SDK_INT >= 23 || Utils.isAndroidN()) {
            Map<String, Integer> map = new HashMap();

            NotificationManager manager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);

            StatusBarNotification[] notifications = manager.getActiveNotifications();

            for (StatusBarNotification notification : notifications) {
                String keyString = notification.getGroupKey();
                if (keyString.contains("|g:")) { // this is a grouped notification
                    keyString = keyString.substring(keyString.indexOf("|g:") + 3, keyString.length());

                    if (map.containsKey(keyString)) {
                        map.put(keyString, map.get(keyString) + 1);
                    } else {
                        map.put(keyString, 1);
                    }
                }
            }

            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                String key = (String) pair.getKey();
                int value = (Integer) pair.getValue();

                if (value == 1) {
                    for (StatusBarNotification notification : notifications) {
                        String keyString = notification.getGroupKey();
                        if (keyString.contains("|g:")) { // this is a grouped notification
                            keyString = keyString.substring(keyString.indexOf("|g:") + 3, keyString.length());

                            if (key.equals(keyString)) {
                                manager.cancel(notification.getId());
                                break;
                            }
                        }
                    }
                }

                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }

    public static int generateRandomId() {
        Random randomGenerator = new Random();
        return randomGenerator.nextInt(100000);
    }

    private static class NotificationIdentifier {
        public int notificationId;
        public String tweetId;
        public Notification notification;
    }
}
