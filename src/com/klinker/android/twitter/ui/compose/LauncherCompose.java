package com.klinker.android.twitter.ui.compose;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.klinker.android.twitter.settings.AppSettings;

public class LauncherCompose extends ComposeActivity {

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

    @Override
    public void setUpLayout() {
        super.setUpLayout();
        attachButton.setVisibility(View.GONE);
    }
}
