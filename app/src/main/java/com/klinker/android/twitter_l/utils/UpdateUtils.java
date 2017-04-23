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

import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.Policy;
import com.google.android.vending.licensing.StrictPolicy;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.services.ActivityRefreshService;
import com.klinker.android.twitter_l.services.DataCheckService;
import com.klinker.android.twitter_l.services.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.ListRefreshService;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.TimelineRefreshService;
import com.klinker.android.twitter_l.services.TrimDataService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.setup.LVLActivity;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import twitter4j.User;


public class UpdateUtils {

    private static final long SEC = 1000;
    private static final long MIN = 60 * SEC;
    private static final long HOUR = 60 * MIN;
    private static final long DAY = 24 * HOUR;
    private static final long RATE_IT_TIMEOUT = 2 * DAY;

    private static final long SUPPORTER_TIMEOUT = 90 * DAY;

    public static void checkUpdate(final Context context) {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        long rateItShown = sharedPrefs.getLong("rate_it_last_shown", 0l);
        long currentTime = Calendar.getInstance().getTimeInMillis();

        if (rateItShown != 0l && currentTime - rateItShown > RATE_IT_TIMEOUT && sharedPrefs.getBoolean("show_rate_it", true)) {
            // show dialog
            showRateItDialog(context, sharedPrefs);
            sharedPrefs.edit().putLong("rate_it_last_shown", currentTime).apply();
        } if (rateItShown == 0l) {
            sharedPrefs.edit().putLong("rate_it_last_shown", currentTime).apply();
        }

        boolean justInstalled = runFirstInstalled(sharedPrefs);

        if (!justInstalled) {
            // version specific things
            if (sharedPrefs.getBoolean("need_mute_fix", true)) {
                String current = sharedPrefs.getString("muted_hashtags", "");
                String newString = current.replaceAll("  ", " ");
                sharedPrefs.edit().putString("muted_hashtags", newString)
                        .putBoolean("need_mute_fix", false)
                        .commit();
            }

            if (sharedPrefs.getBoolean("need_queue_deleted", true)) {
                sharedPrefs.edit().putBoolean("need_queue_deleted", false).commit();
                QueuedDataSource.getInstance(context).deleteAllQueuedTweets();
            }
        }

        sharedPrefs = AppSettings.getInstance(context).sharedPrefs;
        runEveryUpdate(context, sharedPrefs);
    }

    public static boolean showSupporterDialog(Context context) {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


        // if there is a time set for the first run (This was introduced with 4.0.0)
        // and it has been longer than 90 days
        if (sharedPrefs.getLong("first_run_time", 0) != 0 &&
                new Date().getTime() - sharedPrefs.getLong("first_run_time", 0) > SUPPORTER_TIMEOUT) {
            // we want to show them the supporter dialog if they haven't seen it
            if (!sharedPrefs.getBoolean("seen_supporter_dialog", false)) {
                sharedPrefs.edit().putBoolean("seen_supporter_dialog", true).apply();
                return true;
            }
        } else if (sharedPrefs.getLong("first_run_time", 0) == 0 &&
                !sharedPrefs.getBoolean("seen_supporter_dialog", false)) {
            // if there is not a time set for the first run
            // and they have not seen the dialog
            sharedPrefs.edit().putBoolean("seen_supporter_dialog", true).apply();
            return true;
        }

        return false;
    }

    public static boolean runFirstInstalled(final SharedPreferences sharedPrefs) {
        if (sharedPrefs.getBoolean("fresh_install", true)) {
            SharedPreferences.Editor e = sharedPrefs.edit();
            e.putBoolean("fresh_install", false);
            e.putLong("first_run_time", new Date().getTime());

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

            e.putLong("original_activity_refresh_" + 1, Calendar.getInstance().getTimeInMillis());
            e.putLong("original_activity_refresh_" + 2, Calendar.getInstance().getTimeInMillis());

            e.apply();

            return true;
        } else {
            return false;
        }
    }

    public static void runEveryUpdate(final Context context, final SharedPreferences sharedPrefs) {

        ServiceUtils.rescheduleAllServices(context);

        int storedAppVersion = sharedPrefs.getInt("app_version", 0);
        int currentAppVersion = getAppVersion(context);

        if (storedAppVersion != currentAppVersion && Utils.hasInternetConnection(context)) {
            sharedPrefs.edit().putInt("app_version", currentAppVersion).apply();

            new TimeoutThread(new Runnable() {
                @Override
                public void run() {
                    checkLicense(context);
                }
            }).start();
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

                            sharedPreferences.edit().putBoolean("show_rate_it", false).apply();
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
                                "The best design, the best features. Enhance Twitter experience with @TalonAndroid!\n\n" +
                                "http://talon.klinkerapps.com/");

                        context.startActivity(share);

                        sharedPreferences.edit().putBoolean("show_rate_it", false).apply();
                    }
                })
                .setNeutralButton(R.string.ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPreferences.edit().putBoolean("show_rate_it", false).apply();
                    }
                })
                .create().show();
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
                }
            } else if(reason == Policy.NOT_LICENSED) {
                showError();
            }
        }

        @Override
        public void applicationError(int errorCode) {
            if (!checkedOnce) {
                checkedOnce = true;
                checker.checkAccess(this);
            }
        }

        public void showError() {
            final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

            new TimeoutThread(new Runnable() {
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
                            e.apply();

                            android.os.Process.killProcess(android.os.Process.myPid());
                        }
                    })
                    .create()
                    .show();
        }
    }

    protected static int getAppVersion(Context c) {
        try {
            PackageInfo packageInfo = c.getPackageManager()
                    .getPackageInfo(c.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            return -1;
        }
    }
}
