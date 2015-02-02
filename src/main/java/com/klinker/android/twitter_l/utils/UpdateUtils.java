package com.klinker.android.twitter_l.utils;
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
import android.app.ProgressDialog;
import android.content.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.StrictPolicy;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.setup.LVLActivity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;


public class UpdateUtils {

    private static final long SEC = 1000;
    private static final long MIN = 60 * SEC;
    private static final long HOUR = 60 * MIN;
    private static final long DAY = 24 * HOUR;
    private static final long RATE_IT_TIMEOUT = 2 * DAY;

    public static void checkUpdate(final Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        long rateItShown = sharedPrefs.getLong("rate_it_last_shown", 0l);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        sharedPrefs.edit().putLong("rate_it_last_shown", currentTime).commit();

        if (rateItShown != 0l && currentTime - rateItShown > RATE_IT_TIMEOUT && sharedPrefs.getBoolean("show_rate_it", true)) {
            // show dialog
            showRateItDialog(context, sharedPrefs);
        }

        if (sharedPrefs.getBoolean("3.1.5", true)) {
            sharedPrefs.edit().putBoolean("3.1.5", false).commit();

            // want to make sure if tweetmarker was on, it remains on.
            if (sharedPrefs.getBoolean("tweetmarker", false)) {
                sharedPrefs.edit().putString("tweetmarker_options", "1").commit();
                AppSettings.invalidate();
            }
        }

        if (sharedPrefs.getBoolean("2.0.0", true)) {
            SharedPreferences.Editor e = sharedPrefs.edit();
            e.putBoolean("2.0.0", false);

            // show them all for now
            Set<String> set = new HashSet<String>();
            set.add("0"); // activity
            set.add("1"); // timeline
            set.add("2"); // mentions
            set.add("3"); // dm's
            set.add("4"); // discover
            set.add("5"); // lists
            set.add("6"); // favorite users
            set.add("7"); // retweets
            set.add("8"); // favorite Tweets
            set.add("9"); // saved searches

            e.putStringSet("drawer_elements_shown_1", set);
            e.putStringSet("drawer_elements_shown_2", set);

            // reset their pages to just home,
            String pageIdentifier = "account_" + 1 + "_page_";
            e.putInt(pageIdentifier + 1, AppSettings.PAGE_TYPE_ACTIVITY);
            e.putInt(pageIdentifier + 2, AppSettings.PAGE_TYPE_HOME);
            e.putInt(pageIdentifier + 3, AppSettings.PAGE_TYPE_MENTIONS);
            e.putInt(pageIdentifier + 4, AppSettings.PAGE_TYPE_DMS);

            pageIdentifier = "account_" + 2 + "_page_";
            e.putInt(pageIdentifier + 1, AppSettings.PAGE_TYPE_ACTIVITY);
            e.putInt(pageIdentifier + 2, AppSettings.PAGE_TYPE_HOME);
            e.putInt(pageIdentifier + 3, AppSettings.PAGE_TYPE_MENTIONS);
            e.putInt(pageIdentifier + 4, AppSettings.PAGE_TYPE_DMS);

            e.putInt("default_timeline_page_" + 1, 1);
            e.putInt("default_timeline_page_" + 2, 1);

            e.commit();


            new Thread(new Runnable() {
                @Override
                public void run() {
                    checkLicense(context);
                }
            }).start();
        }

        if (sharedPrefs.getBoolean("version_1.3.0", false)) {
            SharedPreferences.Editor e = sharedPrefs.edit();

            e.putBoolean("version_1.3.0", true);
            e.putInt("material_theme_1", AppSettings.getInstance(context).theme);
            e.putInt("material_theme_2", AppSettings.getInstance(context).theme);

            e.commit();
        }
    }

    public static void showRateItDialog(final Context context, final SharedPreferences sharedPreferences) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.enjoying_talon)
                .setMessage(R.string.give_a_rating)
                .setPositiveButton(R.string.rate_on_rating_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

                        try {
                            context.startActivity(goToMarket);

                            sharedPreferences.edit().putBoolean("show_rate_it", false).commit();
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context, "Couldn't launch the market", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.share_on_rating_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("text/plain");
                        share.putExtra(Intent.EXTRA_TEXT,
                                "Have Android 5.0 Lollipop? Get the most out of your Twitter experience with Talon for Twitter!\n\n" +
                                "http://klinkerapps.com/get-talon-plus");

                        context.startActivity(share);

                        sharedPreferences.edit().putBoolean("show_rate_it", false).commit();
                    }
                })
                .setNeutralButton(R.string.ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putBoolean("show_rate_it", false).commit();
                    }
                })
                .create().show();
    }

    public static void updateToGlobalPrefs(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Settings Update")
                .setMessage("Talon has to update your settings preferences to prepare for some new things. This will override any old settings backups.")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new WriteGlobalSharedPrefs(context).execute();
                    }
                })
                .create()
                .show();
    }

    static class WriteGlobalSharedPrefs extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        Context context;

        public WriteGlobalSharedPrefs(Context context) {
            this.context = context;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Saving...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            File des = new File(Environment.getExternalStorageDirectory() + "/Talon/backup.prefs");
            IOUtils.saveSharedPreferencesToFile(des, context);
            IOUtils.loadSharedPreferencesFromFile(des, context);

            return true;
        }

        protected void onPostExecute(Boolean deleted) {
            try {
                pDialog.dismiss();
                Toast.makeText(context, "Save Complete", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // not attached
            }

            SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            sharedPrefs.edit().putBoolean("version_2_2_7_1", false).commit();

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("version_2_2_7_1", false).commit();

            ((Activity)context).finish();
            context.startActivity(new Intent(context, MainActivity.class));
            ((Activity)context).overridePendingTransition(0,0);
        }
    }

    public static void versionThreeDialog(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Blur - A Launcher Replacement")
                .setMessage("With version 3.0.0, Talon has added support for our latest project, Blur, which is Klinker Apps launcher.\n\n" +
                        "It does some very cool interfacing with Talon, basically having the full app just one swipe away on your launcher. It has been a great project to work on and I recommend checking it out, it is completely free!\n\n" +
                        "Head over to the Play Store description for Blur to learn more about getting Talon compatible (it is just downloading one extension app).\n\n" +
                        "Hope you like it!")
                .setPositiveButton("Go to Blur!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent web = new Intent(Intent.ACTION_VIEW);
                        web.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.launcher"));
                        context.startActivity(web);
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    public static void checkLicense(final Context context) {
        LicenseChecker mChecker = new LicenseChecker(
                context, new StrictPolicy(),
                LVLActivity.BASE64_PUBLIC_KEY  // Your public licensing key.
        );

        LicenseCheckerCallback mLicenseCheckerCallback =
                new MyLicenseCheckerCallback(context, mChecker);

        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    protected static class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        private boolean checkedOnce = false;
        private Context context;
        private LicenseChecker checker;

        public MyLicenseCheckerCallback(Context c, LicenseChecker checker) {
            this.context = c;
            this.checker = checker;
        }

        public void allow(int reason) {
            // just won't do anything
        }

        public void dontAllow(int reason) {

            if (reason == Policy.RETRY) {
                if (!checkedOnce) {
                    checkedOnce = true;
                    checker.checkAccess(this);
                } else {
                    showError();
                }
            } else {
                showError();
            }
        }

        @Override
        public void applicationError(int errorCode) {
            if (!checkedOnce) {
                checkedOnce = true;
                checker.checkAccess(this);
            } else {
                showError();
            }
        }

        public void showError() {
            final SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AppSettings settings = new AppSettings(context);
                        final String URL = "https://omega-jet-799.appspot.com/_ah/api/license/v1/addUnlicensedUser/";

                        if (!TextUtils.isEmpty(settings.myScreenName)) {
                            HttpClient client = new DefaultHttpClient();
                            HttpPost post = new HttpPost(
                                    URL + java.net.URLEncoder.encode(settings.myScreenName, "UTF-8")
                            );

                            client.execute(post);
                        }

                        if (!TextUtils.isEmpty(settings.secondScreenName)) {
                            HttpClient client = new DefaultHttpClient();
                            HttpPost post = new HttpPost(
                                    URL +java.net.URLEncoder.encode(settings.secondScreenName, "UTF-8")
                            );

                            client.execute(post);
                        }

                    } catch (Exception e) {

                    }
                }
            }).start();

            new AlertDialog.Builder(context)
                    .setTitle("License Check Failed")
                    .setMessage("Please go to the Play Store to purchase this app. It is not free.")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            SharedPreferences.Editor e = sharedPrefs.edit();
                            e.putBoolean("is_logged_in_1", false);
                            e.putBoolean("is_logged_in_2", false);
                            e.commit();

                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .create()
                    .show();
        }
    }


}
