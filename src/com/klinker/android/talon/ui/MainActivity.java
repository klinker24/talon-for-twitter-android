package com.klinker.android.talon.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.Window;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.MainDrawerArrayAdapter;
import com.klinker.android.talon.adapters.TimelinePagerAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;

public class MainActivity extends DrawerActivity {

    private TimelinePagerAdapter mSectionsPagerAdapter;

    public static boolean isPopup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        try {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {

        }

        sharedPrefs.edit().putBoolean("refresh_me", getIntent().getBooleanExtra("from_notification", false)).commit();

        setUpTheme();
        setUpWindow();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.timeline));

        setContentView(R.layout.main_activity);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
        }

        mSectionsPagerAdapter = new TimelinePagerAdapter(
                getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setCurrentItem(2);

        setUpDrawer(0, getResources().getString(R.string.timeline));

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
                if (position > 1) {
                    MainDrawerArrayAdapter.current = position - 2;
                } else {
                    MainDrawerArrayAdapter.current = 0;
                }

                drawerList.invalidateViews();

                switch (position) {
                    case 0:
                        actionBar.setTitle(getResources().getString(R.string.links));
                        break;
                    case 1:
                        actionBar.setTitle(getResources().getString(R.string.pictures));
                        break;
                    case 2:
                        actionBar.setTitle(getResources().getString(R.string.timeline));
                        break;
                    case 3:
                        actionBar.setTitle(getResources().getString(R.string.mentions));
                        break;
                    case 4:
                        actionBar.setTitle(getResources().getString(R.string.direct_messages));
                        break;
                }
            }
        });

        mViewPager.setCurrentItem(getIntent().getIntExtra("page_to_open", 2), false);
        mViewPager.setOffscreenPageLimit(5);
    }

    public void setUpWindow() {
        // nothing here, will be overrode
        isPopup = false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, MainActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        restart.putExtra("page_to_open", mViewPager.getCurrentItem());
        overridePendingTransition(0, 0);
        startActivity(restart);
    }
}