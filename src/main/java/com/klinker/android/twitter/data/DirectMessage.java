package com.klinker.android.twitter.data;

/**
 * Created by luke on 1/20/14.
 */
public class DirectMessage {

    public String name;
    public String screenname;
    public String message;
    public String proPic;

    public DirectMessage(String name, String screenname, String message, String proPic) {
        this.name = name;
        this.screenname = screenname;
        this.message = message;
        this.proPic = proPic;
    }

    public String getName() {
        return name;
    }

    public String getScreenname() {
        return screenname;
    }

    public String getMessage() {
        return message;
    }

    public String getPicture() {
        return proPic;
    }
}
