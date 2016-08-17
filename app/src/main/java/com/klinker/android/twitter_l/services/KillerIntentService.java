package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.PushSyncSender;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

public abstract class KillerIntentService extends IntentService {

    interface KillerCallback {
        void onKill();
    }

    private static final long TIMEOUT = 120000; // 120 seconds

    private String name;

    public KillerIntentService(String name) {
        super(name);
        this.name = name;
    }

    protected abstract void handleIntent(Intent intent);

    @Override
    public final void onHandleIntent(Intent intent) {
        final KillerCallback callback = new KillerCallback() {
            @Override
            public void onKill() {
                //Object o = null;
                //o.hashCode();
                PushSyncSender.sendToLuke(
                        "<b>Talon:</b>@" + AppSettings.getInstance(KillerIntentService.this).myScreenName + " had a problem.",
                        "The " + name + " was shut down in the background after 2 mins."
                );
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        };

        // activity sometimes get stuck and burns though data... I have not been able to find out why.
        // So, lets kill the process if it takes longer than 45 seconds
        Thread killer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(TIMEOUT);

                    Log.v("talon_killer", "activity refresh killed. What is the issue here...?");

                    callback.onKill();
                } catch (InterruptedException e) {

                }
            }
        });

        killer.start();

        if (dontRunMoreThanEveryMins(intent)) {
            handleIntent(intent);
        }

        // stop the killer from destroying the app
        killer.interrupt();
    }

    // return true if it should refresh, false if it has been refreshed within the last min
    // this is overridden in the timeline refresh service
    protected boolean dontRunMoreThanEveryMins(Intent intent) {
        SharedPreferences prefs = AppSettings.getSharedPreferences(this);

        long currentTime = new Date().getTime();
        if (prefs.contains(name + "_killer_timeout") && currentTime - prefs.getLong(name + "_killer_timeout", currentTime) < TIMEOUT) {
            return false;
        } else {
            updateLastRunTime(prefs, currentTime);
            return true;
        }
    }

    private void updateLastRunTime(SharedPreferences prefs, long time) {
        prefs.edit()
                .putLong(name + "_killer_timeout", time)
                .commit();
    }
}
