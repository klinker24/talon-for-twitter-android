package com.klinker.android.twitter.data;

import android.util.Log;

/**
 * Created by luke on 3/20/14.
 */
public class Link {

    private String shortened;
    private String full;

    public Link() {
        shortened = "";
        full = "";
    }

    public Link(String shortUrl, String longUrl) {
        shortened = shortUrl;
        full = longUrl;
    }

    public void setShort(String shortUrl) {
        shortened = shortUrl;
    }

    public void setLong(String longUrl) {
        full = longUrl;
    }

    public String getShort() {
        return shortened;
    }

    public String getLong() {
        return full;
    }
}
