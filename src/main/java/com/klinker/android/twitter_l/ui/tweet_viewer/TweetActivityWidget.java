package com.klinker.android.twitter_l.ui.tweet_viewer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class TweetActivityWidget extends TweetActivity {

    BroadcastReceiver attach = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startActivity(new Intent(context, TweetActivity.class).putExtras(getIntent()));
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
