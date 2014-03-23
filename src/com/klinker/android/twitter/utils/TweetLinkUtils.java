package com.klinker.android.twitter.utils;

import android.util.Log;

import com.klinker.android.twitter.settings.AppSettings;

import twitter4j.DirectMessage;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

public class TweetLinkUtils {

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
        String mediaDisplay = "";

        for (MediaEntity e : medias) {
            String url = e.getURL();
            if (url.length() > 1) {
                mediaComp += url + "  ";
                mediaExp += e.getExpandedURL() + "  ";
                mediaDisplay += e.getDisplayURL() + "  ";
            }
        }

        String[] sExpandedUrls;
        String[] sCompressedUrls;
        String[] sMediaExp;
        String[] sMediaComp;
        String[] sMediaDisplay;

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

        try {
            sMediaDisplay = mediaDisplay.split("  ");
        } catch (Exception e) {
            sMediaDisplay = new String[0];
        }

        String tweetTexts = status.getText();

        String imageUrl = "";
        String otherUrl = "";

        for (int i = 0; i < sCompressedUrls.length; i++) {
            String comp = sCompressedUrls[i];
            String exp = sExpandedUrls[i];

            if (comp.length() > 1 && exp.length() > 1) {
                try {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", "").substring(0, 22) + "...");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", ""));
                }
                if(exp.toLowerCase().contains("instag") && !exp.contains("blog.insta")) {
                    imageUrl = exp + "media/?size=m";
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
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/hqdefault.jpg";
                    } catch (Exception e) {
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, exp.length() - 1) + "/hqdefault.jpg";
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
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/hqdefault.jpg";
                    } catch (Exception e) {
                        imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, exp.length() - 1) + "/mqefault.jpg";
                    }
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("twitpic")) {
                    int start = exp.indexOf(".com/") + 5;
                    imageUrl = "http://twitpic.com/show/full/" + exp.substring(start).replace("/","");
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("i.imgur")) {
                    int start = exp.indexOf(".com/") + 5;
                    imageUrl = "http://i.imgur.com/" + exp.replace("http://i.imgur.com/", "").replace(".jpg", "") + "m.jpg" ;
                    imageUrl = imageUrl.replace("gallery/", "");
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("imgur")) {
                    int start = exp.indexOf(".com/") + 6;
                    imageUrl = "http://i.imgur.com/" + exp.replace("http://imgur.com/", "").replace(".jpg", "") + "m.jpg" ;
                    imageUrl = imageUrl.replace("gallery/", "").replace("a/", "");
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("pbs.twimg.com")) {
                    imageUrl = exp;
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("ow.ly/i")) {
                    imageUrl = "http://static.ow.ly/photos/original/" + exp.substring(exp.lastIndexOf("/")).replaceAll("/", "") + ".jpg";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains(".jpg") || exp.toLowerCase().contains(".png")) {
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
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", "").substring(0, 22) + "...");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", ""));
                }
                imageUrl = status.getMediaEntities()[0].getMediaURL();
                otherUrl += sMediaDisplay[i];
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
        String mediaDisplay = "";

        for (MediaEntity e : medias) {
            String url = e.getURL();
            if (url.length() > 1) {
                mediaComp += url + "  ";
                mediaExp += e.getExpandedURL() + "  ";
                mediaDisplay += e.getDisplayURL() + "  ";
            }
        }

        String[] sUsers;
        String[] sHashtags;
        String[] sExpandedUrls;
        String[] sCompressedUrls;
        String[] sMediaExp;
        String[] sMediaComp;
        String[] sMediaDisply;

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

        try {
            sMediaDisply = mediaDisplay.split("  ");
        } catch (Exception e) {
            sMediaDisply = new String[0];
        }

        String tweetTexts = status.getText();

        String imageUrl = "";
        String otherUrl = "";

        for (int i = 0; i < sCompressedUrls.length; i++) {
            String comp = sCompressedUrls[i];
            String exp = sExpandedUrls[i];

            if (comp.length() > 1 && exp.length() > 1) {
                tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", ""));
                /*try {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", "").substring(0, 22) + "...");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", ""));
                }*/
                if(exp.toLowerCase().contains("instag") && !exp.contains("blog.instag")) {
                    imageUrl = exp + "media/?size=m";
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
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/hqdefault.jpg";
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
                    imageUrl = "http://img.youtube.com/vi/" + exp.substring(start, end) + "/hqdefault.jpg";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("twitpic")) {
                    int start = exp.indexOf(".com/") + 5;
                    imageUrl = "http://twitpic.com/show/full/" + exp.substring(start).replace("/", "");
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("imgur")) {
                    int start = exp.indexOf(".com/") + 6;
                    imageUrl = "http://i.imgur.com/" + exp.substring(start) + "m.jpg" ;
                    imageUrl = imageUrl.replace("gallery/", "").replace("a/", ""); 
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("pbs.twimg.com")) {
                    imageUrl = exp;
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains("ow.ly/i")) {
                    Log.v("talon_owly", exp);
                    imageUrl = "http://static.ow.ly/photos/original/" + exp.substring(exp.lastIndexOf("/")).replaceAll("/", "") + ".jpg";
                    otherUrl += exp + "  ";
                } else if (exp.toLowerCase().contains(".jpg") || exp.toLowerCase().contains(".png")) {
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
                tweetTexts = tweetTexts.replace(comp, sMediaDisply[i]);
                /*try {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", "").substring(0, 22) + "...");
                } catch (Exception e) {
                    tweetTexts = tweetTexts.replace(comp, exp.replace("http://", "").replace("https://", "").replace("www.", ""));
                }*/
                imageUrl = status.getMediaEntities()[0].getMediaURL();
                otherUrl += sMediaDisply[i];
            }
        }

        return new String[] { tweetTexts, imageUrl, otherUrl, mHashtags, mUsers };
    }

    public static String removeColorHtml(String text, AppSettings settings) {
        text = text.replaceAll("<font color='#FF8800'>", "");
        text = text.replaceAll("</font>", "");
        if (settings.addonTheme) {
            text = text.replaceAll("<font color='" + settings.accentColor + "'>", "");
            text = text.replaceAll("</font>", "");
        }
        return text;
    }
}
