package com.klinker.android.twitter_l.utils.redirects;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;

public class SwitchAccountsToActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);


        int page = -1;
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        if (currentAccount == 1) {
            sharedPrefs.edit().putInt("current_account", 2).apply();
            currentAccount = 2;
        } else {
            sharedPrefs.edit().putInt("current_account", 1).apply();
            currentAccount = 1;
        }

        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type == AppSettings.PAGE_TYPE_ACTIVITY) {
                page = i;
            }
        }

        if (page == -1) {
            page = 0;
        }

        sharedPrefs.edit().putBoolean("open_a_page", true).apply();
        sharedPrefs.edit().putInt("open_what_page", page).apply();

        // close talon pull if it is on. will be restarted when the activity starts
        sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        AppSettings.invalidate();
        main.putExtra("switch_account", true);
        overridePendingTransition(0, 0);
        finish();
        startActivity(main);
    }
}
