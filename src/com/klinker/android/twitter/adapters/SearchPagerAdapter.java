package com.klinker.android.twitter.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.search.TwitterSearchFragment;
import com.klinker.android.twitter.ui.search.UserSearchFragment;


public class SearchPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private boolean onlyId;
    private boolean translucent;
    private String query;

    public SearchPagerAdapter(FragmentManager fm, Context context, boolean onlyId, String query, boolean translucent) {
        super(fm);
        this.context = context;
        this.onlyId = onlyId;
        this.translucent = translucent;
        this.query = query;

        Log.v("talon_searching", "query: " + query);
    }

    @Override
    public Fragment getItem(int i) {
        Fragment f = null;
        switch (i) {
            case 0:
                f = new TwitterSearchFragment(onlyId, query, translucent);
                break;
            case 1:
                f = new UserSearchFragment(query, translucent);
                break;
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
