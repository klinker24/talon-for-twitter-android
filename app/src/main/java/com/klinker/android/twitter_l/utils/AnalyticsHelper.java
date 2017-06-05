package com.klinker.android.twitter_l.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.klinker.android.twitter_l.settings.AppSettings;

public class AnalyticsHelper {

    private static final String START_LOGIN = "START_LOGIN";
    private static final String FINISH_LOGIN = "FINISH_LOGIN";

    private static void logEvent(Context context, String event) {
        Bundle bundle = new Bundle();
        bundle.putString("screenname", AppSettings.getInstance(context).myScreenName);
        FirebaseAnalytics.getInstance(context).logEvent(event, bundle);
    }

    public static void startLogin(Context context) {
        logEvent(context, START_LOGIN);
    }

    public static void finishLogin(Context context) {
        logEvent(context, FINISH_LOGIN);
    }
}
