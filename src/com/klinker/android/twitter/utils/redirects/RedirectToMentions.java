package com.klinker.android.twitter.utils.redirects;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.MainActivityPopup;

public class RedirectToMentions extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        int page1Type = sharedPrefs.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
        int page2Type = sharedPrefs.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);

        int extraPages = 0;
        if (page1Type != AppSettings.PAGE_TYPE_NONE) {
            extraPages++;
        }

        if (page2Type != AppSettings.PAGE_TYPE_NONE) {
            extraPages++;
        }

        Intent mentions = new Intent(this, MainActivity.class);
        mentions.putExtra("page_to_open", 1 + extraPages);
        mentions.putExtra("from_drawer", true);

        finish();

        startActivity(mentions);
    }
}