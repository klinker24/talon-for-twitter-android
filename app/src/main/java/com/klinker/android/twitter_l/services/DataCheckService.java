package com.klinker.android.twitter_l.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Handler;
import android.util.Log;

import com.klinker.android.twitter_l.activities.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;

public class DataCheckService extends IntentService {

    public  static final long RESTART_INTERVAL = 15 * 60 * 1000; // 15 mins

    public DataCheckService() {
        super("DataCheckService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(this);
        int uid = getApplicationInfo().uid;

        long oldMb = sharedPreferences.getLong("last_check_data_mb", 0L);
        long currentMb = (TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid)) / 1000000;

        sharedPreferences.edit().putLong("last_check_data_mb", currentMb).commit();

        if (oldMb != 0 && currentMb - oldMb > 100) {
            //Object o = null;
            //o.hashCode();
            alertLuke(currentMb - oldMb);
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        scheduleRefresh(this);
    }

    public static void scheduleRefresh(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long now = new Date().getTime();
        long alarm = now + RESTART_INTERVAL;

        PendingIntent pendingIntent = PendingIntent.getService(context, 1100, new Intent(context, DataCheckService.class), 0);

        am.cancel(pendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);

        Log.v("alarm_date", "data check: " + new Date(alarm).toString());

    }

    private void alertLuke(long dataUsed) {
        String message = URLEncoder.encode("{\"message_title\":\"" + "@" + AppSettings.getInstance(this).myScreenName + " got killed in data check service" + "\",\"message_text\":\"Data used in 15 mins: " + dataUsed + "MB\",\"message_type\":\"message\",\"message_from_user\":{\"user_name\":\"client\"},\"message_to_user\":{\"user_name\":\"luke\"}}");

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
