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

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Context context = getApplicationContext();

        sharedPrefs.edit().putBoolean("new_notification", false).commit();
        sharedPrefs.edit().putInt("new_retweets", 0).commit();
        sharedPrefs.edit().putInt("new_favorites", 0).commit();
        sharedPrefs.edit().putInt("new_follows", 0).commit();
        sharedPrefs.edit().putString("old_interaction_text", "").commit();
    }

}