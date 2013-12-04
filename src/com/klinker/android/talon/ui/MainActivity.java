package com.klinker.android.talon.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.view.WindowManager;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.MainDrawerArrayAdapter;
import com.klinker.android.talon.adapters.TimelinePagerAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.ui.fragments.HomeFragment;

import de.keyboardsurfer.android.widget.crouton.Crouton;

public class MainActivity extends DrawerActivity {

    private TimelinePagerAdapter mSectionsPagerAdapter;

    public static boolean refreshMe;
    public boolean isPopup = false;
    public static boolean fromSettings = false;
    public static boolean popupOpened = false;

    public static boolean refreshHappened = false;

    public static boolean needRecreate = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

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

        setUpDrawer(0, getResources().getString(R.string.timeline));

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (!actionBar.isShowing()) {
                    actionBar.show();
                }
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
        isPopup = false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (getIntent().getBooleanExtra("from_notification", false) || getIntent().getBooleanExtra("from_drawer", false)) {
            refreshMe = false;
        } else {
            refreshMe = true;
        }

        /*int unread = sharedPrefs.getInt("timeline_unread", 0);

        if (HomeFragment.listView.getFirstVisiblePosition() == 0
                && settings.refreshOnStart
                && !fromSettings
                && !getIntent().getBooleanExtra("from_notification", false)
                && !getIntent().getBooleanExtra("from_drawer", false)) {
            refreshMe = true;
        } else {
            refreshMe = false;
        }

        fromSettings = false;*/
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
        } catch (Exception e) {

        }

        Crouton.cancelAllCroutons();

        /*try {
            sharedPrefs.edit().putInt("timeline_unread", HomeFragment.listView.getFirstVisiblePosition()).commit();
        } catch (Exception e) {
            // they haven't logged in yet so there is no listview
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        if (!isPopup && (needRecreate || getIntent().getBooleanExtra("from_notification", false))) {
            recreate();
        }

        needRecreate = false;

        /*if ((popupOpened || (refreshHappened && !getIntent().getBooleanExtra("from_notification", false))) && !getIntent().getBooleanExtra("from_notification", false)) {
            recreate();
        }

        popupOpened = false;
        refreshHappened = false;*/
    }
}