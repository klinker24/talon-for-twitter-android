package com.klinker.android.twitter.utils.redirects;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.widget.launcher_fragment.LauncherPopup;


public class RedirectToLauncherPopup extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                .edit()
                .putInt("current_account",
                        getIntent().getIntExtra("current_account", 1))
                .commit();

        AppSettings.invalidate();

        Intent popup = new Intent(this, LauncherPopup.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("launcher_page", getIntent().getIntExtra("launcher_page", 0));
        finish();

        startActivity(popup);
    }
}
