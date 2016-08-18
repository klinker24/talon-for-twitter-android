package com.klinker.android.twitter_l.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

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

    public static void scheduleRefresh(Context context) {
        ScheduledService.ScheduleInfo info = new ScheduledService.ScheduleInfo(ActivityRefreshService.class, ActivityFragment.ACTIVITY_REFRESH_ID, AppSettings.getInstance(context).activityRefresh);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, info.pendingIntentId, new Intent(context, info.clazz), 0);

        if (info.interval != 0) {
            long now = new Date().getTime();
            long alarm = now + info.interval;

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
