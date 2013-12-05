package com.klinker.android.talon.ui.drawer_activities.trends;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Window;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.TrendsPagerAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.LoginActivity;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;

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

        setUpTheme(true);

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

        setUpDrawer(7, getResources().getString(R.string.trends));

        /*mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        actionBar.setTitle(getResources().getString(R.string.local_trends));
                        break;
                    case 1:
                        actionBar.setTitle(getResources().getString(R.string.world_trends));
                        break;
                }
            }
        });*/

        mViewPager.setOffscreenPageLimit(3);
    }

}
