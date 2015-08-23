package com.klinker.android.twitter_l.ui.tweet_viewer;
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


import android.content.Context;
import android.os.Bundle;

import com.klinker.android.twitter_l.settings.AppSettings;

public class LauncherTweetActivity extends TweetActivity {

    @Override
    public void init(Bundle savedInstanceState) {

        fromLauncher = true;

        int acc = getIntent().getIntExtra("current_account", 0);

        if (acc != 0) {
            getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                    .edit()
                    .putInt("current_account", acc)
                    .commit();

            AppSettings.invalidate();
        }

        super.init(savedInstanceState);
    }
}
