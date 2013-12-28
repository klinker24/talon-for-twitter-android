package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.TypedValue;
import android.view.Display;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.Date;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created with IntelliJ IDEA.
 * User: luke
 * Date: 11/9/13
 * Time: 2:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class Utils {

    public static Twitter getTwitter(Context context) {
        AppSettings settings = new AppSettings(context);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(settings.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(settings.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(settings.authenticationToken)
                .setOAuthAccessTokenSecret(settings.authenticationTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    public static Twitter getSecondTwitter(Context context) {
        AppSettings settings = new AppSettings(context);
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


    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = getCurrentTime();
        if (time > now || time <= 0) {
            return null;
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1)
                return "an hour ago";
            else
                return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
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
}
