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
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.launcher_page.LauncherPopup;


public class RedirectToLauncherPopup extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSettings.getSharedPreferences(this)
                .edit()
                .putInt("current_account",
                        getIntent().getIntExtra("current_account", 1))
                .apply();

        AppSettings.invalidate();

        Intent popup = new Intent(this, LauncherPopup.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("launcher_page", getIntent().getIntExtra("launcher_page", 0));
        popup.putExtra("from_launcher", true);
        finish();

        startActivity(popup);
    }
}
