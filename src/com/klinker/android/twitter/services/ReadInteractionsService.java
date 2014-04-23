package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ReadInteractionsService extends IntentService {

    SharedPreferences sharedPrefs;

    public ReadInteractionsService() {
        super("MarkReadService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        // clear custom light flow broadcast
        Intent lightFlow = new Intent("com.klinker.android.twitter.CLEARED_NOTIFICATION");
        this.sendBroadcast(lightFlow);

        sharedPrefs.edit().putBoolean("new_notification", false).commit();
        sharedPrefs.edit().putInt("new_retweets", 0).commit();
        sharedPrefs.edit().putInt("new_favorites", 0).commit();
        sharedPrefs.edit().putInt("new_follows", 0).commit();
        sharedPrefs.edit().putString("old_interaction_text", "").commit();
    }

}