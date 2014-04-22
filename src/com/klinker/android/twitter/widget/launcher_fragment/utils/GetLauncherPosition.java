package com.klinker.android.twitter.widget.launcher_fragment.utils;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.util.Log;

import com.klinker.android.twitter.settings.AppSettings;


public class GetLauncherPosition extends IntentService {

    public static AppSettings settings;
    public static long id;

    public GetLauncherPosition() {
        super("GetLauncherPosition");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int currentAccount = intent.getIntExtra("current_account", 1);
        id = PreferenceManager.getDefaultSharedPreferences(this).getLong("current_position_" + currentAccount, 0l);
        settings = AppSettings.getInstance(this);

        try {
            Context launcherContext = createPackageContext("com.klinker.android.launcher", Context.CONTEXT_IGNORE_SECURITY);

            Intent returnPos = new Intent("com.klinker.android.twitter.LAUNCHER_POSITION");
            returnPos.addCategory(Intent.CATEGORY_DEFAULT);
            returnPos.putExtra("pos_for_launcher", id);

            Log.v("talon_fragment", "sending id: " + id);
            launcherContext.sendBroadcast(returnPos);
        } catch (Exception e) {

        }

    }
}
