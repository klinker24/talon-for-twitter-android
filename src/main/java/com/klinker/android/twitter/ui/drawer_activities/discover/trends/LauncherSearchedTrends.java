package com.klinker.android.twitter.ui.drawer_activities.discover.trends;

import android.content.Context;
import android.os.Bundle;

import com.klinker.android.twitter.settings.AppSettings;

/**
 * Created by luke on 4/24/14.
 */
public class LauncherSearchedTrends extends SearchedTrendsActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        int acc = getIntent().getIntExtra("current_account", 0);

        if (acc != 0) {
            getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                    .edit()
                    .putInt("current_account", acc)
                    .commit();

            AppSettings.invalidate();
        }

        super.onCreate(savedInstanceState);
    }

}
