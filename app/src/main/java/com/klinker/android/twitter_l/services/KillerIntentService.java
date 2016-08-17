package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.settings.AppSettings;

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
                alertLuke();
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

    private void alertLuke() {
        String message = URLEncoder.encode("{\"message_title\":\"Talon Background service killed\",\"message_text\":\"" + name + "\",\"message_type\":\"message\",\"message_from_user\":{\"user_name\":\"client\"},\"message_to_user\":{\"user_name\":\"luke\"}}");

        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL("https://omega-jet-799.appspot.com/_ah/api/messaging/v1/sendMessage/");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length", "" +
                    Integer.toString(message.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(message);
            wr.flush ();
            wr.close ();

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}
