package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;

/**
 * Created by luke on 2/22/14.
 */
public class RedirectToDrawer extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent mentions = new Intent(this, MainActivity.class);
        mentions.putExtra("open_interactions", true);

        finish();

        startActivity(mentions);
    }
}