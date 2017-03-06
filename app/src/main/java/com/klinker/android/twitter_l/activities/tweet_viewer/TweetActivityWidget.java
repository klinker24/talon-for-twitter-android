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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class TweetActivityWidget extends TweetActivity {

    BroadcastReceiver attach = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent activity = new Intent(context, TweetActivity.class).putExtras(getIntent());
            TweetActivity.applyDragDismissBundle(context, activity);

            startActivity(activity);
            finish();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(attach, new IntentFilter("com.klinker.android.twitter.ATTACH_BUTTON"));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(attach);
    }

}
