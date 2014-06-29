package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class StopPull extends IntentService {

    SharedPreferences sharedPrefs;

    public StopPull() {
        super("StopPull");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        sharedPreferences.edit().putBoolean("push_notifications", false).commit();

        // write to normal prefs so that it appears in the settings
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("push_notifications", false).commit();

        sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
    }
}
