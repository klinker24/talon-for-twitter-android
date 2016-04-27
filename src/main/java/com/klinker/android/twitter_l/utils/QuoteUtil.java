package com.klinker.android.twitter_l.utils;

import java.util.List;

import twitter4j.Status;

public class QuoteUtil {
    public static String getSearchString(String screenName, long tweetId) {
        screenName = screenName.replace("@", "");

        return  "(" +
                "https://twitter.com/statuses/" + tweetId +
                        " OR " +
                "https://twitter.com/" + screenName + "/status/" + tweetId +
                        " OR " +

                "http://twitter.com/statuses/" + tweetId +
                        " OR " +
                "http://twitter.com/" + screenName + "/status/" + tweetId +
                        " OR " +

                screenName + "/status/" + tweetId +
                ") " +

                " -RT";
    }

    public static List<Status> stripNoQuotes(List<Status> statuses) {
        for (int i = 0; i < statuses.size(); i++) {
            if (statuses.get(i).getQuotedStatus() == null) {
                statuses.remove(i);
                i--;
            }
        }

        return statuses;
    }
}
