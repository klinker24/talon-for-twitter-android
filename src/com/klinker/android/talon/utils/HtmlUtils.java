package com.klinker.android.talon.utils;

import twitter4j.DirectMessage;
import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.UserMentionEntity;

public class HtmlUtils {

    public static String getHtmlStatus(Status status) {
        UserMentionEntity[] users = status.getUserMentionEntities();
        String mUsers = "";

        for(UserMentionEntity name : users) {
            mUsers += name.getScreenName() + "  ";
        }

        HashtagEntity[] hashtags = status.getHashtagEntities();
        String mHashtags = "";

        for (HashtagEntity hashtagEntity : hashtags) {
            mHashtags += hashtagEntity.getText() + "  ";
        }

        String[] sUsers;
        String[] sHashtags;

        try {
            sUsers = mUsers.split("  ");
        } catch (Exception e) {
            sUsers = new String[0];
        }

        try {
            sHashtags = mHashtags.split("  ");
        } catch (Exception e) {
            sHashtags = new String[0];
        }

        String tweetTexts = status.getText();

        if (users.length > 0) {
            for (String s : sUsers) {
                if (s.length() > 1) {
                    tweetTexts = tweetTexts.replace("@" + s, "<font color='#FF8800'>@" + s + "</font>");
                }
            }
        }

        if(hashtags.length > 0) {
            for (String s : sHashtags) {
                if (s.length() > 1) {
                    tweetTexts = tweetTexts.replace("#" + s, "<font color='#FF8800'>#" + s + "</font>");
                }
            }
        }

        if (tweetTexts.contains("http")) {
            int start = tweetTexts.indexOf("http");
            int end = tweetTexts.indexOf(" ", start) + 1;
            String replacement;
            try {
                replacement = tweetTexts.substring(start, end);
            } catch (Exception e) {
                replacement = tweetTexts.substring(start, tweetTexts.length());
            }
            tweetTexts = tweetTexts.replace(replacement, "<font color='#FF8800'>" + replacement + "</font>");
        }

        return tweetTexts;
    }

    public static String getHtmlStatus(DirectMessage status) {
        UserMentionEntity[] users = status.getUserMentionEntities();
        String mUsers = "";

        for(UserMentionEntity name : users) {
            mUsers += name.getScreenName() + "  ";
        }

        HashtagEntity[] hashtags = status.getHashtagEntities();
        String mHashtags = "";

        for (HashtagEntity hashtagEntity : hashtags) {
            mHashtags += hashtagEntity.getText() + "  ";
        }

        String[] sUsers;
        String[] sHashtags;

        try {
            sUsers = mUsers.split("  ");
        } catch (Exception e) {
            sUsers = new String[0];
        }

        try {
            sHashtags = mHashtags.split("  ");
        } catch (Exception e) {
            sHashtags = new String[0];
        }

        String tweetTexts = status.getText();

        if (users.length > 0) {
            for (String s : sUsers) {
                if (s.length() > 1) {
                    tweetTexts = tweetTexts.replace("@" + s, "<font color='#FF8800'>@" + s + "</font>");
                }
            }
        }

        if(hashtags.length > 0) {
            for (String s : sHashtags) {
                if (s.length() > 1) {
                    tweetTexts = tweetTexts.replace("#" + s, "<font color='#FF8800'>#" + s + "</font>");
                }
            }
        }

        if (tweetTexts.contains("http")) {
            int start = tweetTexts.indexOf("http");
            int end = tweetTexts.indexOf(" ", start) + 1;
            String replacement;
            try {
                replacement = tweetTexts.substring(start, end);
            } catch (Exception e) {
                replacement = tweetTexts.substring(start, tweetTexts.length());
            }
            tweetTexts = tweetTexts.replace(replacement, "<font color='#FF8800'>" + replacement + "</font>");
        }

        return tweetTexts;
    }

    public static String removeColorHtml(String text) {
        text = text.replaceAll("<font color='#FF8800'>", "");
        text = text.replaceAll("</font>", "");
        return text;
    }
}
