package com.klinker.android.twitter_l.activities.launcher_page;
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

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;

public class SetAccount extends IntentService {

    public SetAccount() {
        super("SetAccount");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v("talon_setting_account", "setting account to " + intent.getIntExtra("current_account", 1));

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);


        int launcherAccount = intent.getIntExtra("current_account", 1);

        // this checks if the account has switched and will act accordingly
        if (launcherAccount != sharedPrefs.getInt("current_account", 1)) {
            sharedPrefs.edit()
                    .putBoolean("launcher_frag_switch", true)
                    .apply();

            AppSettings.invalidate();
        }

        sharedPrefs.edit().putInt("current_account", launcherAccount).apply();

        if (intent.getBooleanExtra("start_main", false)) {
            startActivity(new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
