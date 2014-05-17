package com.klinker.android.twitter.data;

/**
 * Created by luke on 5/17/14.
 */
public class ScheduledTweet {
    public String text;
    public int alarmId;
    public long time;

    public ScheduledTweet(String text, int alarmId, long time) {
        this.text = text;
        this.alarmId = alarmId;
        this.time = time;
    }
}
