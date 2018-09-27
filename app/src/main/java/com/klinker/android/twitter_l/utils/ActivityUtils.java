package com.klinker.android.twitter_l.utils;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import com.klinker.android.twitter_l.data.sq_lite.ActivityDataSource;
import com.klinker.android.twitter_l.services.background_refresh.MentionsRefreshService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.redirects.RedirectToActivity;
import com.klinker.android.twitter_l.utils.redirects.SwitchAccountsToActivity;
import twitter4j.*;

import java.util.*;

public class ActivityUtils {

    private static String TAG = "ActivityUtils";

    private static String GROUP_ACTIVITY = "activity_notification_group";

    public static final int NOTIFICATON_ID = 434;
    public static final int SECOND_NOTIFICATION_ID = 435;

    private Context context;
    private AppSettings settings;
    private SharedPreferences sharedPrefs;
    private boolean useSecondAccount = false;
    private int currentAccount;
    private long lastRefresh;
    private long lastQuoteRefresh;
    private long originalTime; // if the tweets came before this time, then we don't want to show them in activity because it would just get blown up.

    private boolean separateMentionRefresh = false;
    private List<String> notificationItems = new ArrayList<>();
    private String notificationTitle = "";

    public ActivityUtils(Context context) {
        init(context);
    }

    public ActivityUtils(Context context, boolean useSecondAccount) {
        this.useSecondAccount = useSecondAccount;
        init(context);
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }

        this.context = context;
        this.sharedPrefs = AppSettings.getSharedPreferences(context);

        this.settings = AppSettings.getInstance(context);
        this.currentAccount = sharedPrefs.getInt("current_account", 1);

        if (useSecondAccount) {
            if (currentAccount == 1) {
                currentAccount = 2;
            } else {
                currentAccount = 1;
            }
        }

        this.lastRefresh = sharedPrefs.getLong("last_activity_refresh_" + currentAccount, 0l);
        this.lastQuoteRefresh = sharedPrefs.getLong("last_activity_quote_refresh_" + currentAccount, 0l);

        this.originalTime = sharedPrefs.getLong("original_activity_refresh_" + currentAccount, 0l);

        this.notificationTitle = context.getString(R.string.new_activity) + " - @" + (useSecondAccount ? settings.secondScreenName : settings.myScreenName);
    }

    /**
     * Refresh the new followers, mentions, number of favorites, and retweeters
     * @return boolean if there was something new
     */
    public boolean refreshActivity() {
        boolean newActivity = false;
        Twitter twitter;

        if (!useSecondAccount) {
            twitter = Utils.getTwitter(context, settings);
        } else {
            twitter = Utils.getSecondTwitter(context);
        }

        if (getMentions(twitter)) {
            newActivity = true;
        }

        if (getQuotes(twitter)) {
            newActivity = true;
        }

        if (getFollowers(twitter)) {
            newActivity = true;
        }

        List<Status> myTweets = getMyTweets(twitter);
        if (myTweets != null) {
            if (getRetweets(twitter, myTweets)) {
                newActivity = true;
            }

            if (getFavorites(myTweets)) {
                newActivity = true;
            }
        }

        sharedPrefs.edit().putBoolean("refresh_me_activity", true).apply();

        return newActivity;
    }

    public void postNotification() {
        postNotification(NOTIFICATON_ID);
    }

    public void postNotification(int id) {

        if (separateMentionRefresh) {
            MentionsRefreshService.startNow(context);
        }

        if (notificationItems.size() == 0) {
            return;
        }

        PendingIntent contentIntent;
        if (useSecondAccount) {
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, SwitchAccountsToActivity.class), 0);
        } else {
            contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, RedirectToActivity.class), 0);
        }

        NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(context,
                NotificationChannelUtil.INTERACTIONS_CHANNEL);
        summaryBuilder.setContentTitle(notificationTitle);
        summaryBuilder.setSmallIcon(R.drawable.ic_stat_icon);
        summaryBuilder.setContentIntent(contentIntent);

        if (notificationItems.size() > 1) {
            // inbox style
            NotificationCompat.InboxStyle inbox = new NotificationCompat.InboxStyle();
            inbox.setBigContentTitle(notificationTitle);

            for (String s : notificationItems) {
                inbox.addLine(Html.fromHtml(s));
                activityGroupNotification(contentIntent, Html.fromHtml(s));
            }

            summaryBuilder.setStyle(inbox);
            summaryBuilder.setContentText(notificationItems.size() + " " + context.getString(R.string.items));
            summaryBuilder.setGroup(GROUP_ACTIVITY);
            summaryBuilder.setGroupSummary(true);
        } else {
            // big text style
            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.bigText(Html.fromHtml(notificationItems.get(0)));
            bigText.setBigContentTitle(notificationTitle);

            summaryBuilder.setStyle(bigText);
            summaryBuilder.setContentText(Html.fromHtml(notificationItems.get(0)));
        }

        if (settings.headsUp) {
            summaryBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if (settings.vibrate) {
            summaryBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }

        if (settings.sound) {
            try {
                summaryBuilder.setSound(Uri.parse(settings.ringtone));
            } catch (Exception e) {
                summaryBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            }
        }

        if (settings.led) {
            summaryBuilder.setLights(0xFFFFFF, 1000, 1000);
        }

        if (settings.wakeScreen) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
            wakeLock.acquire(5000);
        }

        summaryBuilder.setAutoCancel(true);

        // Pebble notification
        if(sharedPrefs.getBoolean("pebble_notification", false)) {
            NotificationUtils.sendAlertToPebble(context, notificationTitle, notificationItems.get(0));
        }

        // Light Flow notification
        NotificationUtils.sendToLightFlow(context, notificationTitle, notificationItems.get(0));

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(id, summaryBuilder.build());
    }

    private void activityGroupNotification(PendingIntent contentIntent, Spanned text) {
        NotificationCompat.Builder individualBuilder = new NotificationCompat.Builder(context,
                    NotificationChannelUtil.INTERACTIONS_CHANNEL)
                .setContentTitle(context.getString(R.string.new_activity))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_ACTIVITY)
                .setGroupSummary(false);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(NotificationUtils.generateRandomId(), individualBuilder.build());
    }

    public void commitLastRefresh(long id) {
        sharedPrefs.edit().putLong("last_activity_refresh_" + currentAccount, id).apply();
    }

    public void commitLastQuoteRefresh(long id) {
        sharedPrefs.edit().putLong("last_activity_quote_refresh_" + currentAccount, id).apply();
    }

    public void insertMentions(List<Status> mentions) {
        try {
            List<String> notis = ActivityDataSource.getInstance(context).insertMentions(mentions, currentAccount);

            if (notis.size() > 0) {
                separateMentionRefresh = true;
            }
//            if (settings.mentionsRefresh != 0) {
//                notificationItems.addAll(notis);
//            } else {
//                separateMentionRefresh = true;
//            }
        } catch (Throwable t) {

        }
    }

    public void insertQuotes(List<Status> quotes) {
        try {
            List<String> notis = ActivityDataSource.getInstance(context).insertQuotes(quotes, currentAccount);
            if (settings.mentionsNot) notificationItems.addAll(notis);
        } catch (Throwable t) {

        }
    }

    public void insertFollowers(List<User> users) {
        try {
            String noti = ActivityDataSource.getInstance(context).insertNewFollowers(users, currentAccount);
            if (settings.followersNot) notificationItems.add(noti);
        } catch (Throwable t) {

        }
    }

    public boolean tryInsertRetweets(Status status, Twitter twitter) {
        try {
            String noti = ActivityDataSource.getInstance(context).insertRetweeters(status, currentAccount, twitter);

            if (noti != null) {
                if (settings.retweetNot) notificationItems.add(noti);
                return true;
            } else {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean tryInsertFavorites(Status status) {
        try {
            String noti = ActivityDataSource.getInstance(context).insertFavoriters(status, currentAccount);

            if (noti != null) {
                if (settings.favoritesNot) notificationItems.add(noti);
                return true;
            } else {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public List<Status> getMyTweets(Twitter twitter) {
        try {
            Paging paging = new Paging(1, 20);
            return twitter.getUserTimeline(paging);
        } catch (TwitterException e) {
            return null;
        }
    }

    public boolean getMentions(Twitter twitter) {
        boolean newActivity = false;

        try {
            if (lastRefresh != 0L) {
                Paging paging = new Paging(1, 50, lastRefresh);
                List<Status> mentions = twitter.getMentionsTimeline(paging);

                if (mentions.size() > 0) {
                    insertMentions(mentions);
                    commitLastRefresh(mentions.get(0).getId());
                    newActivity = true;
                }
            } else {
                Paging paging = new Paging(1, 1);
                List<Status> lastMention = twitter.getMentionsTimeline(paging);

                if (lastMention.size() > 0) {
                    commitLastRefresh(lastMention.get(0).getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newActivity;
    }

    public boolean getQuotes(Twitter twitter) {
        boolean newActivity = false;
        String screenname = useSecondAccount ?
                settings.secondScreenName : settings.myScreenName;

        try {
            Query query = new Query(screenname + "/status/");

            if (lastQuoteRefresh == 0L) { // just get last 5 if it is the first time.
                query.setCount(5);
            } else {
                query.setSinceId(lastQuoteRefresh);
                query.setCount(100);
            }

            List<Status> quotes = twitter.search().search(query).getTweets();
            quotes = QuoteUtil.stripNoQuotesForActivity(quotes, screenname);

            if (quotes.size() > 0) {
                insertQuotes(quotes);
                commitLastQuoteRefresh(quotes.get(0).getId());
                newActivity = true;
            }
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        return newActivity;
    }

    public boolean getFollowers(Twitter twitter) {
        boolean newActivity = false;

        try {
            List<User> followers = twitter.getFollowersList(useSecondAccount ? AppSettings.getInstance(context).secondScreenName : AppSettings.getInstance(context).myScreenName, -1, 200);
            User me = twitter.verifyCredentials();

            int oldFollowerCount = sharedPrefs.getInt("activity_follower_count_" + currentAccount, 0);
            Set<String> latestFollowers = sharedPrefs.getStringSet("activity_latest_followers_" + currentAccount, new HashSet<String>());

            Log.v(TAG, "followers set size: " + latestFollowers.size());
            Log.v(TAG, "old follower count: " + oldFollowerCount);
            Log.v(TAG, "current follower count: " + me.getFollowersCount());

            List<User> newFollowers = new ArrayList<User>();
            if (latestFollowers.size() != 0) {
                for (int i = 0; i < followers.size(); i++) {
                    if (!latestFollowers.contains(followers.get(i).getScreenName())) {
                        Log.v(TAG, "inserting @" + followers.get(i).getScreenName() + " as new follower");
                        newFollowers.add(followers.get(i));
                        newActivity = true;
                    } else {
                        break;
                    }
                }
            }

            insertFollowers(newFollowers);

            latestFollowers.clear();
            for (int i = 0; i < 50; i++) {
                if (i < followers.size()) {
                    latestFollowers.add(followers.get(i).getScreenName());
                } else {
                    break;
                }
            }

            SharedPreferences.Editor e = sharedPrefs.edit();
            e.putStringSet("activity_latest_followers_" + currentAccount, latestFollowers);
            e.putInt("activity_follower_count_" + currentAccount, me.getFollowersCount());
            e.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newActivity;
    }

    public boolean getRetweets(Twitter twitter, List<Status> statuses) {
        boolean newActivity = false;

        for (Status s : statuses) {
            if (s.getCreatedAt().getTime() > originalTime && tryInsertRetweets(s, twitter)) {
                newActivity = true;
            }
        }

        return newActivity;
    }

    public boolean getFavorites(List<Status> statuses) {
        boolean newActivity = false;

        for (Status s : statuses) {
            if (s.getCreatedAt().getTime() > originalTime && tryInsertFavorites(s)) {
                newActivity = true;
            }
        }

        return newActivity;
    }
}
