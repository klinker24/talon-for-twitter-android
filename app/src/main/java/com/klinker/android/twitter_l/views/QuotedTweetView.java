package com.klinker.android.twitter_l.views;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;

import twitter4j.Status;

public class QuotedTweetView extends TweetView {
    public QuotedTweetView(Context context, Status status) {
        super(context, status);
    }

    protected View createTweet() {
        View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.tweet_quoted, null, false);
        return tweetView;
    }

    @Override
    protected void setupFontSizes() {

    }

    @Override
    protected void setupImage() {

    }

    @Override
    public void setupProfilePicture() {

    }
}
