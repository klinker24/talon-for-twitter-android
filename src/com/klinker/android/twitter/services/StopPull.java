package com.klinker.android.twitter.services;

import android.app.IntentService;
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean("push_notifications", false).commit();

        sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
    }
}
