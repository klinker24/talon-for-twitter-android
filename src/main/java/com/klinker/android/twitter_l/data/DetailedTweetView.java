package com.klinker.android.twitter_l.data;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Status;

public class DetailedTweetView extends TweetView {

    public static DetailedTweetView create(final Context context, final long tweetId) {
        final DetailedTweetView tweetView = new DetailedTweetView(context);
        final AppSettings settings = AppSettings.getInstance(context);

        tweetView.setCurrentUser(settings.myScreenName);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Status status = Utils.getTwitter(context, settings).showStatus(tweetId);
                    tweetView.setData(status);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return tweetView;
    }

    private DetailedTweetView (Context context) {
        super(context);
    }

    private HoloTextView likesText;
    private HoloTextView retweetsText;

    @Override
    public View getView() {
        View v = super.getView();
        v.setPadding(0,0,Utils.toDP(16, context), Utils.toDP(64, context));

        return v;
    }

    @Override
    protected void setComponents(View v) {
        super.setComponents(v);

        // find the like and retweet buttons
        likesText = (HoloTextView) v.findViewById(R.id.likes);
        retweetsText = (HoloTextView) v.findViewById(R.id.retweets);

        likesText.setTextSize(settings.textSize);
        retweetsText.setTextSize(settings.textSize);
    }

    @Override
    protected void bindData() {
        super.bindData();

        likesText.setText(numLikes + "");
        retweetsText.setText(numRetweets + "");
    }

    @Override
    protected View createTweet() {
        View tweetView = ((Activity) context).getLayoutInflater().inflate(R.layout.detailed_tweet, null, false);
        return tweetView;
    }

    @Override
    protected boolean shouldShowImage() {
        return showImage;
    }
    private boolean showImage = true;
    public void setShouldShowImage(boolean showImage) {
        this.showImage = showImage;
    }
}
