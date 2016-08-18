package com.klinker.android.twitter_l.services;

import android.content.Intent;
import android.content.SharedPreferences;

import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.Date;

/**
 * Only allow these services to run once every few mins
 */
public abstract class LimitedRunService extends KillerIntentService {

    private static final long TIMEOUT = 3 * 60 * 1000; // 3 mins

    public LimitedRunService(String name) {
        super(name);
    }

    protected abstract void handleIntentIfTime(Intent intent);
    protected abstract long getLastRun();
    protected abstract void setJustRun(long currentTime);

    protected void handleIntent(Intent intent) {
        if (dontRunMoreThanEveryMins(intent)) {
            handleIntentIfTime(intent);
        }
    }

    // return true if it should refresh, false if it has been refreshed within the last min
    // this is overridden in the timeline refresh service
    protected boolean dontRunMoreThanEveryMins(Intent intent) {
        long currentTime = new Date().getTime();
        if (getLastRun() != 0 && currentTime - getLastRun() < TIMEOUT) {
            return false;
        } else {
            setJustRun(currentTime);
            return true;
        }
    }
}
