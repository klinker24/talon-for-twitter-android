package com.klinker.android.twitter.utils.api_helper;

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import twitter4j.Twitter;

public class TweetMarkerHelper extends APIHelper {

    public static final String TWEETMARKER_API_KEY = "TA-89115729700A";

    private int currentAccount;
    private String screenname;
    private String postURL;
    private Twitter twitter;

    public TweetMarkerHelper(int currentAccount, String screenname, Twitter twitter) {
        this.currentAccount = currentAccount;
        this.screenname = screenname;
        this.twitter = twitter;

        postURL = "http://api.tweetmarker.net/v2/lastread?api_key=" + Uri.encode(TWEETMARKER_API_KEY) +
                "&username=" + Uri.encode(screenname);
    }

    public void authorize() {
        try {
            String url = "http://api.tweetmarker.net/v2/auth?api_key=" + Uri.encode(TWEETMARKER_API_KEY) +
                    "&username=" + Uri.encode(screenname);

            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(url);

            Log.v("talon_tweetmarker", "authorizing");

            HttpResponse response = client.execute(get);
            Log.v("talon_tweetmarker", "response code: " + response.getStatusLine().getStatusCode());
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                Log.v("talon_tweetmarker", line);
                builder.append(line);
            }

            Log.v("talon_tweetmarker", "done authorizing");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendCurrentId(String collection, long id) {
        try {
            HttpPost post = new HttpPost(postURL);
            post.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            post.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            JSONObject json = new JSONObject();
            json.put("id", id);
            JSONObject base = new JSONObject();
            base.put(collection, json);

            Log.v("talon_tweetmarker", "sending " + id + " to " + screenname);

            post.setEntity(new ByteArrayEntity(base.toString().getBytes("UTF8")));
            DefaultHttpClient client = new DefaultHttpClient();

            HttpResponse response = client.execute(post);
            int responseCode = response.getStatusLine().getStatusCode();
            Log.v("talon_tweetmarker", "sending response code: " + responseCode);

            if (responseCode != 200) { // there was an error, we will retry once
                // wait first
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {

                }

                response = client.execute(post);
                responseCode = response.getStatusLine().getStatusCode();
                Log.v("talon_tweetmarker", "sending response code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getLastStatus(String collection, int lastVersion, SharedPreferences sharedPrefs) {
        try {
            HttpGet get = new HttpGet(postURL + "&" + collection);
            get.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            get.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            HttpClient client = new DefaultHttpClient();

            HttpResponse response = client.execute(get);
            Log.v("talon_tweetmarker", "getting id response code: " + response.getStatusLine().getStatusCode() + " for " + screenname);

            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) { // request ok
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    builder.append(line);
                }

                JSONObject jsonObject = new JSONObject(builder.toString());
                JSONObject timeline = jsonObject.getJSONObject(collection);

                if (timeline != null) {

                    long val = Long.parseLong(timeline.getString("id"));
                    int version = timeline.getInt("version");
                    Log.v("talon_tweetmarker", "getting tweetmarker, version: " + version + " id: " + val + " screename: " + screenname);

                    if (version != lastVersion) {
                        // don't want to move the timeline if the version is the same

                        // this increments the version from shared prefs
                        sharedPrefs.edit().putInt("last_version_account_" + currentAccount, version).commit();
                        return val; // returns the long id from tweetmarker
                    }
                } else {
                    Log.v("talon_tweetmarker", "timeline is null for the response");
                }
            } else { // there was an error, we will retry once
                // wait first
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {

                }

                response = client.execute(get);

                statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == 200) { // request ok
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                    String line;
                    StringBuilder builder = new StringBuilder();
                    while ((line = rd.readLine()) != null) {
                        builder.append(line);
                    }

                    JSONObject jsonObject = new JSONObject(builder.toString());
                    JSONObject timeline = jsonObject.getJSONObject(collection);

                    if (timeline != null) {

                        long val = timeline.getLong("id");
                        int version = timeline.getInt("version");
                        Log.v("talon_tweetmarker", "getting tweetmarker, version: " + version + " id: " + val + " screename: " + screenname);

                        if (version != lastVersion) {
                            // don't want to move the timeline if the version is the same

                            // this increments the version from shared prefs
                            sharedPrefs.edit().putInt("last_version_account_" + currentAccount, version).commit();
                            return val; // returns the long id from tweetmarker
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {

        }

        return 0;
    }
}
