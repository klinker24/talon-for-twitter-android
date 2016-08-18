package com.klinker.android.twitter_l.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.klinker.android.twitter_l.activities.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.ActivityFragment;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ActivityUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Date;

public class ActivityRefreshService extends LimitedRunService {

    SharedPreferences sharedPrefs;

    public ActivityRefreshService() {
        super("ActivityRefreshService");
    }

    public static void cancelRefresh(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getRefreshPendingIntent(context);

        am.cancel(pendingIntent);
    }

    private static PendingIntent getRefreshPendingIntent(Context context) {
        return PendingIntent.getService(
                context,
                ActivityFragment.ACTIVITY_REFRESH_ID,
                new Intent(context, ActivityRefreshService.class),
                0);
    }

    public static void scheduleRefresh(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getRefreshPendingIntent(context);

        if (settings.activityRefresh != 0) {
            long now = new Date().getTime();
            long alarm = now + settings.activityRefresh;

            am.cancel(pendingIntent);
            am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);
        } else {
            am.cancel(pendingIntent);
        }
    }

    @Override
    public void handleIntentIfTime(Intent intent) {
        scheduleRefresh(this);

        AppSettings settings = AppSettings.getInstance(this);
        ActivityUtils utils = new ActivityUtils(this, false);

        if (Utils.getConnectionStatus(this) && !settings.syncMobile) {
            return;
        }

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification();
        }

        if (settings.syncSecondMentions) {
            Intent second = new Intent(this, SecondActivityRefreshService.class);
            startService(second);
        }
    }

    private static long LAST_RUN = 0;

    @Override
    protected long getLastRun() {
        return LAST_RUN;
    }

    @Override
    protected void setJustRun(long currentTime) {
        LAST_RUN = currentTime;
    }
}
