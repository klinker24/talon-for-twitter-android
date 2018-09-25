package com.klinker.android.twitter_l.utils.redirects;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;

public class RedirectToTimeline extends AppCompatActivity {

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

            if (type == AppSettings.PAGE_TYPE_HOME) {
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