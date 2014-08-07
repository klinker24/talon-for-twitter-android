package com.klinker.android.twitter_l.ui.launcher_page;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;

public class SetAccount extends IntentService {

    public SetAccount() {
        super("SetAccount");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v("talon_setting_account", "setting account to " + intent.getIntExtra("current_account", 1));

        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        int launcherAccount = intent.getIntExtra("current_account", 1);

        // this checks if the account has switched and will act accordingly
        if (launcherAccount != sharedPrefs.getInt("current_account", 1)) {
            sharedPrefs.edit()
                    .putBoolean("launcher_frag_switch", true)
                    .commit();

            AppSettings.invalidate();
        }

        sharedPrefs.edit().putInt("current_account", launcherAccount).commit();

        if (intent.getBooleanExtra("start_main", false)) {
            startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
