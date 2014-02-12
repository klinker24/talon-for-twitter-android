package com.klinker.android.twitter.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.fragments.DMFragment;
import com.klinker.android.twitter.ui.fragments.HomeFragment;
import com.klinker.android.twitter.ui.fragments.LinksFragment;
import com.klinker.android.twitter.ui.fragments.ListFragment;
import com.klinker.android.twitter.ui.fragments.MentionsFragment;
import com.klinker.android.twitter.ui.fragments.PicFragment;

public class TimelinePagerAdapter extends FragmentPagerAdapter {

    private boolean extraPages;
    private SharedPreferences sharedPrefs;

    // list stuff
    public int list1Id; // furthest left list
    public int list2Id; // list next to the timeline
    public int page1Type;
    public int page2Type;

    public TimelinePagerAdapter(FragmentManager fm, boolean extraPages, SharedPreferences sharedPreferences) {
        super(fm);
        this.extraPages = extraPages;
        this.sharedPrefs = sharedPreferences;

        int currentAccount = sharedPreferences.getInt("current_account", 1);

        // List ID's
        list1Id = sharedPrefs.getInt("account_" + currentAccount + "_list_1", 0);
        list2Id = sharedPrefs.getInt("account_" + currentAccount + "_list_2", 0);
        page1Type = sharedPreferences.getInt("account_" + currentAccount + "_page_1", 0);
        page2Type = sharedPreferences.getInt("account_" + currentAccount + "_page_2", 0);
    }

    @Override
    public Fragment getItem(int i) {
        if(extraPages) {
            switch (i) {
                case 0:
                    LinksFragment links = new LinksFragment();
                    return links;
                case 1:
                    //ListFragment pics = new ListFragment(settings.account1List2);
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
        } else {
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
        }
        return null;
    }

    @Override
    public int getCount() {
        return extraPages ? 5 : 3;
    }
}
