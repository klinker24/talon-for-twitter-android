package com.klinker.android.twitter_l.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.utils.IOUtils;

import java.util.Date;

public class TrimDataService extends IntentService {

    SharedPreferences sharedPrefs;

    public static final int TRIM_ID = 161;

    public TrimDataService() {
        super("TrimDataService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.v("trimming_database", "trimming database from service");
        IOUtils.trimDatabase(getApplicationContext(), 1); // trims first account
        IOUtils.trimDatabase(getApplicationContext(), 2); // trims second account

        getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        setNextTrim(this);
    }

    public void setNextTrim(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long now = new Date().getTime();
        long alarm = now + AlarmManager.INTERVAL_DAY;

        Log.v("alarm_date", "auto trim " + new Date(alarm).toString());

        PendingIntent pendingIntent = PendingIntent.getService(context, TRIM_ID, new Intent(context, TrimDataService.class), 0);

        am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);
    }
}
