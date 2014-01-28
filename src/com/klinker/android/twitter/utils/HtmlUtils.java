package com.klinker.android.twitter.utils;

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
            String n = name.getScreenName();
            if (n.length() > 1) {
                mUsers += n + "  ";
            }
        }

        HashtagEntity[] hashtags = status.getHashtagEntities();
        String mHashtags = "";

        for (HashtagEntity hashtagEntity : hashtags) {
            String text = hashtagEntity.getText();
            if (text.length() > 1) {
                mHashtags += text + "  ";
            }
        }

        URLEntity[] urls = status.getURLEntities();
        String expandedUrls = "";
        String compressedUrls = "";

        for (URLEntity entity : urls) {
            String url = entity.getExpandedURL();
            if (url.length() > 1) {
                expandedUrls += url + "  ";
                compressedUrls += entity.getURL() + "  ";
            }
        }

        MediaEntity[] medias = status.getMediaEntities();
        String mediaExp = "";
        String mediaComp = "";

        for (MediaEntity e : medias) {
            String url = e.getURL();
            if (url.length() > 1) {
                mediaComp += url + "  ";
                mediaExp += e.getExpandedURL() + "  ";
            }
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
                    if (tweetTexts.contains(s)) {
                        tweetTexts = tweetTexts.replace("@" + s, "<font color='#FF8800'>@" + s + "</font>");
                    } else {
                        tweetTexts = tweetTexts.replace("@" + s.toLowerCase(), "<font color='#FF8800'>@" + s + "</font>");
                    }
                }
            }
        }

        if(hashtags.length > 0) {
            for (String s : sHashtags) {
                if (s.length() > 1 && !s.equals("FF")) {
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
                try {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp + "</font>");
                }
                if(exp.toLowerCase().contains("instag")) {
                    imageUrl = exp + "media/?size=t";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("youtub") && !(exp.contains("channel") || exp.contains("user"))) {
                    // first get the youtube video code
                    int start = exp.indexOf("v=") + 2;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    } else if (exp.substring(start).contains("?")) {
                        end = exp.indexOf("?");
                    }
                    try {
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                    } catch (Exception e) {
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, exp.length() - 1) + "/2.jpg";
                    }
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("youtu.be")) {
                    // first get the youtube video code
                    int start = exp.indexOf(".be/") + 4;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    } else if (exp.substring(start).contains("?")) {
                        end = exp.indexOf("?");
                    }
                    try {
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                    } catch (Exception e) {
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, exp.length() - 1) + "/2.jpg";
                    }
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("twitpic")) {
                    int start = exp.indexOf(".com/") + 5;
                    imageUrl = "http://twitpic.com/show/thumb/" + exp.substring(start).replace("/","");
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("i.imgur")) {
                    int start = exp.indexOf(".com/") + 5;
                    imageUrl = "http://i.imgur.com/" + exp.replace("http://i.imgur.com/", "").replace(".jpg", "") + "t.jpg" ;
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("imgur")) {
                    int start = exp.indexOf(".com/") + 6;
                    imageUrl = "http://i.imgur.com/" + exp.replace("http://imgur.com/", "").replace(".jpg", "") + "t.jpg" ;
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("pbs.twimg.com")) {
                    imageUrl = exp;
                    otherUrl += exp + "  ";
                } else {
                    otherUrl += exp + "  ";
                }
            }
        }

        for (int i = 0; i < sMediaComp.length; i++) {
            String comp = sMediaComp[i];
            String exp = sMediaExp[i];

            if (comp.length() > 1 && exp.length() > 1) {
                try {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp + "</font>");
                }
                imageUrl = status.getMediaEntities()[0].getMediaURL();
                //otherUrl += exp;
            }
        }

        return new String[] { tweetTexts, imageUrl, otherUrl, mHashtags, mUsers };
    }

    public static String[] getHtmlStatus(DirectMessage status) {
        UserMentionEntity[] users = status.getUserMentionEntities();
        String mUsers = "";

        for(UserMentionEntity name : users) {
            String n = name.getScreenName();
            if (n.length() > 1) {
                mUsers += n + "  ";
            }
        }

        HashtagEntity[] hashtags = status.getHashtagEntities();
        String mHashtags = "";

        for (HashtagEntity hashtagEntity : hashtags) {
            String text = hashtagEntity.getText();
            if (text.length() > 1) {
                mHashtags += text + "  ";
            }
        }

        URLEntity[] urls = status.getURLEntities();
        String expandedUrls = "";
        String compressedUrls = "";

        for (URLEntity entity : urls) {
            String url = entity.getExpandedURL();
            if (url.length() > 1) {
                expandedUrls += url + "  ";
                compressedUrls += entity.getURL() + "  ";
            }
        }

        MediaEntity[] medias = status.getMediaEntities();
        String mediaExp = "";
        String mediaComp = "";

        for (MediaEntity e : medias) {
            String url = e.getURL();
            if (url.length() > 1) {
                mediaComp += url + "  ";
                mediaExp += e.getExpandedURL() + "  ";
            }
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
                    if (tweetTexts.contains(s)) {
                        tweetTexts = tweetTexts.replace("@" + s, "<font color='#FF8800'>@" + s + "</font>");
                    } else {
                        tweetTexts = tweetTexts.replace("@" + s.toLowerCase(), "<font color='#FF8800'>@" + s + "</font>");
                    }
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
                try {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp + "</font>");
                }
                if(exp.toLowerCase().contains("instag")) {
                    imageUrl = exp + "media/?size=t";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("youtub") && !(exp.contains("channel") || exp.contains("user"))) { // normal youtube link
                    // first get the youtube video code
                    int start = exp.indexOf("v=") + 2;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    } else if (exp.substring(start).contains("?")) {
                        end = exp.indexOf("?");
                    }
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("youtu.be")) { // shortened youtube link
                    // first get the youtube video code
                    int start = exp.indexOf(".be/") + 4;
                    int end = exp.length();
                    if (exp.substring(start).contains("&")) {
                        end = exp.indexOf("&");
                    } else if (exp.substring(start).contains("?")) {
                        end = exp.indexOf("?");
                    }
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/2.jpg";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("twitpic")) {
                    int start = exp.indexOf(".com/") + 5;
                    imageUrl = "http://twitpic.com/show/thumb/" + exp.substring(start).replace("/", "");
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("imgur")) {
                    int start = exp.indexOf(".com/") + 6;
                    imageUrl = "http://i.imgur.com/" + exp.substring(start) + "m.jpg" ;
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("pbs.twimg.com")) {
                    imageUrl = exp;
                    otherUrl += exp + "  ";
                } else {
                    otherUrl += exp + "  ";
                }
            }
        }

        for (int i = 0; i < sMediaComp.length; i++) {
            String comp = sMediaComp[i];
            String exp = sMediaExp[i];

            if (comp.length() > 1 && exp.length() > 1) {
                try {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp.substring(0, 20) + "..." + "</font>");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, "<font color='#FF8800'>" + exp + "</font>");
                }
                imageUrl = status.getMediaEntities()[0].getMediaURL();
            }
        }

        return new String[] { tweetTexts, imageUrl, otherUrl, mHashtags, mUsers };
    }

    public static String removeColorHtml(String text) {
        text = text.replaceAll("<font color='#FF8800'>", "");
        text = text.replaceAll("</font>", "");
        return text;
    }
}
