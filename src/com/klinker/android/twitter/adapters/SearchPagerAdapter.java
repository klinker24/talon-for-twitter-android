package com.klinker.android.twitter.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.search.TwitterSearch;


public class SearchPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private boolean onlyId;
    private String query;

    public SearchPagerAdapter(FragmentManager fm, Context context, boolean onlyId, String query) {
        super(fm);
        this.context = context;
        this.onlyId = onlyId;
        this.query = query;
    }

    @Override
    public Fragment getItem(int i) {
        Fragment f = null;
        switch (i) {
            case 0:
                f = new TwitterSearch(onlyId, query);
            case 1:
                f = new UserSearch(query);
            /*case 2:
                CategoryFragment people = new CategoryFragment();
                return people;
            case 3:
                NearbyTweets nearby = new NearbyTweets();
                return nearby;*/
        }
        return f;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.twitter);
            case 1:
                return context.getResources().getString(R.string.user);
        }
        return null;
    }
}
