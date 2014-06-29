package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.content.Context;
import android.content.SharedPreferences;


public class NotiTweetPager extends TweetPager {

    @Override
    public void getFromIntent() {
        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        name = sharedPrefs.getString("fav_user_tweet_name", "");
        screenName = sharedPrefs.getString("fav_user_tweet_screenname", "");
        tweet = sharedPrefs.getString("fav_user_tweet_text", "");
        time = sharedPrefs.getLong("fav_user_tweet_time", 0);
        retweeter = sharedPrefs.getString("fav_user_tweet_retweeter", "");
        webpage = sharedPrefs.getString("fav_user_tweet_webpage", "");
        tweetId = sharedPrefs.getLong("fav_user_tweet_tweet_id", 0);
        picture = sharedPrefs.getBoolean("fav_user_tweet_picture", false);
        proPic = sharedPrefs.getString("fav_user_tweet_pro_pic", "");

        try {
            users = sharedPrefs.getString("fav_user_tweet_users", "").split("  ");
        } catch (Exception e) {
            users = null;
        }

        try {
            hashtags = sharedPrefs.getString("fav_user_tweet_hashtags", "").split("  ");
        } catch (Exception e) {
            hashtags = null;
        }

        try {
            linkString = sharedPrefs.getString("fav_user_tweet_links", "");
            otherLinks = linkString.split("  ");
        } catch (Exception e) {
            otherLinks = null;
        }

        if (screenName.equals(settings.myScreenName)) {
            isMyTweet = true;
        } else if (screenName.equals(retweeter)) {
            isMyRetweet = true;
        }

        tweet = restoreLinks(tweet);
    }
}
