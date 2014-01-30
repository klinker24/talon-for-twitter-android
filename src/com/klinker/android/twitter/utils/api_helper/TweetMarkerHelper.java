package com.klinker.android.twitter.utils.api_helper;

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

    public long sendCurrentId(String collection, long id) {
        try {
            HttpPost post = new HttpPost(postURL);
            post.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            post.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            JSONObject json = new JSONObject();
            json.put("id", id);
            JSONObject base = new JSONObject();
            base.put(collection, json);

            Log.v("talon_tweetmarker", base.toString());

            Log.v("talon_tweetmarker", "sending");

            post.setEntity(new ByteArrayEntity(base.toString().getBytes("UTF8")));
            DefaultHttpClient client = new DefaultHttpClient();//forceVerification(new DefaultHttpClient());

            HttpResponse response = client.execute(post);
            Log.v("talon_tweetmarker", "response code: " + response.getStatusLine().getStatusCode());
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = rd.readLine()) != null) {
                Log.v("talon_tweetmarker", line);
                builder.append(line);
            }

            JSONObject jsonObject = new JSONObject(builder.toString());
            JSONObject timeline = jsonObject.getJSONObject(collection);
            if (timeline != null) {
                long val = timeline.getLong("id");
                Log.v("talon_tweetmarker", "id: " + val);
                return val;
            } else {
                return 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getLastStatus(String collection, int lastVersion) {
        try {
            HttpGet get = new HttpGet(postURL + "&" + collection);
            get.addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER);
            get.addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter));

            Log.v("talon_tweetmarker", "getting tweetmarker");

            HttpClient client = new DefaultHttpClient();

            HttpResponse response = client.execute(get);
            Log.v("talon_tweetmarker", "response code: " + response.getStatusLine().getStatusCode());
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) { // request ok
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    Log.v("talon_tweetmarker", line);
                    builder.append(line);
                }

                JSONObject jsonObject = new JSONObject(builder.toString());
                JSONObject timeline = jsonObject.getJSONObject(collection);

                if (timeline != null) {

                    long val = timeline.getLong("id");
                    int version = timeline.getInt("version");
                    Log.v("talon_tweetmarker", "version: " + version);
                    Log.v("talon_tweetmarker", "id: " + val);

                    if (version != lastVersion) {
                        // don't want to move the timeline if the version is the same
                        return val;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public DefaultHttpClient forceVerification(HttpClient httpclient) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }
}
