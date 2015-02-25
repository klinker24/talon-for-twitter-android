package com.klinker.android.twitter_l.ui;
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.transition.ChangeBounds;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.Toolbar;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.Circle;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.InteractionsDataSource;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.services.CatchupPull;
import com.klinker.android.twitter_l.services.TalonPullNotificationService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.ui.main_fragments.MainFragment;
import com.klinker.android.twitter_l.ui.setup.LoginActivity;
import com.klinker.android.twitter_l.ui.setup.TutorialActivity;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.UpdateUtils;

import at.markushi.ui.CircleButton;

public class MainActivity extends DrawerActivity {

    public static boolean isPopup;
    public static Context sContext;

    public static CircleButton sendButton;
    public static boolean showIsRunning = false;
    public static boolean hideIsRunning = false;
    public static Handler sendHandler;
    public static Runnable showSend = new Runnable() {
        @Override
        public void run() {
            if (sendButton.getVisibility() == View.GONE && !showIsRunning) {

                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_expand);
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

                // should create a circular reveal, but doesn't...
                /*MainActivity.sendLayout.setVisibility(View.INVISIBLE);
                int cx = (MainActivity.sendLayout.getLeft() + MainActivity.sendLayout.getRight()) / 2;
                int cy = (MainActivity.sendLayout.getTop() + MainActivity.sendLayout.getBottom()) / 2;
                int finalRadius = MainActivity.sendLayout.getWidth();
                ValueAnimator anim =
                        ViewAnimationUtils.createCircularReveal(MainActivity.sendLayout, cx, cy, 0, finalRadius);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        MainActivity.sendLayout.setVisibility(View.VISIBLE);
                    }
                });
                anim.start();*/
            }
        }
    };
    public static Runnable hideSend = new Runnable() {
        @Override
        public void run() {
            if (sendButton.getVisibility() == View.VISIBLE && !hideIsRunning) {
                Animation anim = AnimationUtils.loadAnimation(sContext, R.anim.fab_hide);
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
                sendButton.startAnimation(anim);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UpdateUtils.checkUpdate(this);

        MainActivity.sendHandler = new Handler();

        context = this;
        sContext = this;
        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        DrawerActivity.settings = AppSettings.getInstance(context);

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

        MainActivity.sendButton = (CircleButton) findViewById(R.id.send_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setSharedElementExitTransition(new ChangeBounds());
        }

        MainActivity.sendButton.setColor(settings.themeColors.accentColor);

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

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
        }

        mSectionsPagerAdapter = new TimelinePagerAdapter(getFragmentManager(), context, sharedPrefs, getIntent().getBooleanExtra("from_launcher", false), this);
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
                    }
                }
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                showBars();
            }

            public void onPageSelected(int position) {

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

    @Override
    public void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean("open_a_page", false)) {
            sharedPrefs.edit().putBoolean("open_a_page", false).commit();
            int page = sharedPrefs.getInt("open_what_page", 1);
            String title = "" + mSectionsPagerAdapter.getPageTitle(page);
            actionBar.setTitle(title);
            mViewPager.setCurrentItem(page);
        }

        if (sharedPrefs.getBoolean("open_interactions", false)) {
            sharedPrefs.edit().putBoolean("open_interactions", false).commit();
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

    public static boolean caughtstarting = false;

    @Override
    public void onStart() {
        super.onStart();

        MainActivity.isPopup = false;

        Log.v("talon_starting", "main activity starting");

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        // check for night mode switching
        boolean dark = AppSettings.getCurrentTheme(sharedPrefs);

        if (sharedPrefs.getBoolean("launcher_frag_switch", false) ||
                (dark != settings.darkTheme)) {

            sharedPrefs.edit().putBoolean("launcher_frag_switch", false)
                              .putBoolean("dont_refresh", true).commit();

            AppSettings.invalidate();

            Log.v("talon_theme", "no action bar overlay found, recreating");

            finish();
            overridePendingTransition(0, 0);
            startActivity(getRestartIntent());
            overridePendingTransition(0, 0);

            MainActivity.caughtstarting = true;

            // return so that it doesn't start the background refresh, that is what caused the dups.
            sharedPrefs.edit().putBoolean("dont_refresh_on_start", true).commit();
            return;
        } else {
            sharedPrefs.edit().putBoolean("dont_refresh", false)
                              .putBoolean("should_refresh", true).commit();

        }

        if(DrawerActivity.settings.pushNotifications) {
            if (!TalonPullNotificationService.isRunning) {
                context.startService(new Intent(context, TalonPullNotificationService.class));
            }
        } else {
            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
        }

        // cancel the alarm to start the catchup service
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(context, 236, new Intent(context, CatchupPull.class), 0);
        am.cancel(pendingIntent); // cancel the old one, then start the new one in 1 min

        // clear the pull unread
        sharedPrefs.edit().putInt("pull_unread", 0).commit();

        UpdateUtils.checkUpdate(this);

        if (sharedPrefs.getBoolean("force_reverse_click", true)) {
            sharedPrefs.edit().putBoolean("reverse_click_option", false)
                    .putBoolean("force_reverse_click", false)
                    .commit();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                NotificationUtils.sendTestNotification(MainActivity.this);
            }
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