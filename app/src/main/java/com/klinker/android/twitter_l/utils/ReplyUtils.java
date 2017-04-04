package com.klinker.android.twitter_l.utils;

import twitter4j.Status;

public class ReplyUtils {

    public boolean shouldCompressReplies(Status status) {
        return status.getInReplyToScreenName() != null && !status.getInReplyToScreenName().isEmpty();
    }

    public static boolean showMultipleReplyNames(String replies) {
        return replies.split(" ").length < 3;
    }

    public static String getReplyingToHandles(String text) {
        String handles = "";
        String[] split = text.split(" ");
        for (int i = 0; i < split.length; i++) {
            if (split[i].contains("@")) {
                handles += split[i] + " ";
            } else {
                break;
            }
        }

        return handles;
    }
}
