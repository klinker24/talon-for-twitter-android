package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import androidx.legacy.app.FragmentPagerAdapter;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.tweet_viewer.LikersFragment;
import com.klinker.android.twitter_l.activities.tweet_viewer.QuotersFragment;
import com.klinker.android.twitter_l.activities.tweet_viewer.RetweetersFragment;

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
                f = LikersFragment.getInstance(tweetId);
                break;
            case 2:
                f = RetweetersFragment.getInstance(tweetId);
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
                return context.getResources().getString(R.string.favorites);
            case 2:
                return context.getResources().getString(R.string.retweets);
        }
        return null;
    }
}

