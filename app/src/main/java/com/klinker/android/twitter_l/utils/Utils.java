package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.transition.ChangeTransform;
import android.transition.Transition;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Window;
import com.klinker.android.twitter_l.APIKeys;
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
        APIKeys keys = new APIKeys(context);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(keys.consumerKey)
                .setOAuthConsumerSecret(keys.consumerSecret)
                .setOAuthAccessToken(settings.authenticationToken)
                .setOAuthAccessTokenSecret(settings.authenticationTokenSecret);
        cb.setTweetModeExtended(true);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    public static TwitterStream getStreamingTwitter(Context context, AppSettings settings) {
        settings = AppSettings.getInstance(context);
        APIKeys keys = new APIKeys(context);

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(keys.consumerKey)
                .setOAuthConsumerSecret(keys.consumerSecret)
                .setOAuthAccessToken(settings.authenticationToken)
                .setOAuthAccessTokenSecret(settings.authenticationTokenSecret);
        cb.setTweetModeExtended(true);
        TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
        return tf.getInstance();
    }

    public static Twitter getSecondTwitter(Context context) {
        AppSettings settings = AppSettings.getInstance(context);

        final APIKeys keys;
        if (settings.currentAccount == 1) {
            keys = new APIKeys(context, 2);
        } else {
            keys = new APIKeys(context, 1);
        }

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(keys.consumerKey)
                .setOAuthConsumerSecret(keys.consumerSecret)
                .setOAuthAccessToken(settings.secondAuthToken)
                .setOAuthAccessTokenSecret(settings.secondAuthTokenSecret);
        cb.setTweetModeExtended(true);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    public static String getTimeAgo(long time, Context context, boolean allowLongFormat) {
        if (allowLongFormat && AppSettings.getInstance(context).revampedTweets()) {
            return getTimeAgoLongFormat(time, context);
        }

        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = getCurrentTime();
        if (time > now || time <= 0) {
            return "+1d";
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return diff / SECOND_MILLIS + "s";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return 1 + "m";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + "m";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return 1 + "h";
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1)
                return 1 + "h";
            else
                return diff / HOUR_MILLIS + "h";
        } else if (diff < 48 * HOUR_MILLIS) {
            return 1 + "d";
        } else {
            return diff / DAY_MILLIS + "d";
        }
    }

    private static String getTimeAgoLongFormat(long time, Context context) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = getCurrentTime();
        if (time > now || time <= 0) {
            return "+1d";
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return context.getString(R.string.seconds_ago, diff / SECOND_MILLIS);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return context.getString(R.string.min_ago, 1);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return context.getString(R.string.mins_ago, diff / MINUTE_MILLIS);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return context.getString(R.string.hour_ago, 1);
        } else if (diff < 24 * HOUR_MILLIS) {
            if (diff / HOUR_MILLIS == 1)
                return context.getString(R.string.hour_ago, 1);
            else
                return context.getString(R.string.new_hours_ago, diff / HOUR_MILLIS);
        } else if (diff < 48 * HOUR_MILLIS) {
            return context.getString(R.string.day_ago, 1);
        } else {
            return context.getString(R.string.new_days_ago, diff / DAY_MILLIS);
        }
    }

    private static char[] c = new char[]{'K', 'M', 'B', 'T'};
    public static String coolFormat(double n, int iteration) {
        double d = ((long) n / 100) / 10.0;
        boolean isRound = (d * 10) %10 == 0;//true if the decimal part is equal to 0 (then it's trimmed anyway)
        return (d < 1000? //this determines the class, i.e. 'k', 'm' etc
                ((d > 99.9 || isRound || (!isRound && d > 9.99)? //this decides whether to trim the decimals
                        (int) d * 10 / 10 : d + "" // (int) d * 10 / 10 drops the decimal
                ) + "" + c[iteration])
                : coolFormat(d, iteration+1));

    }

    public static String getTranslateURL(final String lang) {
        return "https://translate.google.com/m/translate#auto|" +
                lang +
                "|";
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
            int value = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                value += Utils.toDP(6, context);
            }

            return value;
        } else {
            return Utils.toDP(48, context);
        }
    }

    public static int getNavBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        } else if (hasNavBar(context)) {
            Utils.toDP(48, context);
        }

        return result;
    }

    public static boolean hasNavBar(Context context) {
        switch (AppSettings.getInstance(context).navBarOption) {
            case AppSettings.NAV_BAR_AUTOMATIC:
                boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
                boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);

                if (hasBackKey && hasHomeKey) {
                    // no navigation bar, unless it is enabled in the settings
                    Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    Point realSize = new Point();
                    display.getSize(size);
                    display.getRealSize(realSize);

                    if (Build.MANUFACTURER.toLowerCase().contains("samsung") && !(Build.MODEL.toLowerCase().contains("nexus") || Build.MODEL.toLowerCase().contains("s8"))) {
                        return false;
                    }

                    try {
                        return Math.max(size.x, size.y) < Math.max(realSize.x, realSize.y) || (context.getResources().getBoolean(R.bool.isTablet) && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
                    } catch (Exception e) {
                        Resources resources = context.getResources();
                        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
                        if (id > 0) {
                            return resources.getBoolean(id);
                        } else {
                            return false;
                        }
                    }
                } else {
                    return true;
                }
            case AppSettings.NAV_BAR_PRESENT:
                return true;
            case AppSettings.NAV_BAR_NONE:
                return false;
            default:
                return true;
        }

    }

    // true if on mobile data
    // false otherwise
    public static boolean getConnectionStatus(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (null != activeNetwork) {
                if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                    return false;

                if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                    return true;
            }
        } catch (Exception e) {

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

    public static void setSharedContentTransition(Context context, Transition trans) {
        Activity activity = (Activity) context;

        // inside your activity (if you did not enable transitions in your theme)
        try {
            activity.getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            activity.getWindow().setAllowEnterTransitionOverlap(true);
            activity.getWindow().setAllowReturnTransitionOverlap(true);
        } catch (Exception e) {

        }

    }

    public static void setSharedContentTransition(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setSharedContentTransition(context, new ChangeTransform());
        }
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
                    context.setTheme(R.style.Theme_TalonLight_LightGreen);
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
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonLight_Black);
                    break;
                case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                    context.setTheme(R.style.Theme_TalonLight_DarkBackgroundColor);
                    break;
                case AppSettings.THEME_WHITE:
                    context.setTheme(R.style.Theme_TalonLight_White);
                    break;
            }
        } else {
            if (!settings.blackTheme) {
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
                        context.setTheme(R.style.Theme_TalonDark_LightGreen);
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
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonDark_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonDark_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonDark_White);
                        break;
                }
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonBlack_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonBlack_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonBlack_White);
                        break;
                }
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
                    context.setTheme(R.style.Theme_TalonLight_Main_LightGreen);
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
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonLight_Main_Black);
                    break;
                case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                    context.setTheme(R.style.Theme_TalonLight_Main_DarkBackgroundColor);
                    break;
                case AppSettings.THEME_WHITE:
                    context.setTheme(R.style.Theme_TalonLight_Main_White);
                    break;
            }
        } else {
            if (!settings.blackTheme) {
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
                        context.setTheme(R.style.Theme_TalonDark_Main_LightGreen);
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
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonDark_Main_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonDark_Main_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonDark_Main_White);
                        break;
                }
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Main_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Main_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonBlack_Main_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonBlack_Main_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonBlack_Main_White);
                        break;
                }
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
                    context.setTheme(R.style.Theme_TalonLight_Tweet_LightGreen);
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
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_Black);
                    break;
                case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_DarkBackgroundColor);
                    break;
                case AppSettings.THEME_WHITE:
                    context.setTheme(R.style.Theme_TalonLight_Tweet_White);
                    break;
            }
        } else {
            if (!settings.blackTheme) {
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
                        context.setTheme(R.style.Theme_TalonDark_Tweet_LightGreen);
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
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonDark_Tweet_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonDark_Tweet_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonDark_Tweet_White);
                        break;
                }
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonBlack_Tweet_White);
                        break;
                }
            }
        }
    }

    public static void setUpProfileTheme(Context context, AppSettings settings) {
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Profile_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Profile_LightGreen);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Yellow);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonLight_Profile_Black);
                    break;
                case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                    context.setTheme(R.style.Theme_TalonLight_Profile_DarkBackgroundColor);
                    break;
                case AppSettings.THEME_WHITE:
                    context.setTheme(R.style.Theme_TalonLight_Profile_White);
                    break;
            }
        } else {
            if (!settings.blackTheme) {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonDark_Profile_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonDark_Profile_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonDark_Profile_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonDark_Profile_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonDark_Profile_White);
                        break;
                }
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonBlack_Profile_White);
                        break;
                }
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
                    context.setTheme(R.style.Theme_TalonLight_Popup_LightGreen);
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
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonLight_Popup_Black);
                    break;
                case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                    context.setTheme(R.style.Theme_TalonLight_Popup_DarkBackgroundColor);
                    break;
                case AppSettings.THEME_WHITE:
                    context.setTheme(R.style.Theme_TalonLight_Popup_White);
                    break;
            }
        } else {
            if (!settings.blackTheme) {
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
                        context.setTheme(R.style.Theme_TalonDark_Popup_LightGreen);
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
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonDark_Popup_Black);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonDark_Popup_White);
                        break;
                }
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonBlack_Popup_White);
                        break;
                }
            }
        }
    }

    public static void setUpSettingsTheme(Context context, AppSettings settings) {
        if (!settings.darkTheme) {
            switch (settings.theme) {
                case AppSettings.THEME_AMBER:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Amber);
                    break;
                case AppSettings.THEME_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Blue);
                    break;
                case AppSettings.THEME_BLUE_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Settings_BlueGrey);
                    break;
                case AppSettings.THEME_BROWN:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Brown);
                    break;
                case AppSettings.THEME_CYAN:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Cyan);
                    break;
                case AppSettings.THEME_DEEP_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_DeepOrange);
                    break;
                case AppSettings.THEME_DEEP_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_DeepPurple);
                    break;
                case AppSettings.THEME_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Green);
                    break;
                case AppSettings.THEME_GREY:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Grey);
                    break;
                case AppSettings.THEME_INDIGO:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Indigo);
                    break;
                case AppSettings.THEME_LIGHT_BLUE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_LightBlue);
                    break;
                case AppSettings.THEME_LIGHT_GREEN:
                    context.setTheme(R.style.Theme_TalonLight_Settings_LightGreen);
                    break;
                case AppSettings.THEME_LIME:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Lime);
                    break;
                case AppSettings.THEME_PINK:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Pink);
                    break;
                case AppSettings.THEME_PURPLE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Purple);
                    break;
                case AppSettings.THEME_ORANGE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Orange);
                    break;
                case AppSettings.THEME_TEAL:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Teal);
                    break;
                case AppSettings.THEME_RED:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Red);
                    break;
                case AppSettings.THEME_YELLOW:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Yellow);
                    break;
                case AppSettings.THEME_BLACK:
                    context.setTheme(R.style.Theme_TalonLight_Settings_Black);
                    break;
                case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                    context.setTheme(R.style.Theme_TalonLight_Settings_DarkBackgroundColor);
                    break;
                case AppSettings.THEME_WHITE:
                    context.setTheme(R.style.Theme_TalonLight_Settings_White);
                    break;
            }
        } else {
            if (!settings.blackTheme) {
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
                        context.setTheme(R.style.Theme_TalonDark_LightGreen);
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
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonDark_Settings_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonDark_Settings_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonDark_Settings_White);
                        break;
                }
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Yellow);
                        break;
                    case AppSettings.THEME_BLACK:
                        context.setTheme(R.style.Theme_TalonBlack_Settings_Black);
                        break;
                    case AppSettings.THEME_DARK_BACKGROUND_COLOR:
                        context.setTheme(R.style.Theme_TalonBlack_Settings_DarkBackgroundColor);
                        break;
                    case AppSettings.THEME_WHITE:
                        context.setTheme(R.style.Theme_TalonBlack_White);
                        break;
                }
            }
        }
    }

    public static String getBackgroundUrlForTheme(AppSettings settings) {
        switch (settings.theme) {
            case AppSettings.THEME_AMBER:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_amber.jpg?token=6d3e226366def6582b2bc2b1434e78fd89239235";
            case AppSettings.THEME_BLUE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_blue.jpg?token=e93db6f0d6b2ea7a925648754ee6d9d094a2ecee";
            case AppSettings.THEME_BLUE_GREY:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_blue_grey.jpg?token=1d2d3411235304e17630ae780c1cb8fc8a11c616";
            case AppSettings.THEME_BROWN:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_brown.jpg?token=0ffd54e00d5292f93e7102b568ca9177ab20f38b";
            case AppSettings.THEME_CYAN:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_cyan.jpg?token=41229ff18af4f78b73bbe2466414a401440a0709";
            case AppSettings.THEME_DEEP_ORANGE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_deep_orange.jpg?token=f3c83ec3da514673ec72db3eb635ce6e603eed80";
            case AppSettings.THEME_DEEP_PURPLE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_deep_purple.jpg?token=cbaeab289e4cfaea4f555153fe8b3fb9515794d6";
            case AppSettings.THEME_GREEN:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_green.jpg?token=e55ba5aea66b9e0b76411bc3925297b23b69ca67";
            case AppSettings.THEME_BLACK:
            case AppSettings.THEME_DARK_BACKGROUND_COLOR:
            case AppSettings.THEME_GREY:
            case AppSettings.THEME_WHITE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_grey.jpg?token=00367e48d0865900f58afc672054157ad6adbd67";
            case AppSettings.THEME_INDIGO:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_indigo.jpg?token=f04944bd1ff8dc3a6ef5abe91f943b72f65339d5";
            case AppSettings.THEME_LIGHT_BLUE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_light_blue.jpg?token=3a3fa37e4eee720b07e883615f5abcd3714ef248";
            case AppSettings.THEME_LIGHT_GREEN:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_light_green.jpg?token=94d5b9445f39aec1dbf17131b45eb1e6ac50fdda";
            case AppSettings.THEME_LIME:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_lime.jpg?token=7299249adc0c9fbbd897cbb9cbccbac5a7b0e23a";
            case AppSettings.THEME_PINK:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_pink.jpg?token=ac10d10928304b26e802ff14c10efda3d8617e78";
            case AppSettings.THEME_PURPLE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_purple.jpg?token=787e20291acb8ff00161e79bab78d4516b4c7ad3";
            case AppSettings.THEME_ORANGE:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_orange.jpg?token=a4ebbc8b1ed3f58f628f597290a0859293e77cf1";
            case AppSettings.THEME_TEAL:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_teal.jpg?token=4b0be84dafdf32f5243b2902521b1b3143f6a6b4";
            case AppSettings.THEME_RED:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_red.jpg?token=eacd34be8c5e9760a2b54a0d2295d4a3e9c9fed2";
            case AppSettings.THEME_YELLOW:
                return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_yellow.jpg?token=7664dac61cdbac07b138c53678fe0eea2793e5d7";
        }

        return "https://bytebucket.org/jklinker/source/raw/b7485202f320afe19e26077e5b7d3626e63c6c5b/promo/user%20banner/banner_grey.jpg?token=00367e48d0865900f58afc672054157ad6adbd67";
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

    public static boolean isAndroidN() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || Build.VERSION.CODENAME.equals("N");
    }

    public static boolean isAndroidO() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || Build.VERSION.CODENAME.equals("O");
    }

    public static boolean isAndroidP() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 || Build.VERSION.CODENAME.equals("P");
    }

    public static void setTaskDescription(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bitmap bm = BitmapFactory.decodeResource(activity.getResources(), R.mipmap.ic_launcher);
            ActivityManager.TaskDescription td = new ActivityManager.TaskDescription(null, bm, AppSettings.getInstance(activity).themeColors.primaryColor);

            activity.setTaskDescription(td);
        }
    }

    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.30;
    }
}
