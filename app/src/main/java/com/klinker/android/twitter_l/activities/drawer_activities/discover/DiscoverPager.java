package com.klinker.android.twitter_l.activities.drawer_activities.discover;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TrendsPagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;

public class DiscoverPager extends DrawerActivity {

    private TrendsPagerAdapter mSectionsPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        setUpTheme();
        setContentView(R.layout.trends_activity);
        setUpDrawer(0, getResources().getString(R.string.trends));

        actionBar.setTitle(getResources().getString(R.string.trends));
        actionBar.setElevation(0);

        mSectionsPagerAdapter = new TrendsPagerAdapter(getFragmentManager(), context);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(ViewPager.OVER_SCROLL_NEVER);

        TabLayout strip = (TabLayout) findViewById(R.id.pager_tab_strip);
        strip.setBackgroundColor(settings.themeColors.primaryColor);
        strip.setSelectedTabIndicatorColor(settings.themeColors.accentColor);
        strip.setupWithViewPager(mViewPager);

        if (AppSettings.isWhiteToolbar(this)) {
            strip.setTabTextColors(ColorStateList.valueOf(lightStatusBarIconColor));
        } else {
            strip.setTabTextColors(Color.WHITE, Color.WHITE);
        }

        if (statusBar != null) {
            statusBar.setVisibility(View.GONE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getResources().getBoolean(R.bool.has_drawer)) {
                getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));

                kitkatStatusBar.setVisibility(View.VISIBLE);
                kitkatStatusBar.setBackgroundColor(settings.themeColors.primaryColorDark);
            } else {
                getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            }
        }

        mViewPager.setOffscreenPageLimit(3);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.trends_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final int DISMISS = 0;
        final int SEARCH = 1;
        final int COMPOSE = 2;
        final int NOTIFICATIONS = 3;
        final int DM = 4;
        final int SETTINGS = 5;
        final int TOFIRST = 6;

        menu.getItem(NOTIFICATIONS).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        } catch (Exception e) {

        }

        switch (item.getItemId()) {
            /*case R.id.location_settings:
                Intent settings = new Intent(context, PrefActivity.class);
                settings.putExtra("position", 10)
                        .putExtra("title",
                                getResources().getString(R.string.location_settings));
                finish();
                settings.putExtra("open_help", true);
                startActivity(settings);
                return true;*/

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean changedConfig = false;
    private boolean activityActive = true;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (activityActive) {
            restartActivity();
        } else {
            changedConfig = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (changedConfig) {
            restartActivity();
        }

        activityActive = true;
        changedConfig = false;
    }

    private void restartActivity() {
        overridePendingTransition(0, 0);
        finish();
        Intent restart = new Intent(context, DiscoverPager.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}
