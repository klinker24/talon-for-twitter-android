package com.klinker.android.twitter_l.activities;
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

import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.transition.ChangeBounds;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.services.SendScheduledTweet;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.main_fragments.MainFragment;
import com.klinker.android.twitter_l.activities.setup.material_login.MaterialLogin;
import com.klinker.android.twitter_l.activities.setup.TutorialActivity;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.UpdateUtils;
import com.klinker.android.twitter_l.utils.Utils;


public class MainActivity extends DrawerActivity {

    public static boolean isPopup;
    public static Context sContext;

    public static FloatingActionButton sendButton;
    public static boolean showIsRunning = false;
    public static boolean hideIsRunning = false;
    public static Handler sendHandler;
    public static Runnable showSend = new Runnable() {
        @Override
        public void run() {
            if (sendButton.getVisibility() == View.GONE && !showIsRunning) {

                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_in);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        showIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendButton.setVisibility(View.VISIBLE);
                        showIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                sendButton.startAnimation(anim);
            }
        }
    };
    public static Runnable hideSend = new Runnable() {
        @Override
        public void run() {
            if (sendButton.getVisibility() == View.VISIBLE && !hideIsRunning) {
                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_out);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        hideIsRunning = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendButton.setVisibility(View.GONE);
                        hideIsRunning = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                anim.setDuration(ANIM_DURATION);
                sendButton.startAnimation(anim);
            }
        }
    };

    public void topCurrentFragment() {
        Intent top = new Intent("com.klinker.android.twitter.TOP_TIMELINE");
        top.putExtra("fragment_number", mViewPager.getCurrentItem());
        sendBroadcast(top);
    }

    public void showAwayFromTopToast() {
        Intent toast = new Intent("com.klinker.android.twitter.SHOW_TOAST");
        toast.putExtra("fragment_number", mViewPager.getCurrentItem());
        sendBroadcast(toast);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSettings settings = AppSettings.getInstance(this);
        if (settings.myScreenName == null || settings.myScreenName.isEmpty()) {
            if (settings.currentAccount == 1) {
                settings.sharedPrefs.edit().putInt("current_account", 2).commit();
            } else {
                settings.sharedPrefs.edit().putInt("current_account", 1).commit();
            }

            AppSettings.invalidate();
        }

        UpdateUtils.checkUpdate(this);

        MainActivity.sendHandler = new Handler();

        context = this;
        sContext = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        DrawerActivity.settings = AppSettings.getInstance(context);

        try {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {

        }

        sharedPrefs.edit().putBoolean("refresh_me", getIntent().getBooleanExtra("from_notification", false)).apply();

        setUpTheme();
        setUpWindow();
        setContentView(R.layout.main_activity);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        setUpDrawer(0, getResources().getString(R.string.timeline));

        MainActivity.sendButton = (FloatingActionButton) findViewById(R.id.send_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementExitTransition(new ChangeBounds());
        }

        MainActivity.sendButton.setBackgroundTintList(ColorStateList.valueOf(settings.themeColors.accentColor));

        MainActivity.sendHandler.postDelayed(showSend, 1000);
        MainActivity.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent compose = new Intent(context, ComposeActivity.class);
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                compose.putExtra("already_animated", true);
                startActivity(compose, opts.toBundle());
            }
        });

        if (!Utils.hasNavBar(this) ||
                (!getResources().getBoolean(R.bool.isTablet) &&
                        getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())) {
            MainActivity.sendButton.setTranslationY(Utils.toDP(48, this));
        }

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, MaterialLogin.class);
            startActivity(login);
        }

        mSectionsPagerAdapter = new TimelinePagerAdapter(getFragmentManager(), context, sharedPrefs, getIntent().getBooleanExtra("from_launcher", false));
        int currAccount = sharedPrefs.getInt("current_account", 1);
        int defaultPage = sharedPrefs.getInt("default_timeline_page_" + currAccount, 0);
        actionBar.setTitle(mSectionsPagerAdapter.getPageTitle(defaultPage));

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                    int count = mSectionsPagerAdapter.getCount();
                    for (int i = 0; i < count; i++) {
                        MainFragment f = (MainFragment) mSectionsPagerAdapter.getRealFrag(i);
                        f.allowBackPress();
                        f.resetVideoHandler();
                    }
                }
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                showBars();
            }

            public void onPageSelected(int position) {
                MainFragment f = (MainFragment) mSectionsPagerAdapter.getRealFrag(position);
                f.playCurrentVideos();

                String title = "" + mSectionsPagerAdapter.getPageTitle(position);

                MainDrawerArrayAdapter.setCurrent(context, position);
                drawerList.invalidateViews();

                actionBar.setTitle(title);
            }
        });

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mViewPager.setCurrentItem(defaultPage);
        MainDrawerArrayAdapter.setCurrent(this, defaultPage);

        drawerList.invalidateViews();

        if (getIntent().getBooleanExtra("from_launcher", false)) {
            actionBar.setTitle(mSectionsPagerAdapter.getPageTitle(getIntent().getIntExtra("launcher_page", 0)));
        }

        mViewPager.setOffscreenPageLimit(TimelinePagerAdapter.MAX_EXTRA_PAGES);

        final PermissionModelUtils permissionUtils = new PermissionModelUtils(this);

        if (getIntent().getBooleanExtra("tutorial", false) && !sharedPrefs.getBoolean("done_tutorial", true)) {
            getIntent().putExtra("tutorial", false);
            sharedPrefs.edit().putBoolean("done_tutorial", true).apply();
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

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "tap app bar");
                        toolbar.performClick();
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_TAP_APP_BAR));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "long click app bar");
                        toolbar.performLongClick();
                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_LONG_CLICK_APP_BAR));

            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.v("tutorial_activity", "finished");

                        if (permissionUtils.needPermissionCheck()) {
                            permissionUtils.showPermissionExplanationThenAuthorization();
                        }

                        unregisterReceiver(this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, new IntentFilter(TutorialActivity.ACTION_FINISH_TUTORIAL));

            startActivity(new Intent(context, TutorialActivity.class));
            overridePendingTransition(0, 0);
        } else {
            if (permissionUtils.needPermissionCheck()) {
                permissionUtils.showPermissionExplanationThenAuthorization();
            }
        }

        setLauncherPage();

        if (getIntent().getBooleanExtra("from_drawer", false)) {
            mViewPager.setCurrentItem(getIntent().getIntExtra("page_to_open", 1));
        }

        Log.v("talon_starting", "ending on create");
    }

    public void setLauncherPage() {
        // do nothing here
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
        recreate();
    }

    @Override
    public void onBackPressed() {

        // this will go through all the current fragments and check if one has an expanded item
        int count = mSectionsPagerAdapter.getCount();
        boolean clicked = false;
        for (int i = 0; i < count; i++) {
            MainFragment f = (MainFragment) mSectionsPagerAdapter.getRealFrag(i);

            // we only want it to quit if there is an expanded item and the view pager is currently looking at the
            // page with that expanded item. If they swipe to mentions while something is expanded on the main
            // timeline , then it should still quit if the back button is pressed

            if (!f.allowBackPress() && mViewPager.getCurrentItem() == i) {
                clicked = true;
            }
        }

        if (!clicked) {
            super.onBackPressed();
        }
    }

    private void handleOpenPage() {
        if (sharedPrefs.getBoolean("open_a_page", false)) {
            sharedPrefs.edit().putBoolean("open_a_page", false).apply();
            int page = sharedPrefs.getInt("open_what_page", 1);
            String title = "" + mSectionsPagerAdapter.getPageTitle(page);
            actionBar.setTitle(title);
            mViewPager.setCurrentItem(page);
        }

        if (sharedPrefs.getBoolean("open_interactions", false)) {
            sharedPrefs.edit().putBoolean("open_interactions", false).apply();
            mDrawerLayout.openDrawer(Gravity.END);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleOpenPage();
    }

    @Override
    public void onResume() {
        super.onResume();
        handleOpenPage();

        try {
            int current = mViewPager.getCurrentItem();
            MainFragment currentFragment = (MainFragment) mSectionsPagerAdapter.getRealFrag(current);
            currentFragment.scrollDown();
        } catch (Exception e) {
            
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

    public static boolean caughtstarting = false;

    @Override
    public void onStart() {
        super.onStart();

        MainActivity.isPopup = false;

        Log.v("talon_starting", "main activity starting");

        sharedPrefs = AppSettings.getSharedPreferences(this);

        // check for night mode switching
        int theme = AppSettings.getCurrentTheme(sharedPrefs);

        if (sharedPrefs.getBoolean("launcher_frag_switch", false) ||
                (theme != settings.baseTheme)) {

            sharedPrefs.edit().putBoolean("launcher_frag_switch", false)
                              .putBoolean("dont_refresh", true).apply();

            AppSettings.invalidate();

            Log.v("talon_theme", "no action bar overlay found, recreating");

            finish();
            overridePendingTransition(0, 0);
            startActivity(getRestartIntent());
            overridePendingTransition(0, 0);

            MainActivity.caughtstarting = true;

            // return so that it doesn't start the background refresh, that is what caused the dups.
            sharedPrefs.edit().putBoolean("dont_refresh_on_start", true).apply();
            return;
        } else {
            sharedPrefs.edit().putBoolean("dont_refresh", false)
                              .putBoolean("should_refresh", true).apply();

            MainActivity.caughtstarting = false;
        }

        UpdateUtils.checkUpdate(this);

        if (sharedPrefs.getBoolean("force_reverse_click", true)) {
            sharedPrefs.edit().putBoolean("reverse_click_option", false)
                    .putBoolean("force_reverse_click", false)
                    .apply();
        }

        new Handler().postDelayed(() -> {
            NotificationUtils.sendTestNotification(MainActivity.this);
            SendScheduledTweet.scheduleNextRun(context);
        }, 1000);
    }

    public Intent getRestartIntent() {
        return new Intent(context, MainActivity.class);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (settings.floatingCompose) {
            menu.getItem(2).setVisible(false); // hide the compose button here
        }

        if (settings.tweetmarkerManualOnly) {
            menu.getItem(7).setVisible(true);
        }

        return true;
    }

}
