package com.klinker.android.talon.utils;

import android.util.Log;

import twitter4j.DirectMessage;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

public class HtmlUtils {

    public static String[] getHtmlStatus(Status status) {
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

        URLEntity[] urls = status.getURLEntities();
        String expandedUrls = "";
        String compressedUrls = "";

        for (URLEntity entity : urls) {
            expandedUrls += entity.getExpandedURL() + "  ";
            compressedUrls += entity.getURL() + "  ";
        }

        MediaEntity[] medias = status.getMediaEntities();
        String mediaExp = "";
        String mediaComp = "";

        for (MediaEntity e : medias) {
            mediaComp += e.getURL() + "  ";
            mediaExp += e.getExpandedURL() + "  ";
        }

        String[] sUsers;
        String[] sHashtags;
        String[] sExpandedUrls;
        String[] sCompressedUrls;
        String[] sMediaExp;
        String[] sMediaComp;

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

        try {
            sCompressedUrls = compressedUrls.split("  ");
        } catch (Exception e) {
            sCompressedUrls = new String[0];
        }

        try {
            sExpandedUrls = expandedUrls.split("  ");
        } catch (Exception e) {
            sExpandedUrls = new String[0];
        }

        try {
            sMediaComp = mediaComp.split("  ");
        } catch (Exception e) {
            sMediaComp = new String[0];
        }

        try {
            sMediaExp = mediaExp.split("  ");
        } catch (Exception e) {
            sMediaExp = new String[0];
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

        String imageUrl = "";
        String otherUrl = "";

        for (int i = 0; i < sCompressedUrls.length; i++) {
            String comp = sCompressedUrls[i];
            String exp = sExpandedUrls[i];

            if (comp.length() > 1 && exp.length() > 1) {
                tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                if(exp.contains("instag")) {
                    imageUrl = exp + "media/?size=t";
                } else if (exp.contains("youtu")) {
                    // first get the youtube video code
                    int start = exp.indexOf("v=") + 2;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    }
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                    otherUrl = exp;
                } else if (exp.contains("youtu.be")) {
                    // first get the youtube video code
                    int start = exp.indexOf(".be/") + 4;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    }
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                }else {
                    otherUrl = exp;
                }
            }
        }

        for (int i = 0; i < sMediaComp.length; i++) {
            String comp = sMediaComp[i];
            String exp = sMediaExp[i];

            if (comp.length() > 1 && exp.length() > 1) {
                tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                imageUrl = status.getMediaEntities()[0].getMediaURL();
            }
        }

        return new String[] { tweetTexts, imageUrl, otherUrl };
    }

    public static String[] getHtmlStatus(DirectMessage status) {
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

        URLEntity[] urls = status.getURLEntities();
        String expandedUrls = "";
        String compressedUrls = "";

        for (URLEntity entity : urls) {
            expandedUrls += entity.getExpandedURL() + "  ";
            compressedUrls += entity.getURL() + "  ";
        }

        MediaEntity[] medias = status.getMediaEntities();
        String mediaExp = "";
        String mediaComp = "";

        for (MediaEntity e : medias) {
            mediaComp += e.getURL() + "  ";
            mediaExp += e.getExpandedURL() + "  ";
        }

        String[] sUsers;
        String[] sHashtags;
        String[] sExpandedUrls;
        String[] sCompressedUrls;
        String[] sMediaExp;
        String[] sMediaComp;

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

        try {
            sCompressedUrls = compressedUrls.split("  ");
        } catch (Exception e) {
            sCompressedUrls = new String[0];
        }

        try {
            sExpandedUrls = expandedUrls.split("  ");
        } catch (Exception e) {
            sExpandedUrls = new String[0];
        }

        try {
            sMediaComp = mediaComp.split("  ");
        } catch (Exception e) {
            sMediaComp = new String[0];
        }

        try {
            sMediaExp = mediaExp.split("  ");
        } catch (Exception e) {
            sMediaExp = new String[0];
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

        String imageUrl = "";
        String otherUrl = "";

        for (int i = 0; i < sCompressedUrls.length; i++) {
            String comp = sCompressedUrls[i];
            String exp = sExpandedUrls[i];

            if (comp.length() > 1 && exp.length() > 1) {
                tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                if(exp.contains("instag")) {
                    imageUrl = exp + "media/?size=t";
                } else if (exp.contains("youtube")) { // normal youtube link
                    // first get the youtube video code
                    int start = exp.indexOf("v=") + 2;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    }
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                } else if (exp.contains("youtu.be")) { // shortened youtube link
                    // first get the youtube video code
                    int start = exp.indexOf(".be/") + 4;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    }
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                } else {
                    otherUrl = exp;
                }
            }
        }

        for (int i = 0; i < sMediaComp.length; i++) {
            String comp = sMediaComp[i];
            String exp = sMediaExp[i];

            if (comp.length() > 1 && exp.length() > 1) {
                tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                imageUrl = status.getMediaEntities()[0].getMediaURL();
            }
        }

        return new String[] { tweetTexts, imageUrl, otherUrl };
    }

    public static String removeColorHtml(String text) {
        text = text.replaceAll("<font color='#FF8800'>", "");
        text = text.replaceAll("</font>", "");
        return text;
    }
}
