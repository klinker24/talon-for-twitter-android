package com.klinker.android.twitter_l.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.ui.profile_viewer.fragments.ProfileFragment;
import com.klinker.android.twitter_l.ui.profile_viewer.fragments.sub_fragments.ProfileFavoritesFragment;
import com.klinker.android.twitter_l.ui.profile_viewer.fragments.sub_fragments.ProfileMentionsFragment;
import com.klinker.android.twitter_l.ui.profile_viewer.fragments.sub_fragments.ProfilePicturesFragment;

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
                ProfilePicturesFragment pics = new ProfilePicturesFragment(screenName);
                return pics;
            case 1:
                ProfileFragment profile = new ProfileFragment(name, screenName, proPic, tweetId, isRetweet, isMyProfile);
                return profile;
            case 2:
                ProfileMentionsFragment mentions = new ProfileMentionsFragment(screenName);
                return mentions;
            case 3:
                ProfileFavoritesFragment favs = new ProfileFavoritesFragment(screenName);
                return favs;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.pictures);
            case 1:
                return context.getResources().getString(R.string.profile);
            case 2:
                return context.getResources().getString(R.string.mentions);
            case 3:
                return context.getResources().getString(R.string.favorites);
        }
        return null;
    }
}
