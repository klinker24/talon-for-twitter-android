package com.klinker.android.twitter_l.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.FragmentPagerAdapter;
import com.klinker.android.twitter_l.activities.media_viewer.PhotoFragment;

public class PhotoPagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private String[] urls;
    private Fragment[] fragments;

    public PhotoPagerAdapter(FragmentManager fm, Context context, String[] urls) {
        super(fm);
        this.context = context;
        this.urls = urls;
        this.fragments = new Fragment[urls.length];

        for (int i = 0; i < urls.length; i++) {
            fragments[i] = PhotoFragment.getInstance(urls[i]);
        }
    }

    @Override
    public Fragment getItem(int i) {
        return fragments[i];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }
}
