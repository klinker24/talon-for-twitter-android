package com.klinker.android.twitter_l.utils.redirects;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;

public class RedirectToMyAccount extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0, 0);
        ProfilePager.start(this, AppSettings.getInstance(this).myScreenName.replace("@", "").replaceAll(" ", ""));

        overridePendingTransition(0, 0);
        finish();
    }
}