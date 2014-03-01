package com.klinker.android.twitter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;

import com.klinker.android.twitter.services.CatchupPull;
import com.klinker.android.twitter.services.TalonPullNotificationService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.v("talon_pull", "connectivity change: just starting receiver");

        AppSettings settings = AppSettings.getInstance(context);

        // we don't want to do anything here if talon pull isn't on
        if (!settings.pushNotifications) {
            return;
        }

        if (Utils.hasInternetConnection(context)) {
            Log.v("talon_pull", "connectivity change: network is available and talon pull is on");

            // here we want to turn talon pull back on, but lets wait a little bit just to make sure it stays on
            // we will give it 5 seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // make sure we still have the connection before starting the service again
                    if (Utils.hasInternetConnection(context)) {
                        Log.v("talon_pull", "connectivity change: network is still available, starting Talon Pull now.");
                        context.startService(new Intent(context, CatchupPull.class));
                    }
                }
            }, 5000);

        } else {
            Log.v("talon_pull", "connectivity change: network not available but talon pull is on");

            // we want to turn off the live streaming/talon pull because it is just wasting battery not working/looking for connection
            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
        }
    }
}