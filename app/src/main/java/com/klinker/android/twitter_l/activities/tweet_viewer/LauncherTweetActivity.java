package com.klinker.android.twitter_l.activities.tweet_viewer;
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


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.settings.AppSettings;

public class LauncherTweetActivity extends TweetActivity {

    @Override
    public View onCreateContent(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        fromLauncher = true;

        int acc = getIntent().getIntExtra("current_account", 0);

        if (acc != 0) {
            AppSettings.getSharedPreferences(this)
                    .edit()
                    .putInt("current_account", acc)
                    .apply();

            AppSettings.invalidate();
        }

        return super.onCreateContent(inflater, parent, savedInstanceState);
    }
}
