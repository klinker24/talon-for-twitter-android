package com.klinker.android.twitter_l.data;

/**
 * Created by luke on 5/17/14.
 */
public class ScheduledTweet {
    public String text;
    public int alarmId;
    public long time;
    public int account;

    public ScheduledTweet(String text, int alarmId, long time, int account) {
        this.text = text;
        this.alarmId = alarmId;
        this.time = time;
        this.account = account;
    }
}
