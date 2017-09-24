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
import android.os.Handler;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.RateItDialog;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
        }
    }

    public static void showRateItDialog(final Context context, final SharedPreferences sharedPreferences) {
        sharedPreferences.edit().putBoolean("show_rate_it", false).apply();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                context.startActivity(new Intent(context, RateItDialog.class));
            }
        }, 500);
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
