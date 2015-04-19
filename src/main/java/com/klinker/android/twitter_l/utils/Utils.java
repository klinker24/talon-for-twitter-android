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
import android.os.Build;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Transition;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.Window;
import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.text.TextUtils;

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
        TwitterStreamFactory tf = new TwitterStreamFactory(cb.build());
        return tf.getInstance();
    }

    public static Twitter getSecondTwitter(Context context) {
        APIKeys keys = new APIKeys(context);
        AppSettings settings = AppSettings.getInstance(context);
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(keys.consumerKey)
                .setOAuthConsumerSecret(keys.consumerSecret)
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
            int value = TypedValue.complexToDimensionPixelSize(tv.data, context.getResources().getDisplayMetrics());

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                value += Utils.toDP(6, context);
            }

            return value;
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
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);

        if (hasBackKey && hasHomeKey) {
            // no navigation bar, unless it is enabled in the settings
            Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
            Point size = new Point();
            Point realSize = new Point();
            display.getSize(size);
            display.getRealSize(realSize);

            if (Build.MANUFACTURER.toLowerCase().contains("samsung") && !Build.MODEL.toLowerCase().contains("nexus")) {
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

    public static void setSharedContentTransition(Context context, Transition trans) {
        Activity activity = (Activity) context;

        // inside your activity (if you did not enable transitions in your theme)
        try {
            activity.getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            activity.getWindow().setAllowEnterTransitionOverlap(true);
            activity.getWindow().setAllowReturnTransitionOverlap(true);

            activity.getWindow().setSharedElementEnterTransition(trans);
            activity.getWindow().setSharedElementExitTransition(trans);
            activity.getWindow().setSharedElementReenterTransition(trans);
            activity.getWindow().setSharedElementReturnTransition(trans);
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
                }
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
                    context.setTheme(R.style.Theme_TalonLight_Notif_LightGreen);
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
            if (!settings.blackTheme) {
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
                        context.setTheme(R.style.Theme_TalonDark_Notif_LightGreen);
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
            } else {
                switch (settings.theme) {
                    case AppSettings.THEME_AMBER:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Amber);
                        break;
                    case AppSettings.THEME_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Blue);
                        break;
                    case AppSettings.THEME_BLUE_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_BlueGrey);
                        break;
                    case AppSettings.THEME_BROWN:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Brown);
                        break;
                    case AppSettings.THEME_CYAN:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Cyan);
                        break;
                    case AppSettings.THEME_DEEP_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_DeepOrange);
                        break;
                    case AppSettings.THEME_DEEP_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_DeepPurple);
                        break;
                    case AppSettings.THEME_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Green);
                        break;
                    case AppSettings.THEME_GREY:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Grey);
                        break;
                    case AppSettings.THEME_INDIGO:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Indigo);
                        break;
                    case AppSettings.THEME_LIGHT_BLUE:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_LightBlue);
                        break;
                    case AppSettings.THEME_LIGHT_GREEN:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_LightGreen);
                        break;
                    case AppSettings.THEME_LIME:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Lime);
                        break;
                    case AppSettings.THEME_PINK:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Pink);
                        break;
                    case AppSettings.THEME_PURPLE:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Purple);
                        break;
                    case AppSettings.THEME_ORANGE:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Orange);
                        break;
                    case AppSettings.THEME_TEAL:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Teal);
                        break;
                    case AppSettings.THEME_RED:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Red);
                        break;
                    case AppSettings.THEME_YELLOW:
                        context.setTheme(R.style.Theme_TalonBlack_Notif_Yellow);
                        break;
                }
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
