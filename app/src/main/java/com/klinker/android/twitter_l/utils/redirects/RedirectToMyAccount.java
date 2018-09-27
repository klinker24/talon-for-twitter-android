package com.klinker.android.twitter_l.utils.redirects;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
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