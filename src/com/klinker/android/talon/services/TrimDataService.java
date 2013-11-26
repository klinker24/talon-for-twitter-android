package com.klinker.android.talon.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.talon.utils.IOUtils;

/**
 * Created by luke on 11/26/13.
 */
public class TrimDataService extends IntentService {

    SharedPreferences sharedPrefs;

    public TrimDataService() {
        super("TrimDataService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        Log.v("trimming_database", "trimming database from service");
        IOUtils.trimDatabase(getApplicationContext());
    }
}
