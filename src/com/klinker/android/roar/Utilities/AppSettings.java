package com.klinker.android.roar.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
    public boolean isTwitterLoggedIn;
    public int theme;

    public AppSettings(Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        authenticationToken = sharedPrefs.getString("authentication_token", "none");
        authenticationTokenSecret = sharedPrefs.getString("authentication_token_secret", "none");
        isTwitterLoggedIn = sharedPrefs.getBoolean("is_logged_in", false);
        theme = 1;
    }
}
