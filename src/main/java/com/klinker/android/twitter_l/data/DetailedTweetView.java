package com.klinker.android.twitter_l.data;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
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
                    final Status status = Utils.getTwitter(context, settings).showStatus(tweetId);
                    if (status == null) {
                        return;
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tweetView.setData(status);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return tweetView;
    }

    private DetailedTweetView (Context context) {
        super(context);

        createProgressView();
    }

    FrameLayout root = null;
    private void createProgressView() {
        root = (FrameLayout) ((Activity) context).getLayoutInflater().inflate(R.layout.progress_spinner, null, false);
        root.setPadding(0,Utils.toDP(16, context),0, Utils.toDP(64, context));
    }

    @Override
    public void setData(Status status) {
        super.setData(status);

        View tweetView = super.getView();

        root.removeAllViews();
        root.addView(tweetView);
    }

    private HoloTextView likesText;
    private HoloTextView retweetsText;

    @Override
    public View getView() {
        return root;
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
