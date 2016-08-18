package com.klinker.android.twitter_l.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.klinker.android.twitter_l.activities.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.Date;

public abstract class ScheduledService extends KillerIntentService {

    protected static class ScheduleInfo {
        public Class clazz;
        public int pendingIntentId;
        public long interval;

        public ScheduleInfo(Class clazz, int pendingIntentId, long interval) {
            this.clazz = clazz;
            this.pendingIntentId = pendingIntentId;
            this.interval = interval;
        }
    }

    // override for implementation
    protected ScheduleInfo getScheduleInfo(AppSettings settings) {
        return new ScheduleInfo(ScheduledService.class, 0, 0);
    }

    public ScheduledService(String name) {
        super(name);
    }

    @Override
    public final void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        scheduleRefresh(this);
    }

    public void scheduleRefresh(Context context) {
        ScheduleInfo info = getScheduleInfo(AppSettings.getInstance(context));

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
}
