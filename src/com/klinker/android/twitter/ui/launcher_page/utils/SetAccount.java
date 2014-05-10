package com.klinker.android.twitter.ui.launcher_page.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;

public class SetAccount extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        int launcherAccount = getIntent().getIntExtra("current_account", 1);

        // this checks if the account has switched and will act accordingly
        if (launcherAccount != sharedPrefs.getInt("current_account", 1)) {
            sharedPrefs.edit()
                    .putBoolean("launcher_frag_switch", true)
                    .putInt("current_account", launcherAccount)
                    .commit();

            AppSettings.invalidate();
        }

        // finish the activity and start the activity with no animation so the user doesn't notice
        overridePendingTransition(0,0);
        finish();
        overridePendingTransition(0,0);

        if (getIntent().getBooleanExtra("start_main", false)) {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}
