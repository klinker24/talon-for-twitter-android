package com.klinker.android.twitter_l.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class PushSyncSender {

    public static void sendToLuke(String title, String text) {
        URL url;
        HttpURLConnection connection = null;
        try {
            String message = URLEncoder.encode("{\"message_title\":\"" + title + "\",\"message_text\":\"" + text + "\",\"message_type\":\"message\",\"message_from_user\":{\"user_name\":\"client\"},\"message_to_user\":{\"user_name\":\"luke\"}}", "UTF-8");
            url = new URL("https://omega-jet-799.appspot.com/_ah/api/messaging/v1/sendMessage/" + message);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write("");
            wr.close();

            String reponse = "Post complete, code: " + connection.getResponseCode() + " " + connection.getResponseMessage();
            Log.v("talon_response", "response: " + reponse);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}
