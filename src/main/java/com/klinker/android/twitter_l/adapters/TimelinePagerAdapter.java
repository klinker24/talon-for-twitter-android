package com.klinker.android.twitter_l.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.main_fragments.MainFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.extentions.FavUsersFragment;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.extentions.LinksFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.ListFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.MentionsFragment;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.extentions.PicFragment;
import com.klinker.android.twitter_l.utils.SystemBarVisibility;

public class TimelinePagerAdapter extends FragmentPagerAdapter {

    private Context context;
    private SharedPreferences sharedPrefs;
    private boolean removeHome;
    private SystemBarVisibility watcher;

    // list stuff
    public long list1Id; // furthest left list
    public long list2Id; // list next to the timeline
    public int page1Type;
    public int page2Type;
    public String page1Name;
    public String page2Name;

    public int numExtraPages = 0;

    private Fragment[] frags;

    // remove the home fragment to swipe to, since it is on the launcher
    public TimelinePagerAdapter(FragmentManager fm, Context context, SharedPreferences sharedPreferences, boolean removeHome, SystemBarVisibility watcher) {
        super(fm);
        this.context = context;
        this.sharedPrefs = sharedPreferences;
        this.removeHome = removeHome;
        this.watcher = watcher;

        int currentAccount = sharedPreferences.getInt("current_account", 1);

        // used when converting to twitter4j 4.0.0
        if (sharedPrefs.getBoolean("convert_long_lists", true)) {
            sharedPreferences.edit().putBoolean("convert_long_lists", false).commit();
            sharedPrefs.edit().putLong("account_1_list_1_long", sharedPrefs.getInt("account_1_list_1", 0)).commit();
            sharedPrefs.edit().putLong("account_1_list_2_long", sharedPrefs.getInt("account_1_list_2", 0)).commit();
            sharedPrefs.edit().putLong("account_1_list_1_long", sharedPrefs.getInt("account_2_list_1", 0)).commit();
            sharedPrefs.edit().putLong("account_1_list_2_long", sharedPrefs.getInt("account_2_list_2", 0)).commit();
        }

        // List ID's
        list1Id = sharedPrefs.getLong("account_" + currentAccount + "_list_1_long", 0l);
        list2Id = sharedPrefs.getLong("account_" + currentAccount + "_list_2_long", 0l);
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

        if (!removeHome) {
            frags = new Fragment[3 + numExtraPages];
        } else {
            frags = new Fragment[2 + numExtraPages];
        }
    }

    @Override
    public Fragment getItem(int i) {
        MainFragment frag = null;

        if (!removeHome) {
            if (numExtraPages == 2) {
                switch (i) {
                    case 0:
                        frag = getFrag(page1Type, list1Id);
                        break;
                    case 1:
                        frag = getFrag(page2Type, list2Id);
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
                            frag = getFrag(page1Type, list1Id);
                        } else {
                            frag = getFrag(page2Type, list2Id);
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
        } else {
            if (numExtraPages == 2) {
                switch (i) {
                    case 0:
                        frag = getFrag(page1Type, list1Id);
                        break;
                    case 1:
                        frag = getFrag(page2Type, list2Id);
                        break;
                    case 2:
                        frag = new MentionsFragment();
                        break;
                    case 3:
                        frag = new DMFragment();
                        break;
                }
            } else if (numExtraPages == 1) {
                switch (i) {
                    case 0:
                        if (page1Type != AppSettings.PAGE_TYPE_NONE) {
                            frag = getFrag(page1Type, list1Id);
                        } else {
                            frag = getFrag(page2Type, list2Id);
                        }
                        break;
                    case 1:
                        frag = new MentionsFragment();
                        break;
                    case 2:
                        frag = new DMFragment();
                        break;
                }
            } else {
                switch (i) {
                    case 0:
                        frag = new MentionsFragment();
                        break;
                    case 1:
                        frag = new DMFragment();
                        break;
                }
            }
        }

        frag.addSystemBarVisibility(watcher);
        frags[i] = frag;

        return frag;
    }

    @Override
    public CharSequence getPageTitle(int i) {
        String frag = "";
        if (!removeHome) {
            if (numExtraPages == 2) {
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
        } else {
            if (numExtraPages == 2) {
                switch (i) {
                    case 0:
                        frag = getName(page1Name, page1Type);
                        break;
                    case 1:
                        frag = getName(page2Name, page2Type);
                        break;
                    case 2:
                        frag = context.getResources().getString(R.string.mentions);
                        break;
                    case 3:
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
                        frag = context.getResources().getString(R.string.mentions);
                        break;
                    case 2:
                        frag = context.getResources().getString(R.string.direct_messages);
                        break;
                }
            } else {
                switch (i) {
                    case 0:
                        frag = context.getResources().getString(R.string.mentions);
                        break;
                    case 1:
                        frag = context.getResources().getString(R.string.direct_messages);
                        break;
                }
            }
        }
        return frag;
    }

    @Override
    public int getCount() {
        if (!removeHome) {
            return 3 + numExtraPages;
        } else {
            return 2 + numExtraPages;
        }
    }

    public MainFragment getFrag(int type, long listId) {
        switch (type) {
            case AppSettings.PAGE_TYPE_LIST:
                MainFragment f = new ListFragment();

                Bundle b = new Bundle();
                b.putLong("list_id", listId);

                f.setArguments(b);

                return f;
            case AppSettings.PAGE_TYPE_LINKS:
                return new LinksFragment();
            case AppSettings.PAGE_TYPE_PICS:
                return new PicFragment();
            case AppSettings.PAGE_TYPE_FAV_USERS:
                return new FavUsersFragment();
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
            case AppSettings.PAGE_TYPE_FAV_USERS:
                return context.getString(R.string.favorite_users);
        }

        return null;
    }

    public Fragment getRealFrag(int i) {
        return frags[i];
    }
}
