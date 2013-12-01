package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.MainDrawerArrayAdapter;
import com.klinker.android.talon.adapters.TimelinePagerAdapter;
import com.klinker.android.talon.listeners.MainDrawerClickListener;
import com.klinker.android.talon.manipulations.BlurTransform;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.settings.SettingsPagerActivity;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.ui.fragments.HomeFragment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;

import de.keyboardsurfer.android.widget.crouton.Crouton;

public class MainActivity extends DrawerActivity {

    private TimelinePagerAdapter mSectionsPagerAdapter;

    public static boolean refreshMe;
    public boolean isPopup = false;
    public static boolean fromSettings = false;
    public static boolean popupOpened = false;

    public static boolean refreshHappened = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        setUpTheme();
        setUpWindow();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.timeline));

        setContentView(R.layout.main_activity);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        mSectionsPagerAdapter = new TimelinePagerAdapter(
                getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        setUpDrawer(0, getResources().getString(R.string.timeline));

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                MainDrawerArrayAdapter.current = position;
                drawerList.invalidateViews();

                switch (position) {
                    case 0:
                        actionBar.setTitle(getResources().getString(R.string.timeline));
                        break;
                    case 1:
                        actionBar.setTitle(getResources().getString(R.string.mentions));
                        break;
                    case 2:
                        actionBar.setTitle(getResources().getString(R.string.direct_messages));
                        break;
                }
            }
        });

        mViewPager.setCurrentItem(getIntent().getIntExtra("page_to_open", 0), false);
        mViewPager.setOffscreenPageLimit(3);
    }

    public void setUpWindow() {
        // nothing here, will be overrode
    }

    @Override
    protected void onStart() {
        super.onStart();

        int unread = sharedPrefs.getInt("timeline_unread", 0);

        if (unread == 0
                && settings.refreshOnStart
                && !fromSettings
                && !getIntent().getBooleanExtra("from_notification", false)
                && !getIntent().getBooleanExtra("from_drawer", false)) {
            refreshMe = true;
        } else {
            refreshMe = false;
        }

        fromSettings = false;
    }

    @Override
    protected void onDestroy() {
        Crouton.cancelAllCroutons();

        try {
            sharedPrefs.edit().putInt("timeline_unread", HomeFragment.listView.getFirstVisiblePosition()).commit();
        } catch (Exception e) {
            // they haven't logged in yet so there is no listview
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        if ((popupOpened || (refreshHappened && !getIntent().getBooleanExtra("from_notification", false))) && !getIntent().getBooleanExtra("from_notification", false)) {
            recreate();
        }

        popupOpened = false;
        refreshHappened = false;
    }
}