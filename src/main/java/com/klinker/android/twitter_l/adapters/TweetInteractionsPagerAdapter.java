package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.ui.search.TimelineSearchFragment;
import com.klinker.android.twitter_l.ui.search.TwitterSearchFragment;
import com.klinker.android.twitter_l.ui.search.UserSearchFragment;
import com.klinker.android.twitter_l.ui.tweet_viewer.LikersFragment;
import com.klinker.android.twitter_l.ui.tweet_viewer.QuotersFragment;
import com.klinker.android.twitter_l.ui.tweet_viewer.RetweetersFragment;

public class TweetInteractionsPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    private String screenname;
    private long tweetId;

    public TweetInteractionsPagerAdapter(Activity context, String screenname, long tweetId) {
        super(context.getFragmentManager());
        this.context = context;

        this.screenname = screenname;
        this.tweetId = tweetId;
    }

    @Override
    public Fragment getItem(int i) {
        Fragment f = null;
        switch (i) {
            case 0:
                f = QuotersFragment.getInstance(screenname, tweetId);
                break;
            case 1:
                f = RetweetersFragment.getInstance(tweetId);
                break;
            case 2:
                f = LikersFragment.getInstance(tweetId);
                break;
        }
        return f;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.quotes);
            case 1:
                return context.getResources().getString(R.string.retweets);
            case 2:
                return context.getResources().getString(R.string.favorites);
        }
        return null;
    }
}

