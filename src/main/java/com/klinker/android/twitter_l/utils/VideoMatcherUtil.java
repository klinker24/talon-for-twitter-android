package com.klinker.android.twitter_l.utils;

public class VideoMatcherUtil {
    public static boolean containsThirdPartyVideo(String string) {
        return string.contains("vine.co/v/") ||
                string.contains("amp.twimg.com/v/") ||
                string.contains("snpy.tv");
    }

    public static boolean noInAppPlayer(String string) {
        return string.contains("amp.twimg.com/v/") ||
                string.contains("snpy.tv");
    }

    public static boolean isTwitterGifLink(String link) {
        return (link.contains("/photo/1") && link.contains("twitter.com/")) || // before gifs were in api
                (link.contains("pbs.twimg.com/tweet_video")); // after gifs in api
    }

    public static boolean isTwitterVideoLink(String link) {
        return link.contains("video.twimg.com");
    }
}
