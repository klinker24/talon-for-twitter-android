package com.klinker.android.twitter_l.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.ui.search.TimelineSearchFragment;
import com.klinker.android.twitter_l.ui.search.TwitterSearchFragment;
import com.klinker.android.twitter_l.ui.search.UserSearchFragment;


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
                f = new TimelineSearchFragment(query, translucent);
                break;
            case 1:
                f = new TwitterSearchFragment(onlyId, query, translucent);
                break;
            case 2:
                f = new UserSearchFragment(query, translucent);
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
                return context.getResources().getString(R.string.timeline);
            case 1:
                return context.getResources().getString(R.string.twitter);
            case 2:
                return context.getResources().getString(R.string.user);
        }
        return null;
    }
}
