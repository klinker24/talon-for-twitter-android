package com.klinker.android.twitter_l.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.klinker.android.twitter_l.settings.AppSettings;

public class AnalyticsHelper {

    private static final String START_LOGIN = "START_LOGIN";
    private static final String LOGIN_TO_TWITTER = "LOGIN_TO_TWITTER";
    private static final String FINISH_LOGIN_TO_TWITTER = "FINISH_LOGIN_TO_TWITTER";
    private static final String LOGIN_DOWNLOAD_TWEETS = "LOGIN_DOWNLOADED_TWEETS";
    private static final String FINISH_LOGIN = "FINISH_LOGIN";

    private static void logEvent(Context context, String event) {
        Bundle bundle = new Bundle();
        bundle.putString("screenname", AppSettings.getInstance(context).myScreenName);
        FirebaseAnalytics.getInstance(context).logEvent(event, bundle);
    }

    public static void startLogin(Context context) {
        logEvent(context, START_LOGIN);
    }

    public static void loginToTwitter(Context context) {
        logEvent(context, LOGIN_TO_TWITTER);
    }

    public static void finishLoginToTwitter(Context context) {
        logEvent(context, LOGIN_TO_TWITTER);
    }

    public static void downloadTweets(Context context) {
        logEvent(context, LOGIN_DOWNLOAD_TWEETS);
    }

    public static void finishLogin(Context context) {
        logEvent(context, FINISH_LOGIN);
    }
}
