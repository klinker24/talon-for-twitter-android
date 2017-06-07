package com.klinker.android.twitter_l.utils;

import android.util.Log;

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

    // used to put a badge on the videos on the adapters and tweetview
    public static boolean isTwitterGifLink(String link) {
        if (link == null)
            return false;

        if (link.contains("dm_gif")) {
            return true;
        }

        // Ex: http://pbs.twimg.com/tweet_video/CcAargSUAAADXgD.mp4
        return (link.contains("/photo/1") && link.contains("twitter.com/")) || // before gifs were in api
                (link.contains("video.twimg.com/tweet_video") || link.contains("pbs.twimg.com/tweet_video")); // after gifs in api
    }

    public static boolean isTwitterVideoLink(String link) {
        if (link == null)
            return false;

        // Ex; https://video.twimg.com/ext_tw_video/702708414506401792/pu/vid/720x1280/X6igXR0RphT5oQDW.mp4
        return link.contains("video.twimg.com/ext_tw_video");
    }
}
