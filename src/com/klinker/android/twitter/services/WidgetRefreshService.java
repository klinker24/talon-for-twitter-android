package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.MainActivityPopup;
import com.klinker.android.twitter.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class WidgetRefreshService  extends IntentService {

    SharedPreferences sharedPrefs;

    public WidgetRefreshService() {
        super("WidgetRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        if (MainActivity.canSwitch) {
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            Context context = getApplicationContext();
            int numberNew = 0;

            AppSettings settings = new AppSettings(context);

            // if they have mobile data on and don't want to sync over mobile data
            if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
                return;
            }

            try {
                Twitter twitter = Utils.getTwitter(context, settings);

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                User user = twitter.verifyCredentials();
                long lastId = sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0);
                long secondToLastId = sharedPrefs.getLong("second_last_tweet_id_" + currentAccount, 0);
                List<Status> statuses = new ArrayList<Status>();

                boolean foundStatus = false;
                int lastJ = 0;

                for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                    if (foundStatus) {
                        break;
                    } else {
                        statuses.addAll(getList(i + 1, twitter));
                    }

                    try {
                        for (int j = lastJ; j < statuses.size(); j++) {
                            long id = statuses.get(j).getId();
                            if (id == lastId || id == secondToLastId) {
                                statuses = statuses.subList(0, j);
                                foundStatus = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        foundStatus = true;
                    }

                    lastJ = statuses.size();
                }

                if (statuses.size() != 0) {
                    try {
                        sharedPrefs.edit().putLong("second_last_tweet_id_" + currentAccount, statuses.get(1).getId()).commit();
                    } catch (Exception e) {
                        sharedPrefs.edit().putLong("second_last_tweet_id_" + currentAccount, sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0)).commit();
                    }
                    sharedPrefs.edit().putLong("last_tweet_id_" + currentAccount, statuses.get(0).getId()).commit();

                    numberNew = statuses.size();
                } else {
                    numberNew = 0;
                }

                HomeDataSource dataSource = new HomeDataSource(context);
                dataSource.open();

                for (twitter4j.Status status : statuses) {
                    try {
                        HomeContentProvider.insertTweet(status, currentAccount, context);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

                numberNew = dataSource.getUnreadCount(currentAccount);

                int mId = 1;

                if (numberNew > 0) {

                    RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
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

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(this)
                                    .setSmallIcon(R.drawable.timeline_dark)
                                            //.setContent(remoteView);
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText(numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_tweet) : numberNew + " " + getResources().getString(R.string.new_tweets));

                    Intent resultIntent = new Intent(this, MainActivity.class);
                    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    resultIntent.putExtra("from_notification", true);

                    PendingIntent resultPendingIntent =
                            PendingIntent.getActivity(
                                    this,
                                    0,
                                    resultIntent,
                                    0
                            );

                    int count = 0;

                    if (settings.vibrate)
                        count++;
                    if (settings.sound)
                        count++;

                    if (settings.notifications) {
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

                        sharedPrefs.edit().putBoolean("refresh_me", true).commit();

                        /*mBuilder.setContentIntent(resultPendingIntent);
                        NotificationManager mNotificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(mId, mBuilder.build());

                        // if we want to wake the screen on a new message
                        if (settings.wakeScreen) {
                            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                            final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                            wakeLock.acquire(5000);
                        }*/
                    }


                }

            } catch (TwitterException e) {
                Log.d("Twitter Update Error", e.getMessage());
            }

            context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
        }
    }

    public List<twitter4j.Status> getList(int page, Twitter twitter) {
        try {
            return twitter.getHomeTimeline(new Paging(page, 200));
        } catch (Exception e) {
            Log.v("timeline_refreshing", "caught: " + e.getMessage());
            return new ArrayList<twitter4j.Status>();
        }
    }
}