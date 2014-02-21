package com.klinker.android.twitter.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter.ui.profile_viewer.ProfileFragment;

/**
 * Created by luke on 2/21/14.
 */
public class ProfilePagerAdapter extends FragmentPagerAdapter {
    private Context context;
    private String name;
    private String screenName;
    private String proPic;
    private long tweetId;
    private boolean isRetweet;
    private boolean isMyProfile;

    public ProfilePagerAdapter(FragmentManager fm, Context context, String name, String screenName, String proPic, long tweetId, boolean isRetweet, boolean isMyProfile) {
        super(fm);
        this.context = context;
        this.name = name;
        this.screenName = screenName;
        this.proPic = proPic;
        this.tweetId = tweetId;
        this.isRetweet = isRetweet;
        this.isMyProfile = isMyProfile;
    }
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                ProfileFragment profile = new ProfileFragment(name, screenName, proPic, tweetId, isRetweet, isMyProfile);
                return profile;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Profile";
        }
        return null;
    }
}
