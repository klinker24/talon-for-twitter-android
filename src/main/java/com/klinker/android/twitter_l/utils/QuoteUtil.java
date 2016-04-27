package com.klinker.android.twitter_l.utils;

public class QuoteUtil {
    public static String getSearchString(String screenName, long tweetId) {
        return  "https://twitter.com/statuses/" + tweetId +
                        " OR " +
                "https://twitter.com/" + screenName + "/status/" + tweetId +
                        " OR " +

                "http://twitter.com/statuses/" + tweetId +
                        " OR " +
                "http://twitter.com/" + screenName + "/status/" + tweetId +
                        " OR " +

                screenName + "/status/" + tweetId;
    }
}
