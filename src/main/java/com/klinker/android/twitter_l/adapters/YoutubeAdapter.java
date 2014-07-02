package com.klinker.android.twitter_l.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.tweet_viewer.fragments.TweetYouTubeFragment;

/**
 * Created by luke on 7/2/14.
 */
public class YoutubeAdapter extends FragmentPagerAdapter {

    public String url;
    public AppSettings settings;

    public YoutubeAdapter(FragmentManager fm, String url, AppSettings settings) {
        super(fm);
        this.url = url;
        this.settings = settings;
    }

    @Override
    public Fragment getItem(int i) {
        return new TweetYouTubeFragment(settings, url);
    }

    @Override
    public int getCount() {
        return 1;
    }
}
