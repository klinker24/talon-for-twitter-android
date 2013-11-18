package com.klinker.android.talon.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import com.klinker.android.talon.ui.fragments.DMFragment;
import com.klinker.android.talon.ui.fragments.HomeFragment;
import com.klinker.android.talon.ui.fragments.MentionsFragment;
import android.support.v13.app.FragmentPagerAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: luke
 * Date: 11/15/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class TimelinePagerAdapter extends FragmentPagerAdapter {

    public TimelinePagerAdapter(FragmentManager fm) {
        super(fm);
    }
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                HomeFragment home = new HomeFragment();
                return home;
            case 1:
                MentionsFragment mentions = new MentionsFragment();
                return mentions;
            case 2:
                DMFragment dm = new DMFragment();
                return dm;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }
}
