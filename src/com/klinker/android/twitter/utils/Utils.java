package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.services.TrimDataService;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.Date;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Utils {

    public static Twitter getTwitter(Context context, AppSettings settings) {
        if (settings == null) {
            settings = AppSettings.getInstance(context);
        }
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(settings.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(settings.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(settings.authenticationToken)
                .setOAuthAccessTokenSecret(settings.authenticationTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    public static TwitterStream getStreamingTwitter(Context context, AppSettings settings) {
        settings = AppSettings.getInstance(context);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(settings.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(settings.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(settings.authenticationToken)
                .setOAuthAccessTokenSecret(settings.authenticationTokenSecret);
        TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
        return tf.getInstance();
    }

    public static Twitter getSecondTwitter(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(AppSettings.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(AppSettings.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(settings.secondAuthToken)
                .setOAuthAccessTokenSecret(settings.secondAuthTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    public static String getTimeAgo(long time, Context context) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = getCurrentTime();
        if (time > now || time <= 0) {
            return null;
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return context.getResources().getString(R.string.just_now);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return context.getResources().getString(R.string.a_min_ago);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return (context.getResources().getString(R.string.minutes_ago)).replace("%s", diff / MINUTE_MILLIS + "");
        } else if (diff < 90 * MINUTE_MILLIS) {
            return context.getResources().getString(R.string.an_hour_ago);
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1)
                return context.getResources().getString(R.string.an_hour_ago);
            else
                return (context.getResources().getString(R.string.hours_ago)).replace("%s", diff / HOUR_MILLIS + "");
        } else if (diff < 48 * HOUR_MILLIS) {
            return context.getResources().getString(R.string.yesterday);
        } else {
            return (context.getResources().getString(R.string.days_ago)).replace("%s", diff / DAY_MILLIS + "");
        }
    }

    private static long getCurrentTime() {
        return new Date().getTime();
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static int getNavBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static boolean hasNavBar(Context context) {
        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        Point realSize = new Point();
        display.getSize(size);
        display.getRealSize(realSize);

        return Math.max(size.x, size.y) < Math.max(realSize.x, realSize.y) || (context.getResources().getBoolean(R.bool.isTablet) && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    // true if on mobile data
    // false otherwise
    public static boolean getConnectionStatus(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return false;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return true;
        }

        return false;
    }

    public static boolean hasInternetConnection(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        return isConnected;
    }

    public static int toDP(int px, Context context) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, context.getResources().getDisplayMetrics());
        } catch (Exception e) {
            return px;
        }
    }

    public static boolean isPackageInstalled(Context context, String targetPackage){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public static void setUpTheme(Context context, AppSettings settings) {
        if (settings.layout == AppSettings.LAYOUT_TALON) {
            Log.v("talon_theme", "setting talon theme");
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    context.setTheme(R.style.Theme_TalonLight);
                    break;
                case AppSettings.THEME_DARK:
                    context.setTheme(R.style.Theme_TalonDark);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonBlack);
                    break;
            }
        } else {
            Log.v("talon_theme", "setting talon theme");
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    context.setTheme(R.style.Theme_TalonLight_Hangouts);
                    break;
                case AppSettings.THEME_DARK:
                    context.setTheme(R.style.Theme_TalonDark_Hangouts);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonBlack_Hangouts);
                    break;
            }
        }
    }

    public static void setUpPopupTheme(Context context, AppSettings settings) {
        if (settings.layout == AppSettings.LAYOUT_TALON) {
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    context.setTheme(R.style.Theme_TalonLight_Popup);
                    break;
                case AppSettings.THEME_DARK:
                    context.setTheme(R.style.Theme_TalonDark_Popup);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonBlack_Popup);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    context.setTheme(R.style.Theme_TalonLight_Hangouts_Popup);
                    break;
                case AppSettings.THEME_DARK:
                    context.setTheme(R.style.Theme_TalonDark_Hangouts_Popup);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonBlack_Hangouts_Popup);
                    break;
            }
        }
    }

    public static void setUpNotifTheme(Context context, AppSettings settings) {
        if (settings.layout == AppSettings.LAYOUT_TALON) {
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Notif);
                    break;
                case AppSettings.THEME_DARK:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Notif);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonBlack_Popup_Notif);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_LIGHT:
                    context.setTheme(R.style.Theme_TalonLight_Hangouts_Popup_Notif);
                    break;
                case AppSettings.THEME_DARK:
                    context.setTheme(R.style.Theme_TalonDark_Hangouts_Popup_Notif);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonBlack_Hangouts_Popup_Notif);
                    break;
            }
        }
    }

    public static void newDMRefresh(final Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Refresh Direct Messages")
                .setMessage("With this version comes a new system to interact with Direct Messages. This system needs some setup though. It will connect to data, press OK to continue")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new RefreshDM(context).execute();
                        dialogInterface.dismiss();
                    }
                })
                .create()
                .show();
    }

    public static void needCleanTimeline(final Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("need_clean_databases_version_1_3_0", false).commit();
        new AlertDialog.Builder(context)
                .setTitle("Tip: Speed up the timeline")
                .setMessage("Never slow down. Cleaning and speeding up Talon is easy! Check out the \"Clean Databases\" option under advanced settings to get all the speed you want!\n\n" +
                        "Click the \"Clean Now!\" option to preform this action now!")
                .setPositiveButton("Clean Now!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new CleanDatabases(context).execute();
                        dialogInterface.dismiss();

                    }
                })
                .create()
                .show();

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long now = new Date().getTime();
        long alarm = now + AlarmManager.INTERVAL_DAY;

        Log.v("alarm_date", "auto trim " + new Date(alarm).toString());

        PendingIntent pendingIntent = PendingIntent.getService(context, 161, new Intent(context, TrimDataService.class), 0);

        am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);
    }

    static class RefreshDM extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        Context context;
        SharedPreferences sharedPrefs;

        public RefreshDM(Context context) {
            this.context = context;
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Getting direct messages...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {

            IOUtils.trimDatabase(context, sharedPrefs.getInt("current_account", 1));

            DMDataSource data = new DMDataSource(context);
            data.open();
            data.deleteAllTweets(1);
            data.deleteAllTweets(2);

            sharedPrefs.edit().putLong("last_direct_message_id_1", 0).commit();
            sharedPrefs.edit().putLong("last_direct_message_id_2", 0).commit();

            try {
                Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));

                Paging paging = new Paging(1, 100);

                List<DirectMessage> dm = twitter.getDirectMessages(paging);
                boolean id = false;
                try {
                    sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), dm.get(0).getId()).commit();
                    id = true;
                } catch (Exception e) {
                    // no received messages
                }

                for (DirectMessage directMessage : dm) {
                    try {
                        data.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                    } catch (Exception e) {
                        break;
                    }
                }

                List<DirectMessage> sent = twitter.getSentDirectMessages();

                try {
                    if (!id) {
                        sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), dm.get(0).getId()).commit();
                    }
                } catch (Exception e) {
                    // no received messages
                }

                for (DirectMessage directMessage : sent) {
                    try {
                        data.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                    } catch (Exception e) {
                        break;
                    }
                }

                data.close();
                return true;

            } catch (Exception e) {
                // they have no direct messages
                return true;
            }

        }

        protected void onPostExecute(Boolean deleted) {
            try {
                pDialog.dismiss();
                Toast.makeText(context, "Done", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // they closed it so the dialog wasn't attached
            }

            sharedPrefs.edit().putBoolean("need_new_dm", false).commit();
        }
    }

    static class CleanDatabases extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;
        Context context;
        SharedPreferences sharedPrefs;

        public CleanDatabases(Context context) {
            this.context = context;
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Cleaning up...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {

            try {
                IOUtils.trimDatabase(context, sharedPrefs.getInt("current_account", 1));
                return true;
            } catch (Exception e) {
                return false;
            }

        }

        protected void onPostExecute(Boolean deleted) {
            try {
                pDialog.dismiss();
                Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                // they closed it so the dialog wasn't attached
            }

            sharedPrefs.edit().putBoolean("need_clean_databases", false).commit();
        }
    }
}
