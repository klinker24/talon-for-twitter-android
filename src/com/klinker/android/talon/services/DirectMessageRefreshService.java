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
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.MainActivityPopup;
import com.klinker.android.talon.utils.Utils;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class DirectMessageRefreshService extends IntentService {

    SharedPreferences sharedPrefs;

    public DirectMessageRefreshService() {
        super("DirectMessageRefreshService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Context context = getApplicationContext();
        boolean update = false;
        int numberNew = 0;

        try {
            Twitter twitter = Utils.getTwitter(context);

            User user = twitter.verifyCredentials();
            long lastId = sharedPrefs.getLong("last_direct_message_id", 0);
            Paging paging;
            if (lastId != 0) {
                paging = new Paging(1).sinceId(lastId);
            } else {
                paging = new Paging(1, 500);
            }

            List<DirectMessage> dm = twitter.getDirectMessages(paging);
            List<DirectMessage> sent = twitter.getSentDirectMessages(paging);

            if (dm.size() != 0) {
                sharedPrefs.edit().putLong("last_direct_message_id", dm.get(0).getId()).commit();
                update = true;
                numberNew = dm.size();
            } else {
                update = false;
                numberNew = 0;
            }

            DMDataSource dataSource = new DMDataSource(context);
            dataSource.open();

            for (DirectMessage directMessage : dm) {
                try {
                    dataSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                } catch (Exception e) {
                    break;
                }
            }

            for (DirectMessage directMessage : sent) {
                try {
                    dataSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                } catch (Exception e) {
                    break;
                }
            }

            dataSource.close();

            int mId = 3;

            if (numberNew > 0) {

                int currentUnread = sharedPrefs.getInt("dm_unread", 0);
                sharedPrefs.edit().putInt("dm_unread", numberNew + currentUnread).commit();
                numberNew += currentUnread;

                RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
                Intent popup = new Intent(context, MainActivityPopup.class);
                popup.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent popupPending =
                        PendingIntent.getActivity(
                                this,
                                0,
                                popup,
                                0
                        );
                remoteView.setOnClickPendingIntent(R.id.popup_button, popupPending);
                remoteView.setTextViewText(R.id.content, numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_direct_message) : numberNew + " " + getResources().getString(R.string.new_direct_messages));

                remoteView.setImageViewResource(R.id.icon, R.drawable.ic_action_reply_dark);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_action_reply_dark)
                                //.setContent(remoteView);
                                .setContentTitle(getResources().getString(R.string.app_name))
                                .setContentText(numberNew == 1 ? numberNew + " " + getResources().getString(R.string.new_direct_message) : numberNew + " " + getResources().getString(R.string.new_direct_messages));

                Intent resultIntent = new Intent(this, MainActivity.class);
                resultIntent.putExtra("open_to_page", 2);
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
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }
    }
}