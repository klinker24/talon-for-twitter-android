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

        Intent profile = new Intent(this, ProfilePager.class);
        profile.putExtra("screenname",
                AppSettings.getInstance(this).myScreenName.replace("@", "").replaceAll(" ", ""));
        profile.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        overridePendingTransition(0, 0);
        startActivity(profile);

        overridePendingTransition(0, 0);
        finish();
    }
}