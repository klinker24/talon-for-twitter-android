package com.klinker.android.twitter.ui.launcher_page;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.util.Log;
import com.klinker.android.twitter.services.TalonPullNotificationService;
import com.klinker.android.twitter.settings.AppSettings;

public class StartPull extends IntentService {

    public StartPull() {
        super("StartPull");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v("talon_launcher", "starting pull from launcher service");

        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        sharedPrefs.edit()
                .putBoolean("launcher_frag_switch", true)
                .putInt("current_account", intent.getIntExtra("current_account", 1))
                .commit();

        AppSettings.invalidate();

        if (AppSettings.getInstance(this).pushNotifications && !TalonPullNotificationService.isRunning) {
            startService(new Intent(this, TalonPullNotificationService.class));
        }
    }
}
