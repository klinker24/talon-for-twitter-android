package com.klinker.android.twitter_l.services;
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
import android.content.Intent;
import android.content.SharedPreferences;

import com.klinker.android.twitter_l.services.abstract_services.KillerIntentService;
import com.klinker.android.twitter_l.settings.AppSettings;

public class ReadInteractionsService extends KillerIntentService {

    SharedPreferences sharedPrefs;

    public ReadInteractionsService() {
        super("MarkReadService");
    }

    @Override
    public void handleIntent(Intent intent) {
        markRead(this);
    }

    public static void markRead(Context context) {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        // clear custom light flow broadcast
        Intent lightFlow = new Intent("com.klinker.android.twitter.CLEARED_NOTIFICATION");
        context.sendBroadcast(lightFlow);

        sharedPrefs.edit().putBoolean("new_notification", false)
                .putInt("new_retweets", 0)
                .putInt("new_favorites", 0)
                .putInt("new_followers", 0)
                .putInt("new_quotes", 0)
                .putString("old_interaction_text", "")
                .apply();
    }
}
