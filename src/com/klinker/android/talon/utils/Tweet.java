package com.klinker.android.talon.utils;

public class Tweet {
    private long id;
    private String text;
    private String name;
    private String picUrl;
    private String screenName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTweet() {
        return text;
    }

    public void setTweet(String tweet) {
        this.text = tweet;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicUrl() {
        return this.picUrl;
    }

    public void setPicUrl(String url) {
        this.picUrl = url;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String name) {
        this.screenName = name;
    }

    // Will be used by the ArrayAdapter in the ListView
    @Override
    public String toString() {
        return text;
    }
}
