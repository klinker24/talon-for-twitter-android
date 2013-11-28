package com.klinker.android.talon.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.MainActivityPopup;
import com.klinker.android.talon.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class TimelineRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public TimelineRefreshService() {
        super("TimelineRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Context context = getApplicationContext();
        int numberNew = 0;

        AppSettings settings = new AppSettings(context);

        try {
            Twitter twitter = Utils.getTwitter(context);

            int currentAccount = sharedPrefs.getInt("current_account", 1);

            User user = twitter.verifyCredentials();
            long lastId = sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0);
            long secondToLastId = sharedPrefs.getLong("second_last_tweet_id_" + currentAccount, 0);
            List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

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
                    dataSource.createTweet(status, currentAccount);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            dataSource.close();

            int mId = 1;

            if (numberNew > 0) {

                int currentUnread = sharedPrefs.getInt("timeline_new_" + currentAccount, 0);
                sharedPrefs.edit().putInt("timeline_new_" + currentAccount, numberNew + currentUnread).commit();
                numberNew += currentUnread;

                RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
                Intent popup = new Intent(context, MainActivityPopup.class);
                popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(mId, mBuilder.build());
            }

        } catch (TwitterException e) {
            Log.d("Twitter Update Error", e.getMessage());
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