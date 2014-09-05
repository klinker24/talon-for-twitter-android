package com.klinker.android.twitter_l.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.data.ScheduledTweet;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.services.CatchupPull;
import com.klinker.android.twitter_l.services.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.SendScheduledTweet;
import com.klinker.android.twitter_l.services.TimelineRefreshService;
import com.klinker.android.twitter_l.services.TrimDataService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.MentionsFragment;
import com.klinker.android.twitter_l.ui.scheduled_tweets.ViewScheduledTweets;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by luke on 11/26/13.
 */
public class BootReceiver extends BroadcastReceiver {

    public static final int TRIM_ID = 131;

    private Context context;
    private SharedPreferences sharedPrefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        AppSettings settings = AppSettings.getInstance(context);

        if (settings.timelineRefresh != 0) { // user only wants manual
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + settings.timelineRefresh;

            Log.v("alarm_date", "timeline " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, HomeFragment.HOME_REFRESH_ID, new Intent(context, TimelineRefreshService.class), 0);

            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.timelineRefresh, pendingIntent);
        }

        if (settings.mentionsRefresh != 0) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + settings.mentionsRefresh;

            Log.v("alarm_date", "mentions " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, MentionsFragment.MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);

            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.mentionsRefresh, pendingIntent);
        }

        if (settings.dmRefresh != 0) { // user only wants manual
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + settings.dmRefresh;

            Log.v("alarm_date", "dircet message " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, DMFragment.DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);

            am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.dmRefresh, pendingIntent);
        }

        if (settings.autoTrim) { // user only wants manual
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + 1000*60;

            Log.v("alarm_date", "auto trim " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, TRIM_ID, new Intent(context, TrimDataService.class), 0);

            am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);
        }

        if (settings.pushNotifications) {
            context.startService(new Intent(context, CatchupPull.class));
        }

        ArrayList<ScheduledTweet> tweets = QueuedDataSource.getInstance(context).getScheduledTweets();

        for (ScheduledTweet s : tweets) {
            Intent serviceIntent = new Intent(context.getApplicationContext(), SendScheduledTweet.class);

            Log.v("talon_scheduled_tweets", "in boot text: " + s.text);

            serviceIntent.putExtra(ViewScheduledTweets.EXTRA_TEXT, s.text);
            serviceIntent.putExtra("account", s.account);
            serviceIntent.putExtra("alarm_id", s.alarmId);

            PendingIntent pi = getDistinctPendingIntent(serviceIntent, s.alarmId);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            am.set(AlarmManager.RTC_WAKEUP,
                    s.time,
                    pi);
        }

    }

    protected PendingIntent getDistinctPendingIntent(Intent intent, int requestId) {
        PendingIntent pi =
                PendingIntent.getService(
                        context,
                        requestId,
                        intent,
                        0);

        return pi;
    }


}
