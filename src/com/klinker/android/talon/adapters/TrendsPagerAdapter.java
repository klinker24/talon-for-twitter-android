package com.klinker.android.talon.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.talon.ui.drawer_activities.trends.LocalTrends;
import com.klinker.android.talon.ui.drawer_activities.trends.WorldTrends;

public class TrendsPagerAdapter extends FragmentPagerAdapter {

    public TrendsPagerAdapter(FragmentManager fm) {
        super(fm);
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
}
