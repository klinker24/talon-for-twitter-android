package com.klinker.android.twitter.ui.drawer_activities.trends;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Window;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.TrendsPagerAdapter;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.LoginActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;

/**
 * Created by luke on 11/29/13.
 */
public class TrendsPager extends DrawerActivity {

    private TrendsPagerAdapter mSectionsPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.trends));

        setContentView(R.layout.trends_activity);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        mSectionsPagerAdapter = new TrendsPagerAdapter(getFragmentManager(), context);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(ViewPager.OVER_SCROLL_NEVER);

        setUpDrawer(3, getResources().getString(R.string.trends));

        mViewPager.setOffscreenPageLimit(3);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, TrendsPager.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}
