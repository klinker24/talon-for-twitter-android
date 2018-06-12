package com.klinker.android.twitter_l.utils.api_helper;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import twitter4j.Twitter;

public class TweetMarkerHelper extends APIHelper {

    public static final String TWEETMARKER_API_KEY = APIKeys.TWEETMARKER_API_KEY;

    private int currentAccount;
    private String screenname;
    private String postURL;
    private Twitter twitter;
    private Context context;
    private SharedPreferences sharedPrefs;

    public TweetMarkerHelper(int currentAccount, String screenname, Twitter twitter, SharedPreferences sharedPrefs, Context c) {
        this.currentAccount = currentAccount;
        this.screenname = screenname;
        this.twitter = twitter;
        this.sharedPrefs = sharedPrefs;
        this.context = c;

        postURL = "https://api.tweetmarker.net/v2/lastread?api_key=" + Uri.encode(TWEETMARKER_API_KEY) +
                "&username=" + Uri.encode(screenname);
    }

    public boolean contentProvider = false;
    public void setUseContentProvider(boolean use) {
        contentProvider = use;
    }

    public boolean sendCurrentId(String collection, long id) {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            JSONObject base = new JSONObject();
            base.put(collection, json);

            Request request = new Request.Builder()
                    .url(postURL)
                    .addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER)
                    .addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter, context))
                    .post(RequestBody.create(MediaType.parse("application/json;  charset=utf-8"), base.toString().getBytes("UTF8")))
                    .build();

            Log.v("talon_tweetmarker", "sending " + id + " to " + screenname);

            Response response = new OkHttpClient().newCall(request).execute();
            int responseCode = response.code();

            Log.v("talon_tweetmarker", "sending response code: " + responseCode);
            Log.v("talon_tweetmarker", "sending response message: " + response.message());

            if (responseCode == 200) {
                int currentVersion = sharedPrefs.getInt("last_version_account_" + currentAccount, 0);
                sharedPrefs.edit().putInt("last_version_account_" + currentAccount, currentVersion + 1).apply();
                return true;
            } else {
                try {
                    Thread.sleep(1500);
                } catch (Exception e) {
                }

                Request retry = new Request.Builder()
                        .url(postURL)
                        .addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER)
                        .addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter, context))
                        .post(RequestBody.create(MediaType.parse("application/json;  charset=utf-8"), base.toString().getBytes("UTF8")))
                        .build();

                Log.v("talon_tweetmarker", "retry sending " + id + " to " + screenname);

                Response responseRetry = new OkHttpClient().newCall(retry).execute();
                int responseCodeRetry = responseRetry.code();

                Log.v("talon_tweetmarker", "retry sending response code: " + responseCodeRetry);
                Log.v("talon_tweetmarker", "retry sending response message: " + responseRetry.message());

                if (responseCodeRetry == 200) {
                    int currentVersion = sharedPrefs.getInt("last_version_account_" + currentAccount, 0);
                    sharedPrefs.edit().putInt("last_version_account_" + currentAccount, currentVersion + 1).apply();
                    return true;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean getLastStatus(String collection, final Context context) {

        long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0l);

        boolean updated = false;

        try {
            long startTime = Calendar.getInstance().getTimeInMillis();

            Request request = new Request.Builder()
                    .url(postURL + "&" + collection)
                    .addHeader("X-Auth-Service-Provider", SERVICE_PROVIDER)
                    .addHeader("X-Verify-Credentials-Authorization", getAuthrityHeader(twitter, context))
                    .get().build();

            final Response response = new OkHttpClient().newCall(request).execute();
            final int responseCode = response.code();
            final String responseMessage = response.message();

            Log.v("talon_tweetmarker", "getting id response code: " + responseCode + " for " + screenname);
            Log.v("talon_tweetmarker", "response mess: " + responseMessage);

            long endTime = Calendar.getInstance().getTimeInMillis();
            final long responseTime = endTime - startTime;

            if (endTime - startTime > 15000 && responseCode == 200) {
                ((Activity) context).runOnUiThread(() -> {
                    try {
                        if (sharedPrefs.getBoolean("show_tweetmarker_length", true)) {
                            new AlertDialog.Builder(context)
                                    .setTitle("Slow TweetMarker Fetch")
                                    .setMessage("TweetMarker successfully fetched it's position, but seemed to take quite a bit of time. " +
                                            "They may be experiencing issues at the moment, you may want to try again in a little while! \n\n" +
                                            "Server Response Time: " + (responseTime / 1000) + " seconds")
                                    .setPositiveButton("Turn Off TM", (dialog, which) -> {
                                        sharedPrefs.edit().putString("tweetmarker_options", "0").apply();
                                        AppSettings.invalidate();
                                    })
                                    .setNeutralButton(R.string.dont_show_again, (dialog, which) -> {
                                        sharedPrefs.edit().putBoolean("show_tweetmarker_length", false).apply();
                                        dialog.dismiss();
                                    })
                                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                                    .create()
                                    .show();
                        }
                    } catch (Exception e) {

                    }
                });
            }

            if (responseCode == 500 || responseCode == 503) {
                // common tweetmarker failure codes

                ((Activity) context).runOnUiThread(() -> {
                    try {
                        new AlertDialog.Builder(context)
                                .setTitle("TweetMarker Failure")
                                .setMessage("Error: " + responseCode + "(" + responseMessage + ")" + "\n\n" +
                                        "TweetMarker has been experiencing some issues on their end lately with some apps. They seem intermittent, random, and are causing incredibly slow load times." +
                                        "I have been in contact with them, but I would recommend turning off this feature until these issues are resolved.")
                                .setPositiveButton("Turn Off TM", (dialog, which) -> {
                                    sharedPrefs.edit().putString("tweetmarker_options", "0").apply();
                                    AppSettings.invalidate();
                                })
                                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                                .create()
                                .show();
                    } catch (Exception e) {

                    }
                });

                updated = false;
            } else if (responseCode == 200) { // request ok
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.body().byteStream()));

                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    builder.append(line);
                }

                JSONObject jsonObject = new JSONObject(builder.toString());
                JSONObject timeline = jsonObject.getJSONObject(collection);

                if (timeline != null) {
                    currentId = Long.parseLong(timeline.getString("id"));
                    int version = Integer.parseInt(timeline.getString("version"));

                    Log.v("talon_tweetmarker", "getting tweetmarker," +
                            " version: " + version +
                            " id: " + currentId +
                            " screename: " + screenname);

                    int lastVer = sharedPrefs.getInt("last_version_account_" + currentAccount, 0);

                    if (lastVer != version) {
                        updated = true;
                    }

                    sharedPrefs.edit().putInt("last_version_account_" + currentAccount, version).apply();
                }
            }
        } catch (Exception e) {
            // timeout when connecting to host.
            ((Activity) context).runOnUiThread(() -> {
                try {
                    new AlertDialog.Builder(context)
                            .setTitle("TweetMarker Failure")
                            .setMessage("Timeout connecting to TweetMarker." + "\n\n" +
                                    "TweetMarker has been experiencing some issues on their end lately with some apps. They seem intermittent, random, and are causing incredibly slow load times." +
                                    "I have been in contact with them, but I would recommend turning off this feature until these issues are resolved.")
                            .setPositiveButton("Turn Off TM", (dialog, which) -> {
                                sharedPrefs.edit().putString("tweetmarker_options", "0").apply();
                                AppSettings.invalidate();
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } catch (Exception e1) {

                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

        Log.v("talon_launcher_stuff", "writing " + currentId + " to shared prefs");
        sharedPrefs.edit().putLong("current_position_" + currentAccount, currentId).apply();
        if (contentProvider) {
            HomeContentProvider.updateCurrent(currentAccount, context, currentId);
        } else {
            try {
                HomeDataSource.getInstance(context).markPosition(currentAccount, currentId);
            } catch (Exception e) {

            }
        }


        return updated;
    }
}
