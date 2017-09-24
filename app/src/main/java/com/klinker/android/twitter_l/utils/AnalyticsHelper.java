package com.klinker.android.twitter_l.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.klinker.android.twitter_l.settings.AppSettings;

public class AnalyticsHelper {

    // LOGIN EVENTS
    private static final String START_LOGIN = "START_LOGIN";
    private static final String LOGIN_TO_TWITTER = "LOGIN_TO_TWITTER";
    private static final String FINISH_LOGIN_TO_TWITTER = "FINISH_LOGIN_TO_TWITTER";
    private static final String LOGIN_DOWNLOAD_TWEETS = "LOGIN_DOWNLOADED_TWEETS";
    private static final String FINISH_LOGIN = "FINISH_LOGIN";

    // RATE IT EVENTS
    private static final String SHOW_RATE_IT_PROMPT = "SHOW_RATE_IT_PROMPT";
    private static final String RATE_IT_ON_PLAY_STORE = "RATE_IT_ON_PLAY_STORE";

    // GENERAL LOGGING
    private static final String ERROR_LOADING_FROM_NOTIFICATION = "ERROR_LOADING_FROM_NOTIFICATION";

    private static void logEvent(Context context, String event) {
        Bundle bundle = new Bundle();
        logEvent(context, event, bundle);
    }

    private static void logEvent(Context context, String event, Bundle bundle) {
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
        logEvent(context, FINISH_LOGIN_TO_TWITTER);
    }

    public static void downloadTweets(Context context) {
        logEvent(context, LOGIN_DOWNLOAD_TWEETS);
    }

    public static void finishLogin(Context context) {
        logEvent(context, FINISH_LOGIN);
    }

    public static void showRateItPrompt(Context context) {
        logEvent(context, SHOW_RATE_IT_PROMPT);
    }

    public static void rateItOnPlayStore(Context context) {
        logEvent(context, RATE_IT_ON_PLAY_STORE);
    }

    public static void errorLoadingTweetFromNotification(Context context, String errorMessage) {
        Bundle bundle = new Bundle();
        bundle.putString("error_message", errorMessage);
        logEvent(context, ERROR_LOADING_FROM_NOTIFICATION);
    }
}
