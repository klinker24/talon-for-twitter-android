package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.Date;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

public class Utils {

    public static final String PACKAGE_NAME = "com.klinker.android.twitter_l";

    public static Twitter getTwitter(Context context, AppSettings settings) {
        if (settings == null) {
            settings = AppSettings.getInstance(context);
        }
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(AppSettings.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(AppSettings.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(settings.authenticationToken)
                .setOAuthAccessTokenSecret(settings.authenticationTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    public static TwitterStream getStreamingTwitter(Context context, AppSettings settings) {
        settings = AppSettings.getInstance(context);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(AppSettings.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(AppSettings.TWITTER_CONSUMER_SECRET)
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
            return 0 + "m";//context.getResources().getString(R.string.just_now);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return 1 + "m";//context.getResources().getString(R.string.a_min_ago);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + "m";//(context.getResources().getString(R.string.minutes_ago)).replace("%s", diff / MINUTE_MILLIS + "");
        } else if (diff < 90 * MINUTE_MILLIS) {
            return 1 + "h";//context.getResources().getString(R.string.an_hour_ago);
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1)
                return 1 + "h";//context.getResources().getString(R.string.an_hour_ago);
            else
                return diff / HOUR_MILLIS + "h";//(context.getResources().getString(R.string.hours_ago)).replace("%s", diff / HOUR_MILLIS + "");
        } else if (diff < 48 * HOUR_MILLIS) {
            return 1 + "d";//context.getResources().getString(R.string.yesterday);
        } else {
            return diff / DAY_MILLIS + "d";//(context.getResources().getString(R.string.days_ago)).replace("%s", diff / DAY_MILLIS + "");
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

    public static int getActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            Log.v("talon_actionbar", "getting size from dimen");
            return TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());
        } else {
            return 48;
        }
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

        try {
            return Math.max(size.x, size.y) < Math.max(realSize.x, realSize.y) || (context.getResources().getBoolean(R.bool.isTablet) && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        } catch (Exception e) {
            return false;
        }
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

    public static int toPx(int dp, Context context) {
        Resources r = context.getResources();
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        } catch (Exception e) {
            return dp;
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
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Yellow);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonDark_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonDark_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonDark_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonDark_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonDark_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonDark_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonDark_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonDark_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonDark_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonDark_Yellow);
                    break;
            }
        }
    }

    public static void setUpMainTheme(Context context, AppSettings settings) {
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Main_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Main_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Main_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Main_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Main_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Main_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Main_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Main_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Main_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Main_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Main_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Main_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Main_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Main_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Main_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Main_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Main_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Main_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Main_Yellow);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonDark_Main_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Main_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Main_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonDark_Main_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonDark_Main_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Main_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Main_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Main_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Main_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonDark_Main_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Main_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Main_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonDark_Main_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonDark_Main_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Main_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Main_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonDark_Main_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonDark_Main_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonDark_Main_Yellow);
                    break;
            }
        }
    }

    public static void setUpTweetTheme(Context context, AppSettings settings) {
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Yellow);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonDark_Tweet_Yellow);
                    break;
            }
        }
    }

    public static void setUpPopupTheme(Context context, AppSettings settings) {
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Popup_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Yellow);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Popup_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Popup_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Popup_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Popup_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonDark_Popup_Yellow);
                    break;
            }
        }
    }

    public static void setUpNotifTheme(Context context, AppSettings settings) {
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Notif_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Notif_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Notif_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Notif_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Notif_Yellow);
                    break;
            }
        } else {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Notif_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Notif_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Notif_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonDark_Notif_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Green);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonDark_Notif_Yellow);
                    break;
            }
        }
    }

    public static void setActionBar(Context context) {
        AppSettings settings = AppSettings.getInstance(context);
        if (settings.actionBar != null) {
            //Drawable back = settings.actionBar;
            try {
                ((Activity) context).getActionBar().setBackgroundDrawable(settings.actionBar);
            } catch (Exception e) {
                // on the compose there isnt an action bar
            }
        }

        // we will only do this if it is specified with the function below
        //setWallpaper(settings, context);
    }

    public static void setActionBar(Context context, boolean setWallpaper) {
        setActionBar(context);

        if (setWallpaper) {
            setWallpaper(AppSettings.getInstance(context), context);
        }
    }

    protected static void setWallpaper(AppSettings settings, Context context) {
        if (settings.addonTheme) {
            if (settings.customBackground != null) {
                Log.v("custom_background", "attempting to set custom background");
                try {
                    //Drawable background = settings.customBackground;
                    ((Activity)context).getWindow().setBackgroundDrawable(settings.customBackground);
                } catch (Throwable e) {
                    e.printStackTrace();
                    Log.v("custom_background", "error setting custom background");
                }
            } else if (settings.customBackground == null) {
                ((Activity)context).getWindow().setBackgroundDrawable(new ColorDrawable(settings.backgroundColor));
            }
        } else {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
            int resource = a.getResourceId(0, 0);
            a.recycle();

            ((Activity)context).getWindow().getDecorView().setBackgroundResource(resource);
        }
    }
}
