package com.klinker.android.talon.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.MainActivityPopup;
import com.klinker.android.talon.utils.Utils;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class MentionsRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public MentionsRefreshService() {
        super("MentionsRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Context context = getApplicationContext();
        AppSettings settings = new AppSettings(context);

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile) {
            return;
        }

        boolean update = false;
        int numberNew = 0;

        try {
            Twitter twitter = Utils.getTwitter(context);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            User user = twitter.verifyCredentials();
            long lastId = sharedPrefs.getLong("last_mention_id_" + currentAccount, 0);
            Paging paging;
            paging = new Paging(1, 50);

            List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

            boolean broken = false;

            // first try to get the top 50 tweets
            for (int i = 0; i < statuses.size(); i++) {
                if (statuses.get(i).getId() == lastId) {
                    statuses = statuses.subList(0, i);
                    broken = true;
                    break;
                }
            }

            // if that doesn't work, then go for the top 150
            if (!broken) {
                Paging paging2 = new Paging(1, 150);
                List<twitter4j.Status> statuses2 = twitter.getMentionsTimeline(paging2);

                for (int i = 0; i < statuses2.size(); i++) {
                    if (statuses2.get(i).getId() == lastId) {
                        statuses2 = statuses2.subList(0, i);
                        break;
                    }
                }

                statuses = statuses2;
            }

            if (statuses.size() != 0) {
                sharedPrefs.edit().putLong("last_mention_id_" + currentAccount, statuses.get(0).getId()).commit();
                update = true;
                numberNew = statuses.size();
            } else {
                update = false;
                numberNew = 0;
            }

            MentionsDataSource dataSource = new MentionsDataSource(context);
            dataSource.open();

            for (twitter4j.Status status : statuses) {
                try {
                    dataSource.createTweet(status, currentAccount);
                } catch (Exception e) {
                    break;
                }
            }

            dataSource.close();

            int mId = 2;

            if (numberNew > 0) {

                int currentUnread = sharedPrefs.getInt("mentions_unread_" + currentAccount, 0);
                sharedPrefs.edit().putInt("mentions_unread_" + currentAccount, numberNew + currentUnread).commit();
                numberNew += currentUnread;

                RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
                Intent popup = new Intent(context, MainActivityPopup.class);
                popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent popupPending =
                        PendingIntent.getActivity(
                                this,
                                0,
                                popup,
                                0
                        );
                remoteView.setOnClickPendingIntent(R.id.popup_button, popupPending);
                remoteView.setTextViewText(R.id.content, numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_mention) : numberNew + " " + getResources().getString(R.string.new_mentions));

                remoteView.setImageViewResource(R.id.icon, R.drawable.mentions_dark);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.mentions_dark)
                                //.setContent(remoteView);
                                .setContentTitle(getResources().getString(R.string.app_name))
                                .setContentText(numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_mention) : numberNew + " " + getResources().getString(R.string.new_mentions));

                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                resultIntent.putExtra("open_to_page", 1);
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

                    mBuilder.setContentIntent(resultPendingIntent);
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(mId, mBuilder.build());

                    // if we want to wake the screen on a new message
                    if (settings.wakeScreen) {
                        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        final PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                        wakeLock.acquire(5000);
                    }

                }
            }

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}