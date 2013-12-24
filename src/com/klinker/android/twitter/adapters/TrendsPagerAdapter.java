package com.klinker.android.twitter.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.drawer_activities.trends.LocalTrends;
import com.klinker.android.twitter.ui.drawer_activities.trends.WorldTrends;

public class TrendsPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public TrendsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                LocalTrends local = new LocalTrends();
                return local;
            case 1:
                WorldTrends world = new WorldTrends();
                return world;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.local_trends);
            case 1:
                return context.getResources().getString(R.string.world_trends);
        }
        return null;
    }
}
