package com.klinker.android.talon.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.talon.ui.fragments.DMFragment;
import com.klinker.android.talon.ui.fragments.HomeFragment;
import com.klinker.android.talon.ui.fragments.LinksFragment;
import com.klinker.android.talon.ui.fragments.MentionsFragment;
import com.klinker.android.talon.ui.fragments.PicFragment;

public class TimelinePagerAdapter extends FragmentPagerAdapter {

    public TimelinePagerAdapter(FragmentManager fm) {
        super(fm);
    }
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                LinksFragment links = new LinksFragment();
                return links;
            case 1:
                PicFragment pics = new PicFragment();
                return pics;
            case 2:
                HomeFragment home = new HomeFragment();
                return home;
            case 3:
                MentionsFragment mentions = new MentionsFragment();
                return mentions;
            case 4:
                DMFragment dm = new DMFragment();
                return dm;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 5;
    }
}
