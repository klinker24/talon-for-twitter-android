package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.content.Context;
import android.os.Bundle;

import com.klinker.android.twitter_l.settings.AppSettings;

/**
 * Created by luke on 4/21/14.
 */
public class LauncherTweetActivity extends TweetActivity {

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
