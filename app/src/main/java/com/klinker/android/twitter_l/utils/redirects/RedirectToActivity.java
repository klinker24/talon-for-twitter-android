package com.klinker.android.twitter_l.utils.redirects;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;


public class RedirectToActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);


        int currentAccount = sharedPrefs.getInt("current_account", 1);

        int page = 0;
        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_ACTIVITY) {
                page = i;
            }
        }

        Intent mentions = new Intent(this, MainActivity.class);

        sharedPrefs.edit().putBoolean("open_a_page", true).apply();
        sharedPrefs.edit().putInt("open_what_page", page).apply();

        finish();

        overridePendingTransition(0,0);

        startActivity(mentions);
    }
}