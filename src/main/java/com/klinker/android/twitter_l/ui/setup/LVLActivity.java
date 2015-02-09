package com.klinker.android.twitter_l.ui.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.vending.licensing.*;
import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.utils.Utils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class LVLActivity extends Activity {

    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;

    public boolean licenced = false;
    public boolean isCheckComplete = false;

    public final static String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl0N0d4hUOVmXfBj468w3dylIN44rK19R0zxHZLDG6CHaBFF8qAYbIX2BoC0br/ET5JUVS4HTmk55lCCFb/t49Me7b5ywz2H+vZe2bmuYYTfNR2kUpxKaZM3Q3UdF+rvmIGypza5MVWcq7Q0qEeFc8efaPR1TyxDkmlSr8FVW/A/4QiDYva0vYtRLr4MBqUXaoAyobe9TTnDdeP45qFT5DBytZN+PtFdy7556OhUVv/DVvwQa5WPnPymrYfTQl6pwNlGmGjlDAf8nkXMOMPEQeMhWkpiE3rT/URZ4B/vd5sSlLMSXKlG5l9sOC5Uxz7TzuOsP/Z4pKJcsRF5432MS4wIDAQAB";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLicenseCheckerCallback = new MyLicenseCheckerCallback();

        mChecker = new LicenseChecker(
                this, new StrictPolicy(),
                BASE64_PUBLIC_KEY  // Your public licensing key.
        );

        doCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChecker.onDestroy();
    }

    protected void doCheck() {
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private boolean checkLuckyPatcher() {
        if (Utils.isPackageInstalled(this, "com.dimonvideo.luckypatcher")) {
            return true;
        }

        if (Utils.isPackageInstalled(this, "com.chelpus.lackypatch")) {
            return true;
        }

        return false;
    }

    protected boolean isDebuggable(Context context) {
        if ((context.getApplicationContext().getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE) != 0)  {
            return true;
        } else {
            return false;
        }
    }

    private static final String SAVE_USER = "https://omega-jet-799.appspot.com/_ah/api/license/v1/saveUser/";

    public String getUserUrl() {
        try {
            Signature[] signatures =
                    getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures;

            String sig = signatures[0].toCharsString();

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(
                    SAVE_USER + sig
            );

            HttpResponse response = client.execute(post);

            int statusCode = response.getStatusLine().getStatusCode();
            StringBuilder builder = new StringBuilder();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                return null;
            }

            String json = builder.toString();

            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray("items");
            if (array.length() == 2) {
                String url = (String) array.get(0);
                String key = (String) array.get(1);

                SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

                sharedPrefs.edit().putString("consumer_key_2", key).commit();

                return url;
            } else {
                return null;
            }

        } catch (Exception ex) { }

        return null;
    }

    public void licenseTimeout() {
        new AlertDialog.Builder(this)
                .setTitle("Failed to connect")
                .setMessage("Connection timed out while setting up the connection with Twitter. Do you have a good internet connection?")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .create()
                .show();
    }

    public void notLicenced() {
        new AlertDialog.Builder(this)
                .setTitle("Twitter Error")
                .setMessage("Failed to sign into Twitter. (Code: 112)")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                })
                .setNegativeButton("Info", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri weburi = Uri.parse("https://plus.google.com/117432358268488452276/posts/aiQgceLKXiK");
                        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                        launchBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchBrowser);
                    }
                })
                .create()
                .show();
    }

    public void countUser(final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String URL = "https://omega-jet-799.appspot.com/_ah/api/license/v1/addLicensedUser/";

                    HttpClient client = new DefaultHttpClient();
                    HttpPost post = new HttpPost(
                            URL + java.net.URLEncoder.encode(name, "UTF-8")
                    );

                    client.execute(post);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        private boolean checkedOnce = false;

        public void allow(int reason) {
            isCheckComplete = true;

            if (isFinishing()) {
                return;
            }

            licenced = true;
        }

        public void dontAllow(int reason) {

            if (isFinishing()) {
                return;
            }

            if (reason == Policy.RETRY) {
                if (!checkedOnce) {
                    checkedOnce = true;
                    doCheck();
                } else {
                    isCheckComplete = true;
                    licenced = false;
                }
            } else {
                isCheckComplete = true;
                licenced = false;
            }
        }

        @Override
        public void applicationError(int errorCode) {
            if (!checkedOnce) {
                checkedOnce = true;
                doCheck();
            } else {
                isCheckComplete = true;
                licenced = false;
            }
        }
    }
}
