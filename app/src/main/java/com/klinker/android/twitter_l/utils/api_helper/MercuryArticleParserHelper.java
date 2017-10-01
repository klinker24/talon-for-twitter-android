package com.klinker.android.twitter_l.utils.api_helper;

import android.os.AsyncTask;
import android.util.Log;

import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.data.WebPreview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

public class MercuryArticleParserHelper {

    public interface Callback {
        void onResponse(WebPreview webPreview);
    }

    public static void getArticle(String url, Callback callback) {
        new ParseLink(url, callback).execute();
    }

    private static class ParseLink extends AsyncTask<Void, Void, WebPreview> {

        private String url;
        private Callback callback;

        public ParseLink(String url, Callback callback) {
            this.url = url;
            this.callback = callback;
        }

        @Override
        protected WebPreview doInBackground(Void... arg0) {
            try {
                // create the connection
                URL urlToRequest = new URL(buildMercuryUrl(url));
                HttpURLConnection urlConnection = buildAuthenticatedUrlConnection(urlToRequest);

                // create JSON object from content
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                JSONObject data = new JSONObject(getResponseText(in));

                String title = data.getString("title");
                String summary = data.getString("excerpt");
                String dek = data.getString("dek");
                String leadImage = data.getString("lead_image_url");
                String webDomain = data.getString("domain");

                return new WebPreview(title, summary != null ? summary : dek, leadImage, webDomain, url);
            } catch (Exception e) {
                return new WebPreview("", url, "", "", url);
            }
        }

        @Override
        protected void onPostExecute(WebPreview result) {
            if (callback != null) {
                callback.onResponse(result);
            }
        }

        private String buildMercuryUrl(String link) {
            return "https://mercury.postlight.com/parser?" +
                    "url=" + link;
        }

        private HttpsURLConnection buildAuthenticatedUrlConnection(URL url) throws IOException {
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("x-api-key", APIKeys.MERCURY_API_KEY);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            return urlConnection;
        }

        private String getResponseText(InputStream inStream) {
            return new Scanner(inStream).useDelimiter("\\A").next();
        }
    }

}
