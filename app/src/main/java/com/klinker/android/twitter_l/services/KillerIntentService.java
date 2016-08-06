package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public abstract class KillerIntentService extends IntentService {

    interface KillerCallback {
        void onKill();
    }

    private static final long TIMEOUT = 120000; // 120 seconds

    public KillerIntentService(String name) {
        super(name);
    }

    protected abstract void handleIntent(Intent intent);

    @Override
    public final void onHandleIntent(Intent intent) {
        final KillerCallback callback = new KillerCallback() {
            @Override
            public void onKill() {
                Object o = null;
                o.hashCode();
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

        handleIntent(intent);

        // stop the killer from destroying the app
        killer.interrupt();
    }
}
