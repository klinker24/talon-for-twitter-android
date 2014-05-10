package com.klinker.android.twitter.ui.launcher_page.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.klinker.android.twitter.services.TalonPullNotificationService;
import com.klinker.android.twitter.settings.AppSettings;

public class StartPull extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        sharedPrefs.edit()
                .putBoolean("launcher_frag_switch", true)
                .putInt("current_account", getIntent().getIntExtra("current_account", 1))
                .commit();

        AppSettings.invalidate();

        if (AppSettings.getInstance(this).pushNotifications && !TalonPullNotificationService.isRunning) {
            startService(new Intent(this, TalonPullNotificationService.class));
        }

        overridePendingTransition(0,0);
        finish();
        overridePendingTransition(0,0);
    }
}
