package com.klinker.android.twitter_l.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by lklinker on 8/17/16.
 */

public class PushSyncSender {

    public static void sendToLuke(String title, String text) {
        String message = URLEncoder.encode("{\"message_title\":\"" + title + "\",\"message_text\":\"" + text + "\",\"message_type\":\"message\",\"message_from_user\":{\"user_name\":\"client\"},\"message_to_user\":{\"user_name\":\"luke\"}}");

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
