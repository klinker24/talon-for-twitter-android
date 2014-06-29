package com.klinker.android.twitter_l.ui.compose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.klinker.android.twitter_l.settings.AppSettings;

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

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(0,0);
                startActivity(new Intent(context, ComposeActivity.class).putExtra("start_attach", true));
            }
        });
    }
}
