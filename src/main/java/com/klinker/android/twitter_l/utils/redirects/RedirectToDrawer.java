package com.klinker.android.twitter_l.utils.redirects;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.klinker.android.twitter_l.ui.MainActivity;

/**
 * Created by luke on 2/22/14.
 */
public class RedirectToDrawer extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent drawer = new Intent(this, MainActivity.class);

        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        sharedPrefs.edit().putBoolean("open_interactions", true).commit();

        finish();

        startActivity(drawer);
    }
}