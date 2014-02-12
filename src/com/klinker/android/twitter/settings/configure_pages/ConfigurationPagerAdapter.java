package com.klinker.android.twitter.settings.configure_pages;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter.settings.configure_pages.fragments.ExampleHomeFragment;
import com.klinker.android.twitter.settings.configure_pages.fragments.PageOneFragment;
import com.klinker.android.twitter.settings.configure_pages.fragments.PageTwoFragment;


public class ConfigurationPagerAdapter extends FragmentPagerAdapter {

    public ConfigurationPagerAdapter(FragmentManager manager, Context context) {
        super(manager);
    }

    @Override
    public Fragment getItem(int i) {

        switch (i) {
            case 0:
                return new PageOneFragment();
            case 1:
                return new PageTwoFragment();
            case 2:
                return new ExampleHomeFragment();
        }

        return null;
    }

    @Override
    public int getCount() {
        return 3; // 3 pages
    }
}
