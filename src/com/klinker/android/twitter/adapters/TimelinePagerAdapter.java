package com.klinker.android.twitter.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v13.app.FragmentPagerAdapter;
import android.util.Log;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.fragments.DMFragment;
import com.klinker.android.twitter.ui.fragments.HomeFragment;
import com.klinker.android.twitter.ui.fragments.LinksFragment;
import com.klinker.android.twitter.ui.fragments.lists.List1Fragment;
import com.klinker.android.twitter.ui.fragments.lists.List2Fragment;
import com.klinker.android.twitter.ui.fragments.lists.ListFragment;
import com.klinker.android.twitter.ui.fragments.MentionsFragment;
import com.klinker.android.twitter.ui.fragments.PicFragment;

public class TimelinePagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private SharedPreferences sharedPrefs;

    // list stuff
    public int list1Id; // furthest left list
    public int list2Id; // list next to the timeline
    public int page1Type;
    public int page2Type;
    public String page1Name;
    public String page2Name;

    public int numExtraPages = 0;

    public TimelinePagerAdapter(FragmentManager fm, Context context, SharedPreferences sharedPreferences) {
        super(fm);
        this.context = context;
        this.sharedPrefs = sharedPreferences;

        int currentAccount = sharedPreferences.getInt("current_account", 1);

        // List ID's
        list1Id = sharedPrefs.getInt("account_" + currentAccount + "_list_1", 0);
        list2Id = sharedPrefs.getInt("account_" + currentAccount + "_list_2", 0);
        page1Type = sharedPreferences.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
        page2Type = sharedPreferences.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);
        page1Name = sharedPreferences.getString("account_" + currentAccount + "_name_1", "");
        page2Name = sharedPreferences.getString("account_" + currentAccount + "_name_2", "");

        if (page1Type != AppSettings.PAGE_TYPE_NONE) {
            numExtraPages++;
        }

        if (page2Type != AppSettings.PAGE_TYPE_NONE) {
            numExtraPages++;
        }

        Log.v("talon_lists", page1Type + " " + page2Type);
    }

    @Override
    public Fragment getItem(int i) {
        Fragment frag = null;

        if(numExtraPages == 2) {
            switch (i) {
                case 0:
                    frag = getFrag(page1Type, list1Id, 1);
                    break;
                case 1:
                    frag = getFrag(page2Type, list2Id, 2);
                    break;
                case 2:
                    frag = new HomeFragment();
                    break;
                case 3:
                    frag = new MentionsFragment();
                    break;
                case 4:
                    frag = new DMFragment();
                    break;
            }
        } else if (numExtraPages == 1) {
            switch (i) {
                case 0:
                    if (page1Type != AppSettings.PAGE_TYPE_NONE) {
                        frag = getFrag(page1Type, list1Id, 1);
                    } else {
                        frag = getFrag(page2Type, list2Id, 1);
                    }
                    break;
                case 1:
                    frag = new HomeFragment();
                    break;
                case 2:
                    frag = new MentionsFragment();
                    break;
                case 3:
                    frag = new DMFragment();
                    break;
            }
        } else {
            switch (i) {
                case 0:
                    frag = new HomeFragment();
                    break;
                case 1:
                    frag = new MentionsFragment();
                    break;
                case 2:
                    frag = new DMFragment();
                    break;
            }
        }

        return frag;
    }

    @Override
    public CharSequence getPageTitle(int i) {
        String frag = "";
        if(numExtraPages == 2) {
            switch (i) {
                case 0:
                    frag = getName(page1Name, page1Type);
                    break;
                case 1:
                    frag = getName(page2Name, page2Type);
                    break;
                case 2:
                    frag = context.getResources().getString(R.string.timeline);
                    break;
                case 3:
                    frag = context.getResources().getString(R.string.mentions);
                    break;
                case 4:
                    frag = context.getResources().getString(R.string.direct_messages);
            }
        } else if (numExtraPages == 1) {
            switch (i) {
                case 0:
                    if (page1Type != AppSettings.PAGE_TYPE_NONE) {
                        frag = getName(page1Name, page1Type);
                    } else {
                        frag = getName(page2Name, page2Type);
                    }
                    break;
                case 1:
                    frag = context.getResources().getString(R.string.timeline);
                    break;
                case 2:
                    frag = context.getResources().getString(R.string.mentions);
                    break;
                case 3:
                    frag = context.getResources().getString(R.string.direct_messages);
                    break;
            }
        } else {
            switch (i) {
                case 0:
                    frag = context.getResources().getString(R.string.timeline);
                    break;
                case 1:
                    frag = context.getResources().getString(R.string.mentions);
                    break;
                case 2:
                    frag = context.getResources().getString(R.string.direct_messages);
                    break;
            }
        }
        return frag;
    }

    @Override
    public int getCount() {
        return 3 + numExtraPages;
    }

    public Fragment getFrag(int type, int listId, int listPage) {
        switch (type) {
            case AppSettings.PAGE_TYPE_LIST:
                if (listPage == 1) {
                    return new List1Fragment(listId);
                } else {
                    return new List2Fragment(listId);
                }
            case AppSettings.PAGE_TYPE_LINKS:
                return new LinksFragment();
            case AppSettings.PAGE_TYPE_PICS:
                return new PicFragment();
        }

        return null;
    }

    public String getName(String listName, int type) {
        switch (type) {
            case AppSettings.PAGE_TYPE_LIST:
                return listName;
            case AppSettings.PAGE_TYPE_LINKS:
                return context.getResources().getString(R.string.links);
            case AppSettings.PAGE_TYPE_PICS:
                return context.getResources().getString(R.string.pictures);
        }

        return null;
    }
}
