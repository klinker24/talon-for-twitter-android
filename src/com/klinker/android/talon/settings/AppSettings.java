package com.klinker.android.talon.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: luke
 * Date: 11/9/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AppSettings {

    public SharedPreferences sharedPrefs;

    public static String TWITTER_CONSUMER_KEY = "l1RXEJCfdU7q1CRYkTmeaw";
    public static String TWITTER_CONSUMER_SECRET = "uVsk5H5umoLcYdcVSa6rWFQMN0kFOoTBxAnBid4OAkM";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_BLACK = 2;

    public String authenticationToken;
    public String authenticationTokenSecret;
    public String myScreenName;
    public String myName;
    public String myBackgroundUrl;
    public String myProfilePicUrl;
    public String favoriteUserNames;

    public boolean isTwitterLoggedIn;
    public boolean reverseClickActions;
    public boolean advanceWindowed;
    public boolean notifications;
    public boolean led;
    public boolean vibrate;
    public boolean sound;
    public boolean refreshOnStart;
    public boolean autoTrim;
    public boolean uiExtras;
    public boolean wakeScreen;
    public boolean nightMode;

    public int theme;
    public int textSize;
    public int maxTweetsRefresh;
    public int timelineSize;
    public int mentionsSize;
    public int dmSize;

    public long timelineRefresh;
    public long mentionsRefresh;
    public long dmRefresh;

    public AppSettings(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Strings
        if (sharedPrefs.getInt("current_account", 1) == 1) {
            authenticationToken = sharedPrefs.getString("authentication_token_1", "none");
            authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret_1", "none");
            myScreenName = sharedPrefs.getString("twitter_screen_name_1", "");
            myName = sharedPrefs.getString("twitter_users_name_1", "");
            myBackgroundUrl = sharedPrefs.getString("twitter_background_url_1", "");
            myProfilePicUrl = sharedPrefs.getString("profile_pic_url_1", "");
            favoriteUserNames = sharedPrefs.getString("favorite_user_names_1", "");
        } else {
            authenticationToken = sharedPrefs.getString("authentication_token_2", "none");
            authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret_2", "none");
            myScreenName = sharedPrefs.getString("twitter_screen_name_2", "");
            myName = sharedPrefs.getString("twitter_users_name_2", "");
            myBackgroundUrl = sharedPrefs.getString("twitter_background_url_2", "");
            myProfilePicUrl = sharedPrefs.getString("profile_pic_url_2", "");
            favoriteUserNames = sharedPrefs.getString("favorite_user_names_2", "");
        }

        // Booleans
        isTwitterLoggedIn = sharedPrefs.getBoolean("is_logged_in_1", false) || sharedPrefs.getBoolean("is_logged_in_2", false);
        reverseClickActions = sharedPrefs.getBoolean("reverse_click_option", true);
        advanceWindowed = sharedPrefs.getBoolean("advance_windowed", true);
        notifications = sharedPrefs.getBoolean("notifications", true);
        led = sharedPrefs.getBoolean("led", true);
        sound = sharedPrefs.getBoolean("sound", true);
        vibrate = sharedPrefs.getBoolean("vibrate", true);
        refreshOnStart = sharedPrefs.getBoolean("refresh_on_start", true);
        autoTrim = sharedPrefs.getBoolean("auto_trim", true);
        uiExtras = sharedPrefs.getBoolean("ui_extras", true);
        wakeScreen = sharedPrefs.getBoolean("wake", true);

        // Integers
        theme = Integer.parseInt(sharedPrefs.getString("theme", "0"));
        textSize = Integer.parseInt(sharedPrefs.getString("text_size", "14"));
        maxTweetsRefresh = Integer.parseInt(sharedPrefs.getString("max_tweets", "1"));
        timelineSize = Integer.parseInt(sharedPrefs.getString("timeline_size", "1000"));
        mentionsSize = Integer.parseInt(sharedPrefs.getString("mentions_size", "100"));
        dmSize = Integer.parseInt(sharedPrefs.getString("dm_size", "100"));

        // Longs
        timelineRefresh = Long.parseLong(sharedPrefs.getString("timeline_sync_interval", "1800000"));
        mentionsRefresh = Long.parseLong(sharedPrefs.getString("mentions_sync_interval", "1800000"));
        dmRefresh = Long.parseLong(sharedPrefs.getString("dm_sync_interval", "1800000"));

        if (sharedPrefs.getBoolean("night_mode", false)) {
            int nightStartHour = sharedPrefs.getInt("night_start_hour", 22);
            int nightStartMin = sharedPrefs.getInt("night_start_min", 0);
            int dayStartHour = sharedPrefs.getInt("day_start_hour", 6);
            int dayStartMin = sharedPrefs.getInt("day_start_min", 0);

            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minutes = c.get(Calendar.MINUTE);

            int dayStartMinutes = dayStartHour * 60 + dayStartMin;
            int nightStartMinutes = nightStartHour * 60 + nightStartMin;
            int currentMinutes = hour * 60 + minutes;

            if (!(currentMinutes > dayStartMinutes && nightStartMinutes > currentMinutes)) {
                nightMode = true;
                theme = sharedPrefs.getInt("night_theme", 1);
            }
        }
    }
}
