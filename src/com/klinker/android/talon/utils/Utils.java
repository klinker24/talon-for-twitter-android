package com.klinker.android.talon.utils;

import android.content.Context;

import com.klinker.android.talon.settings.AppSettings;

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
}
