package com.klinker.android.twitter_l.ui.launcher_page;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.settings.AppSettings;


public class HandleScrollService extends IntentService {

    public static AppSettings settings;
    public static long id;

    public HandleScrollService() {
        super("HandleScrollService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v("talon_launcher", "running scroll service");

        sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        sharedPrefs.edit().putBoolean("refresh_me", true).commit();
    }
}
