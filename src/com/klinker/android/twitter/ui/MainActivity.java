package com.klinker.android.twitter.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter.data.sq_lite.ListDataSource;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.HtmlUtils;
import com.klinker.android.twitter.utils.NotificationUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends DrawerActivity {

    public static boolean isPopup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        DrawerActivity.settings = AppSettings.getInstance(context);

        /*if(sharedPrefs.getBoolean("pebble_notification", false)) {
            NotificationUtils.sendAlertToPebble(context,
                    "Test from Talon",
                    "Here is just a test from Talon, it will run every time you recreate the app unless the setting is off");
        }*/

        try {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {

        }

        sharedPrefs.edit().putBoolean("refresh_me", getIntent().getBooleanExtra("from_notification", false)).commit();

        setUpTheme();
        setUpWindow();
        setContentView(R.layout.main_activity);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        setUpDrawer(0, getResources().getString(R.string.timeline));

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.timeline));


        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
        }

        mSectionsPagerAdapter = new TimelinePagerAdapter(getFragmentManager(), context, sharedPrefs);

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setCurrentItem(mSectionsPagerAdapter.getCount() - 3);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (!actionBar.isShowing()) {
                    actionBar.show();

                    if (translucent) {
                        statusBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            public void onPageSelected(int position) {

                String title = "" + mSectionsPagerAdapter.getPageTitle(position);

                if (title.equals(getResources().getString(R.string.mentions))) {
                    MainDrawerArrayAdapter.current = 1;
                } else if (title.equals(getResources().getString(R.string.direct_messages))) {
                    MainDrawerArrayAdapter.current = 2;
                } else if (title.equals(getResources().getString(R.string.timeline))) {
                    MainDrawerArrayAdapter.current = 0;
                } else {
                    MainDrawerArrayAdapter.current = -1;
                }

                drawerList.invalidateViews();

                actionBar.setTitle(title);
            }
        });

        mViewPager.setOffscreenPageLimit(4);

        if (getIntent().getBooleanExtra("tutorial", false) && !sharedPrefs.getBoolean("done_tutorial", false)) {
            getIntent().putExtra("tutorial", false);
            sharedPrefs.edit().putBoolean("done_tutorial", true).commit();
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "close drawer");
                        mDrawerLayout.closeDrawer(Gravity.LEFT);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_CLOSE_DRAWER));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "open drawer");
                        mDrawerLayout.openDrawer(Gravity.LEFT);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_OPEN_DRAWER));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "page left");
                        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_PAGE_LEFT));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "page right");
                        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_PAGE_RIGHT));

            startActivity(new Intent(context, TutorialActivity.class));
            overridePendingTransition(0, 0);
        }
    }

    public void setUpWindow() {
        // nothing here, will be overrode
        MainActivity.isPopup = false;

        if ((getIntent().getFlags() & 0x00002000) != 0) {
            MainActivity.isPopup = true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }

        context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));
        sharedPrefs.edit().putBoolean("refresh_me", true).commit();

        overridePendingTransition(0, 0);
        finish();
        Intent restart = new Intent(context, MainActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        restart.putExtra("page_to_open", mViewPager.getCurrentItem());
        overridePendingTransition(0, 0);
        sharedPrefs.edit().putBoolean("should_refresh", false).commit();
        startActivity(restart);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getIntent().getBooleanExtra("from_drawer", false)) {
            int page = getIntent().getIntExtra("page_to_open", 0);
            String title = "" + mSectionsPagerAdapter.getPageTitle(page);
            actionBar.setTitle(title);
            mViewPager.setCurrentItem(page);
        }

        if (getIntent().getBooleanExtra("open_interactions", false)) {
            Log.v("talon_interactions", "should open the drawer");
            mDrawerLayout.openDrawer(Gravity.END);
        }
    }

    @Override
    public void onDestroy() {
        try {
            HomeDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            MentionsDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            DMDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            ListDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            FollowersDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            FavoriteUsersDataSource.getInstance(context).close();
        } catch (Exception e) { }
        try {
            InteractionsDataSource.getInstance(context).close();
        } catch (Exception e) { }

        super.onDestroy();
    }

}