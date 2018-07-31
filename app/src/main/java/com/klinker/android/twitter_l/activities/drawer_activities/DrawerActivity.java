package com.klinker.android.twitter_l.activities.drawer_activities;
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

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.adapters.InteractionsCursorAdapter;
import com.klinker.android.twitter_l.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.*;
import com.klinker.android.twitter_l.listeners.InteractionClickListener;
import com.klinker.android.twitter_l.listeners.MainDrawerClickListener;
import com.klinker.android.twitter_l.views.NavBarOverlayLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.settings.PrefActivity;
import com.klinker.android.twitter_l.settings.SettingsActivity;
import com.klinker.android.twitter_l.activities.setup.material_login.MaterialLogin;
import com.klinker.android.twitter_l.utils.*;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.activities.compose.ComposeDMActivity;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.views.widgets.ActionBarDrawerToggle;
import com.klinker.android.twitter_l.views.widgets.NotificationDrawerLayout;

import com.klinker.android.twitter_l.utils.XmlFaqUtils;

import de.timroes.android.listview.EnhancedListView;
import me.leolin.shortcutbadger.ShortcutBadger;

import java.lang.reflect.Field;

public abstract class DrawerActivity extends WhiteToolbarActivity implements SystemBarVisibility {

    public static AppSettings settings;
    public Activity context;
    public SharedPreferences sharedPrefs;

    public android.support.v7.app.ActionBar actionBar;

    public static ViewPager mViewPager;
    public TimelinePagerAdapter mSectionsPagerAdapter;

    public NotificationDrawerLayout mDrawerLayout;
    public InteractionsCursorAdapter notificationAdapter;
    public LinearLayout mDrawer;
    public ListView drawerList;
    public EnhancedListView notificationList;
    public ActionBarDrawerToggle mDrawerToggle;

    public ListView listView;

    public boolean logoutVisible = false;
    public static boolean translucent;

    public static boolean canSwitch = true;

    public View kitkatStatusBar;
    public static View statusBar;
    public static int statusBarHeight;
    public static int navBarHeight;

    public int openMailResource;
    public int closedMailResource;
    public static TextView oldInteractions;
    public ImageView readButton;

    private ImageView backgroundPic;
    private ImageView profilePic;

    private LinearLayout noInteractions;

    public Toolbar toolbar = null;
    public static boolean hasToolbar = false;
    public MainDrawerArrayAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        settings = AppSettings.getInstance(this);
        if (getResources().getBoolean(R.bool.has_drawer)) {
            Utils.setUpMainTheme(this, settings);
        } else {
            Utils.setUpTheme(this, settings);
        }

        super.onCreate(savedInstanceState);

        actionBar = getSupportActionBar();
        Utils.setSharedContentTransition(this);
        Utils.setTaskDescription(this);
    }

    private SearchUtils searchUtils;

    public void setUpDrawer(int number, final String actName) {
        int statusBarHeight = Utils.getStatusBarHeight(context);

        View header = findViewById(R.id.header);
        header.getLayoutParams().height = Utils.toDP(144, context) + statusBarHeight;
        header.invalidate();

        View profilePicImage = findViewById(R.id.profile_pic_contact);
        ((RelativeLayout.LayoutParams) profilePicImage.getLayoutParams()).topMargin = Utils.toDP(12, context) + statusBarHeight;
        profilePicImage.invalidate();

        View profilePic2Image = findViewById(R.id.profile_pic_contact_2);
        ((RelativeLayout.LayoutParams) profilePic2Image.getLayoutParams()).topMargin = Utils.toDP(12, context) + statusBarHeight;
        profilePic2Image.invalidate();



        searchUtils = new SearchUtils(this);
        searchUtils.setUpSearch();

        try {
            findViewById(R.id.dividerView).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } catch (Throwable t) {

        }

        int currentAccount = sharedPrefs.getInt("current_account", 1);
        for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
            String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
            int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

            if (type != AppSettings.PAGE_TYPE_NONE) {
                number++;
            }
        }

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        adapter = new MainDrawerArrayAdapter(context);
        MainDrawerArrayAdapter.setCurrent(context, number);

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.drawerIcon});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        noInteractions = (LinearLayout) findViewById(R.id.no_interaction);
        ImageView noInterImage = (ImageView) findViewById(R.id.no_inter_icon);
        noInterImage.getDrawable().setColorFilter(settings.themeColors.primaryColor, PorterDuff.Mode.MULTIPLY);
        noInteractions.setVisibility(View.GONE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            try {
                setSupportActionBar(toolbar);
                DrawerActivity.hasToolbar = true;
            } catch (Exception e) {
                // already has an action bar supplied?? comes when you switch to landscape and back to portrait
                e.printStackTrace();
            }

            int color = AppSettings.isWhiteToolbar(this) ? lightStatusBarIconColor : Color.WHITE;
            toolbar.setTitleTextColor(color);
            toolbar.setNavigationIcon(resource);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                        mDrawerLayout.closeDrawer(Gravity.LEFT);
                    } else {
                        mDrawerLayout.openDrawer(Gravity.LEFT);
                    }
                }
            });

            toolbar.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (DrawerActivity.this instanceof MainActivity) {
                        ((MainActivity) DrawerActivity.this).topCurrentFragment();
                    }
                    return true;
                }
            });

            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DrawerActivity.this instanceof MainActivity) {
                        ((MainActivity) DrawerActivity.this).showAwayFromTopToast();
                    }
                }
            });

            boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            try {
                RelativeLayout.LayoutParams toolParams = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
                toolParams.height = Utils.getActionBarHeight(context);
                if (!getResources().getBoolean(R.bool.isTablet)) {
                    toolParams.topMargin = Utils.getStatusBarHeight(context);
                }
                toolbar.setLayoutParams(toolParams);
            } catch (ClassCastException e) {
                // they are linear layout here
                LinearLayout.LayoutParams toolParams = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
                toolParams.height = Utils.getActionBarHeight(context);
                if (!getResources().getBoolean(R.bool.isTablet)) {
                    toolParams.topMargin = Utils.getStatusBarHeight(context);
                }
                toolbar.setLayoutParams(toolParams);
            }

            toolbar.setBackgroundColor(settings.themeColors.primaryColor);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);

            if (getResources().getBoolean(R.bool.isTablet) && toolbar != null) {
                if (toolbar.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) toolbar.getLayoutParams();
                    params.topMargin = Utils.getStatusBarHeight(context);
                    toolbar.setLayoutParams(params);
                } else if (toolbar.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
                    params.topMargin = Utils.getStatusBarHeight(context);
                    toolbar.setLayoutParams(params);
                }
            }
        }

        actionBar = getSupportActionBar();

        MainDrawerArrayAdapter.current = number;

        a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.read_button});
        openMailResource = a.getResourceId(0, 0);
        a.recycle();

        a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.unread_button});
        closedMailResource = a.getResourceId(0, 0);
        a.recycle();


        mDrawerLayout = (NotificationDrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

        TextView name = (TextView) mDrawer.findViewById(R.id.name);
        TextView screenName = (TextView) mDrawer.findViewById(R.id.screen_name);
        backgroundPic = (ImageView) mDrawer.findViewById(R.id.background_image);
        profilePic = (ImageView) mDrawer.findViewById(R.id.profile_pic_contact);
        ImageView profilePic2 = (ImageView) mDrawer.findViewById(R.id.profile_pic_contact_2);
        final ImageButton showMoreDrawer = (ImageButton) mDrawer.findViewById(R.id.options);
        final LinearLayout logoutLayout = (LinearLayout) mDrawer.findViewById(R.id.logoutLayout);
        final Button logoutDrawer = (Button) mDrawer.findViewById(R.id.logoutButton);
        drawerList = (ListView) mDrawer.findViewById(R.id.drawer_list);
        notificationList = (EnhancedListView) findViewById(R.id.notificationList);

        if (getResources().getBoolean(R.bool.has_drawer)) {
            findViewById(R.id.notification_drawer_ab).setVisibility(View.GONE);
        }

        if (settings.blackTheme) {
            mDrawer.setBackgroundColor(Color.BLACK);
        }

        try {
            mDrawerLayout = (NotificationDrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_rev, Gravity.RIGHT);

            final boolean hasDrawer = getResources().getBoolean(R.bool.has_drawer);

            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    resource,  /* nav drawer icon to replace 'Up' caret */
                    R.string.app_name,  /* "open drawer" description */
                    R.string.app_name  /* "close drawer" description */
            ) {

                public void onDrawerClosed(View view) {
                    activateLightStatusBar(true);

                    actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

                    if (logoutVisible) {
                        /*Animation ranim = AnimationUtils.loadAnimation(context, R.anim.drawer_rotate_back);
                        ranim.setFillAfter(true);
                        showMoreDrawer.startAnimation(ranim);*/

                        logoutLayout.setVisibility(View.GONE);
                        drawerList.setVisibility(View.VISIBLE);

                        logoutVisible = false;
                    }

                    try {
                        if (oldInteractions.getText().toString().equals(getResources().getString(R.string.new_interactions))) {
                            Cursor c = InteractionsDataSource.getInstance(context).getUnreadCursor(DrawerActivity.settings.currentAccount);
                            oldInteractions.setText(getResources().getString(R.string.old_interactions));
                            readButton.setImageResource(openMailResource);
                            notificationList.enableSwipeToDismiss();
                            notificationAdapter = new InteractionsCursorAdapter(context, c);
                            notificationList.setAdapter(notificationAdapter);

                            try {
                                if (c.getCount() == 0) {
                                    noInteractions.setVisibility(View.VISIBLE);
                                } else {
                                    noInteractions.setVisibility(View.GONE);
                                }
                            } catch (Exception e) {

                            }
                        }
                    } catch (Exception e) {
                        // don't have talon pull on
                    }

                    invalidateOptionsMenu();
                }

                public void onDrawerOpened(View drawerView) {
                    //actionBar.setTitle(getResources().getString(R.string.app_name));
                    //actionBar.setIcon(R.mipmap.ic_launcher);

                    activateLightStatusBar(false);

                    searchUtils.hideSearch(false);

                    Cursor c = InteractionsDataSource.getInstance(context).getUnreadCursor(DrawerActivity.settings.currentAccount);
                    try {
                        notificationAdapter = new InteractionsCursorAdapter(context, c);
                        notificationList.setAdapter(notificationAdapter);
                        notificationList.enableSwipeToDismiss();
                        oldInteractions.setText(getResources().getString(R.string.old_interactions));
                        readButton.setImageResource(openMailResource);
                        sharedPrefs.edit().putBoolean("new_notification", false).apply();
                    } catch (Exception e) {
                        // don't have talon pull on
                    }

                    try {
                        if (c.getCount() == 0) {
                            noInteractions.setVisibility(View.VISIBLE);
                        } else {
                            noInteractions.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {

                    }

                    invalidateOptionsMenu();
                }

                public void onDrawerSlide(View drawerView, float slideOffset) {
                    //super.onDrawerSlide(drawerView, slideOffset);

                    if (tranparentSystemBar == -1) {
                        tranparentSystemBar = AppSettings.isWhiteToolbar(DrawerActivity.this) ?
                                getResources().getColor(R.color.light_status_bar_transparent_system_bar) :
                                getResources().getColor(R.color.transparent_system_bar);
                    }

                    /*if (hasDrawer) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            getWindow().setStatusBarColor((Integer) EVALUATOR.evaluate(slideOffset,
                                    (toolbar != null && toolbar.getAlpha() == 1f) ?
                                            settings.themeColors.primaryColorDark : tranparentSystemBar, Color.BLACK));
                        }
                    }*/
                }

                @Override
                public boolean onOptionsItemSelected(MenuItem item) {
                    Log.v("talon_drawer", "item clicked");
                    // Toggle drawer
                    if (item.getItemId() == android.R.id.home) {
                        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                            mDrawerLayout.closeDrawer(Gravity.LEFT);
                        } else {
                            mDrawerLayout.openDrawer(Gravity.LEFT);
                        }
                        return true;
                    }
                    return false;
                }
            };

            mDrawerLayout.setDrawerListener(mDrawerToggle);
        } catch (Exception e) {
            // landscape mode
        }

        actionBar.setHomeButtonEnabled(true);

        showMoreDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (logoutLayout.getVisibility() == View.GONE) {
                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            drawerList.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim.setDuration(300);
                    drawerList.startAnimation(anim);

                    Animation anim2 = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                    anim2.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            logoutLayout.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim2.setDuration(300);
                    logoutLayout.startAnimation(anim2);

                    logoutVisible = true;
                } else {
                    /*Animation ranim = AnimationUtils.loadAnimation(context, R.anim.drawer_rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);*/

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            drawerList.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim.setDuration(300);
                    drawerList.startAnimation(anim);

                    Animation anim2 = AnimationUtils.loadAnimation(context, R.anim.fade_out);
                    anim2.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            logoutLayout.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    anim2.setDuration(300);
                    logoutLayout.startAnimation(anim2);

                    logoutVisible = false;
                }
            }
        });

        logoutDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutFromTwitter();
            }
        });

        final String sName = settings.myName;
        final String sScreenName = settings.myScreenName;
        final String backgroundUrl = settings.myBackgroundUrl;
        final String profilePicUrl = settings.myProfilePicUrl;


        if (!backgroundUrl.equals("")) {
            Glide.with(this).load(backgroundUrl).into(backgroundPic);
        } else {
            Glide.with(this).load(Utils.getBackgroundUrlForTheme(settings)).into(backgroundPic);
        }

        backgroundPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                } catch (Exception e) {

                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ProfilePager.start(context, sName, sScreenName, profilePicUrl);
                    }
                }, 400);
            }
        });

        backgroundPic.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                try {
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                } catch (Exception e) {

                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ProfilePager.start(context, sName, sScreenName, profilePicUrl);
                    }
                }, 400);

                return false;
            }
        });

        try {
            name.setText(sName);
            screenName.setText("@" + sScreenName);
        } catch (Exception e) {
            // 7 inch tablet in portrait
        }

        Glide.with(this).load(profilePicUrl).into(profilePic);

        drawerList.setAdapter(adapter);

        drawerList.setOnItemClickListener(new MainDrawerClickListener(context, mDrawerLayout, mViewPager));

        // set up for the second account
        int count = 0; // number of accounts logged in

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }

        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        final RelativeLayout secondAccount = (RelativeLayout) findViewById(R.id.second_profile);
        TextView name2 = (TextView) findViewById(R.id.name_2);
        TextView screenname2 = (TextView) findViewById(R.id.screen_name_2);
        ImageView proPic2 = (ImageView) findViewById(R.id.profile_pic_2);

        name2.setTextSize(15);
        screenname2.setTextSize(15);

        final int current = sharedPrefs.getInt("current_account", 1);

        // make a second account
        if (count == 1) {
            name2.setText(getResources().getString(R.string.new_account));
            screenname2.setText(getResources().getString(R.string.tap_to_setup));
            secondAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (canSwitch) {
                        if (current == 1) {
                            sharedPrefs.edit().putInt("current_account", 2).apply();
                        } else {
                            sharedPrefs.edit().putInt("current_account", 1).apply();
                        }
                        context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                        context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));

                        Intent login = new Intent(context, MaterialLogin.class);
                        AppSettings.invalidate();
                        finish();
                        startActivity(login);
                    }
                }
            });
        } else { // switch accounts

            if (profilePic2 != null) {
                profilePic2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        secondAccount.performClick();
                    }
                });
            }

            if (current == 1) {
                name2.setText(sharedPrefs.getString("twitter_users_name_2", ""));
                screenname2.setText("@" + sharedPrefs.getString("twitter_screen_name_2", ""));

                if (profilePic2 != null) {
                    Glide.with(this).load(sharedPrefs.getString("profile_pic_url_2", "")).into(profilePic2);
                }
                Glide.with(this).load(sharedPrefs.getString("profile_pic_url_2", "")).into(proPic2);

                secondAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION").putExtra("current_account", current));

                            Toast.makeText(context, "Preparing to switch", Toast.LENGTH_SHORT).show();

                            // we want to wait a second so that the mark position broadcast will work
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sharedPrefs.edit().putInt("current_account", 2).apply();
                                    sharedPrefs.edit().remove("new_notifications").remove("new_retweets").remove("new_favorites").remove("new_followers").apply();
                                    AppSettings.invalidate();
                                    finish();
                                    Intent next = new Intent(context, MainActivity.class);
                                    startActivity(next);
                                }
                            }, 500);
                        }
                    }
                });

            } else {
                name2.setText(sharedPrefs.getString("twitter_users_name_1", ""));
                screenname2.setText("@" + sharedPrefs.getString("twitter_screen_name_1", ""));

                if (profilePic2 != null) {
                    Glide.with(this).load(sharedPrefs.getString("profile_pic_url_1", "")).into(profilePic2);
                }
                Glide.with(this).load(sharedPrefs.getString("profile_pic_url_1", "")).into(proPic2);

                secondAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION").putExtra("current_account", current));

                            Toast.makeText(context, "Preparing to switch", Toast.LENGTH_SHORT).show();

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sharedPrefs.edit().putInt("current_account", 1).apply();
                                    sharedPrefs.edit().remove("new_notifications").remove("new_retweets").remove("new_favorites").remove("new_followers").apply();
                                    AppSettings.invalidate();
                                    finish();
                                    Intent next = new Intent(context, MainActivity.class);
                                    startActivity(next);
                                }
                            }, 500);
                        }
                    }
                });
            }
        }

        statusBar = findViewById(R.id.activity_status_bar);
        try {
            statusBar.setBackgroundColor(settings.themeColors.primaryColorDark);
        } catch (Exception e) {

        }

        statusBarHeight = Utils.getStatusBarHeight(context);

        if (MainActivity.isPopup) {
            toolbar.setTranslationY(-1 * statusBarHeight);
            statusBarHeight = 0;
        }
        
        navBarHeight = Utils.getNavBarHeight(context);

        try {
            RelativeLayout.LayoutParams statusParams = (RelativeLayout.LayoutParams) statusBar.getLayoutParams();
            statusParams.height = statusBarHeight;
            statusBar.setLayoutParams(statusParams);
        } catch (Exception e) {
            try {
                LinearLayout.LayoutParams statusParams = (LinearLayout.LayoutParams) statusBar.getLayoutParams();
                statusParams.height = statusBarHeight;
                statusBar.setLayoutParams(statusParams);
            } catch (Exception x) {
                // in the trends
            }
        }

        View navBarSeperater = findViewById(R.id.nav_bar_seperator);

        if (translucent && Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE || getResources().getBoolean(R.bool.isTablet))) {
            try {
                RelativeLayout.LayoutParams navParams = (RelativeLayout.LayoutParams) navBarSeperater.getLayoutParams();
                navParams.height = navBarHeight;
                navBarSeperater.setLayoutParams(navParams);
            } catch (Exception e) {
                try {
                    LinearLayout.LayoutParams navParams = (LinearLayout.LayoutParams) navBarSeperater.getLayoutParams();
                    navParams.height = navBarHeight;
                    navBarSeperater.setLayoutParams(navParams);
                } catch (Exception x) {
                    // in the trends
                }
            }
        }

        if (translucent) {
            if (getResources().getBoolean(R.bool.options_drawer)) {
                View options = getLayoutInflater().inflate(R.layout.drawer_options, null, false);
                drawerList.addFooterView(options);
            }

            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                drawerList.addFooterView(footer);
                drawerList.setFooterDividersEnabled(false);
            }

            try {
                View drawerStatusBar = findViewById(R.id.drawer_status_bar);
                RelativeLayout.LayoutParams status2Params = (RelativeLayout.LayoutParams) drawerStatusBar.getLayoutParams();
                status2Params.height = statusBarHeight;
                drawerStatusBar.setLayoutParams(status2Params);

                drawerStatusBar.setVisibility(View.VISIBLE);
            } catch (Exception e) {

            }

            if (statusBar != null && !getResources().getBoolean(R.bool.isTablet)) {
                statusBar.setVisibility(View.VISIBLE);
            }

            View drawerStatusBar = findViewById(R.id.drawer_status_bar_2);
            LinearLayout.LayoutParams status2Params = (LinearLayout.LayoutParams) drawerStatusBar.getLayoutParams();
            status2Params.height = statusBarHeight;
            drawerStatusBar.setLayoutParams(status2Params);
            drawerStatusBar.setVisibility(View.VISIBLE);
        }

        try {
            mDrawerLayout.setDrawerLockMode(NotificationDrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        } catch (Exception e) {
            // no drawer?
        }

        kitkatStatusBar = findViewById(R.id.kitkat_status_bar);

        if (kitkatStatusBar != null) {
            try {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) kitkatStatusBar.getLayoutParams();
                params.height = Utils.getStatusBarHeight(context);
                kitkatStatusBar.setLayoutParams(params);
            } catch (Exception e) {
                // frame layout on discover
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) kitkatStatusBar.getLayoutParams();
                params.height = Utils.getStatusBarHeight(context);
                kitkatStatusBar.setLayoutParams(params);
            }
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            if (kitkatStatusBar != null) {
                kitkatStatusBar.setVisibility(View.VISIBLE);
                kitkatStatusBar.setBackgroundColor(getResources().getColor(android.R.color.black));
            }

            if (statusBar != null) {
                statusBar.setBackgroundColor(getResources().getColor(android.R.color.black));
            }

            View status = findViewById(R.id.drawer_status_bar);
            status.setBackgroundColor(getResources().getColor(android.R.color.transparent));

            status = findViewById(R.id.drawer_status_bar_2);
            status.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }


        Switch nightModeSwitch = (Switch) findViewById(R.id.night_mode_switch);
        if (nightModeSwitch != null) {
            nightModeSwitch.setChecked(sharedPrefs.getBoolean("night_mode", false));
        }
    }

    public void onNightModeClicked(View v) {
        context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));

        Switch nightModeSwitch = (Switch) findViewById(R.id.night_mode_switch);
        boolean wasNightMode = nightModeSwitch.isChecked();

        nightModeSwitch.setChecked(!nightModeSwitch.isChecked());
        mDrawerLayout.closeDrawer(Gravity.START);

        if (wasNightMode) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("night_mode", false).commit();
            sharedPrefs.edit().putBoolean("night_mode", false)
                    .putInt("night_start_hour", 22)
                    .commit();
        } else {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean("night_mode", true).commit();
            sharedPrefs.edit().putBoolean("night_mode", true)
                    .putInt("night_start_hour", -1)
                    .commit();
        }

        nightModeSwitch.postDelayed(new Runnable() {
            @Override
            public void run() {
                AppSettings.invalidate();

                finish();
                overridePendingTransition(0, 0);
                startActivity(new Intent(DrawerActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
            }
        }, 400);

    }

    public void onSettingsClicked(View v) {
        context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));
        Intent settings = new Intent(context, SettingsActivity.class);
        finish();
        sharedPrefs.edit().putBoolean("should_refresh", false).apply();
        //overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);
        startActivity(settings);
    }

    public void onHelpClicked(View v) {
        context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));
        sharedPrefs.edit().putBoolean("should_refresh", false).apply();
        Intent settings = new Intent(context, PrefActivity.class);
        settings.putExtra("position", 10)
                .putExtra("title",
                        getResources().getString(R.string.get_help_settings));
        settings.putExtra("open_help", true);
        startActivity(settings);
    }

    public void onFeedbackClicked(View v) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.faq)
                .setMessage(R.string.faq_first)
                .setPositiveButton("FAQ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XmlFaqUtils.showFaqDialog(context);
                    }
                })
                .setNegativeButton(R.string.contact, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showContactUsDialog();
                    }
                })
                .setNeutralButton(R.string.follow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showFollowDialog();
                    }
                })
                .create()
                .show();
    }

    private void showContactUsDialog() {
        new AlertDialog.Builder(context)
                .setItems(new CharSequence[]{"Twitter", "Google+", "Email"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (i == 0) {
                            final Intent tweet = new Intent(context, ComposeActivity.class);
                            new AlertDialog.Builder(context)
                                    .setItems(new CharSequence[]{"@TalonAndroid", "@lukeklinker"}, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (i == 0) {
                                                tweet.putExtra("user", "@TalonAndroid");
                                            } else {
                                                tweet.putExtra("user", "@lukeklinker");
                                            }
                                            startActivity(tweet);
                                        }
                                    })
                                    .create()
                                    .show();
                        } else if (i == 1) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://goo.gl/KCXlZk")));
                        } else {
                            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"luke@klinkerapps.com"});
                            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Talon (Plus)");
                            emailIntent.setType("plain/text");

                            startActivity(emailIntent);
                        }
                    }
                })
                .create()
                .show();
    }

    public void showFollowDialog() {
        new AlertDialog.Builder(context)
                .setItems(new CharSequence[]{"@TalonAndroid", "@lukeklinker", "Luke's Google+"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (i == 0) {
                            // talon
                            ProfilePager.start(context, "Talon", "TalonAndroid", null);
                        } else if (i == 1) {
                            // luke (twitter)
                            ProfilePager.start(context, "Luke Klinker", "lukeklinker", null);
                        } else {
                            // luke (google+)
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://google.com/+LukeKlinker")));
                        }

                    }
                })
                .create()
                .show();
    }

    public void setUpTweetTheme() {
        setUpTheme();
        Utils.setUpTweetTheme(context, settings);
    }

    public void setUpTheme() {

        if (settings.uiExtras && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE ||
                getResources().getBoolean(R.bool.isTablet)) && !MainActivity.isPopup) {
            translucent = true;

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            try {
                int immersive = android.provider.Settings.System.getInt(getContentResolver(), "immersive_mode");

                if (immersive == 1) {
                    translucent = false;
                }
            } catch (Exception e) {
            }
        } else if (settings.uiExtras && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                !getResources().getBoolean(R.bool.isTablet)) && !MainActivity.isPopup) {
            translucent = true;

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

            try {
                int immersive = android.provider.Settings.System.getInt(getContentResolver(), "immersive_mode");

                if (immersive == 1) {
                    translucent = false;
                }
            } catch (Exception e) {
            }
        } else {
            translucent = false;
        }
    }

    NavBarOverlayLayout navOverlay;

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        try {
            mDrawerToggle.syncState();
        } catch (Exception e) {
            // landscape mode
        }

        if (translucent) { // want to check translucent since some devices disable softkeys...
            if (!settings.transpartSystemBars) {
                if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())) {
                    navOverlay = new NavBarOverlayLayout(this);
                    navOverlay.show();
                }
            }

            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT) {
                if (!settings.transpartSystemBars) {
                    tranparentSystemBar = AppSettings.getInstance(this).themeColors.primaryColorDark;
                } else {
                    tranparentSystemBar = AppSettings.isWhiteToolbar(this) ?
                            getResources().getColor(R.color.light_status_bar_transparent_system_bar) :
                            getResources().getColor(R.color.transparent_system_bar);
                }
                statusColor = AppSettings.getInstance(this).themeColors.primaryColorDark;
            } else {
                if (!settings.transpartSystemBars) {
                    tranparentSystemBar = getResources().getColor(android.R.color.black);
                } else {
                    tranparentSystemBar = getResources().getColor(android.R.color.transparent);
                }
                statusColor = getResources().getColor(android.R.color.black);
            }
        } else {
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT) {
                tranparentSystemBar = AppSettings.isWhiteToolbar(this) ?
                        getResources().getColor(R.color.light_status_bar_transparent_system_bar) :
                        getResources().getColor(R.color.transparent_system_bar);
                statusColor = AppSettings.getInstance(this).themeColors.primaryColorDark;
            } else {
                tranparentSystemBar = getResources().getColor(android.R.color.transparent);
                statusColor = getResources().getColor(android.R.color.black);
            }
        }
    }

    private void logoutFromTwitter() {

        context.sendBroadcast(new Intent("com.klinker.android.STOP_PUSH_SERVICE"));

        int currentAccount = sharedPrefs.getInt("current_account", 1);
        boolean login1 = sharedPrefs.getBoolean("is_logged_in_1", false);
        boolean login2 = sharedPrefs.getBoolean("is_logged_in_2", false);

        // Delete the data for the logged out account
        SharedPreferences.Editor e = sharedPrefs.edit();
        e.remove("authentication_token_" + currentAccount);
        e.remove("authentication_token_secret_" + currentAccount);
        e.remove("is_logged_in_" + currentAccount);
        e.remove("new_notification");
        e.remove("new_retweets");
        e.remove("new_favorites");
        e.remove("new_followers");
        e.remove("new_quotes");
        e.remove("current_position_" + currentAccount);
        e.remove("last_activity_refresh_" + currentAccount);
        e.remove("original_activity_refresh_" + currentAccount);
        e.remove("activity_follower_count_" + currentAccount);
        e.remove("activity_latest_followers_" + currentAccount);
        e.apply();

        HomeDataSource homeSources = HomeDataSource.getInstance(context);
        homeSources.deleteAllTweets(currentAccount);

        MentionsDataSource mentionsSources = MentionsDataSource.getInstance(context);
        mentionsSources.deleteAllTweets(currentAccount);

        DMDataSource dmSource = DMDataSource.getInstance(context);
        dmSource.deleteAllTweets(currentAccount);

        InteractionsDataSource inters = InteractionsDataSource.getInstance(context);
        inters.deleteAllInteractions(currentAccount);

        ActivityDataSource activity = ActivityDataSource.getInstance(context);
        activity.deleteAll(currentAccount);

        FavoriteTweetsDataSource favTweets = FavoriteTweetsDataSource.getInstance(context);
        favTweets.deleteAllTweets(currentAccount);

        try {
            long account1List1 = sharedPrefs.getLong("account_" + currentAccount + "_list_1", 0l);
            long account1List2 = sharedPrefs.getLong("account_" + currentAccount + "_list_2", 0l);

            ListDataSource list = ListDataSource.getInstance(context);
            list.deleteAllTweets(account1List1);
            list.deleteAllTweets(account1List2);
        } catch (Exception x) {

        }

        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
        suggestions.clearHistory();

        AppSettings.invalidate();

        if (currentAccount == 1 && login2) {
            e.putInt("current_account", 2).apply();
            finish();
            Intent next = new Intent(context, MainActivity.class);
            startActivity(next);
        } else if (currentAccount == 2 && login1) {
            e.putInt("current_account", 1).apply();
            finish();
            Intent next = new Intent(context, MainActivity.class);
            startActivity(next);
        } else { // only the one account
            e.putInt("current_account", 1).apply();
            finish();
            Intent login = new Intent(context, MaterialLogin.class);
            startActivity(login);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        if (sharedPrefs.getBoolean("remake_me", false) && !MainActivity.isPopup) {
            sharedPrefs.edit().putBoolean("remake_me", false).apply();
            recreate();

            sharedPrefs.edit().putBoolean("launcher_frag_switch", false)
                    .putBoolean("dont_refresh", true).apply();

            return;
        }

        // clear interactions notifications
        sharedPrefs.edit().putInt("new_retweets", 0).commit();
        sharedPrefs.edit().putInt("new_favorites", 0).commit();
        sharedPrefs.edit().putInt("new_followers", 0).commit();
        sharedPrefs.edit().putInt("new_quotes", 0).commit();
        sharedPrefs.edit().putString("old_interaction_text", "").commit();

        invalidateOptionsMenu();
    }

    @TargetApi(Build.VERSION_CODES.N) @Override
    public void onMultiWindowModeChanged(boolean isMultiWindow) {
        super.onMultiWindowModeChanged(isMultiWindow);

        Log.v("talon_multiwindow", "is multi window: " + isMultiWindow);
        if (isMultiWindow) {
            if (navOverlay != null) {
                navOverlay.hide();
            }
        } else {
            recreate();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (settings.autoDismissNotifications) {
            // cancels the notifications when the app is opened
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        cancelTeslaUnread();

        if (translucent && !settings.transpartSystemBars) {
            if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode())) {
                if (navOverlay == null) {
                    navOverlay = new NavBarOverlayLayout(this);
                }

                if (!navOverlay.isShowing()) {
                    navOverlay.show();
                }
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
            if (navOverlay == null) {
                navOverlay = new NavBarOverlayLayout(this);
            }

            if (navOverlay.isShowing()) {
                navOverlay.hide();
            }
        }

        if (settings.autoDismissNotifications) {
            // cancels the notifications when the app is opened
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
        }

        SharedPreferences.Editor e = sharedPrefs.edit();
        e.putInt("new_followers", 0);
        e.putInt("new_favorites", 0);
        e.putInt("new_retweets", 0);
        e.putString("old_interaction_text", "");
        e.apply();

        DrawerActivity.settings = AppSettings.getInstance(context);

        try {
            mDrawerLayout.setDrawerLockMode(NotificationDrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        } catch (Exception x) {
            // no drawer?
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final int DISMISS = 0;
        final int SEARCH = 1;
        final int COMPOSE = 2;
        final int NOTIFICATIONS = 3;
        final int DM = 4;
        final int SETTINGS = 5;
        final int TOFIRST = 6;
        final int TWEETMARKER = 7;

        menu.getItem(TWEETMARKER).setVisible(false);

        if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT) || sharedPrefs.getBoolean("open_interactions", false)) {
            menu.getItem(DISMISS).setVisible(true);

            menu.getItem(SEARCH).setVisible(false);
            menu.getItem(COMPOSE).setVisible(false);
            menu.getItem(DM).setVisible(false);
            menu.getItem(TOFIRST).setVisible(false);
            menu.getItem(NOTIFICATIONS).setVisible(false);

        } else {
            menu.getItem(DISMISS).setVisible(false);

            menu.getItem(SEARCH).setVisible(true);
            menu.getItem(COMPOSE).setVisible(true);
            menu.getItem(DM).setVisible(true);
            menu.getItem(NOTIFICATIONS).setVisible(false);
        }

        // to first button in overflow instead of the toast
        if (MainDrawerArrayAdapter.current > adapter.pageTypes.size() || (settings.uiExtras && settings.useToast)) {
            menu.getItem(TOFIRST).setVisible(false);
        } else {
            menu.getItem(TOFIRST).setVisible(true);
        }

        if (MainActivity.isPopup) {
            menu.getItem(SETTINGS).setVisible(false); // hide the settings button if the popup is up
            menu.getItem(SEARCH).setVisible(false); // hide the search button in popup

            // disable the left drawer so they can't switch activities in the popup.
            // causes problems with the layouts
            mDrawerLayout.setDrawerLockMode(NotificationDrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);

            if (toolbar != null) {
                toolbar.setNavigationIcon(null);
            }
        }

        noti = menu.getItem(NOTIFICATIONS);

        if (getResources().getBoolean(R.bool.options_drawer)) {
            menu.getItem(SETTINGS).setVisible(false);
        }

        if (InteractionsDataSource.getInstance(context).getUnreadCount(settings.currentAccount) > 0) {
            setNotificationFilled(true);
        } else {
            setNotificationFilled(false);
        }

        menu.getItem(DM).setVisible(false);
        menu.getItem(DISMISS).setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }

    public MenuItem noti;

    public void setNotificationFilled(boolean isFilled) {
        if (isFilled) {
            noti.setIcon(getResources().getDrawable(R.drawable.ic_action_notification_dark));
        } else {
            noti.setIcon(getResources().getDrawable(R.drawable.ic_action_notification_none_dark));
        }
    }

    public static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                } else {
                    mDrawerLayout.openDrawer(Gravity.LEFT);
                }
                return super.onOptionsItemSelected(item);
            case R.id.menu_search:
                searchUtils.showSearchView();
                return super.onOptionsItemSelected(item);

            case R.id.menu_compose:
                Intent compose = new Intent(context, ComposeActivity.class);
                sharedPrefs.edit().putBoolean("from_notification_bool", false).apply();
                startActivity(compose);
                return super.onOptionsItemSelected(item);

            case R.id.menu_direct_message:
                Intent dm = new Intent(context, ComposeDMActivity.class);
                sharedPrefs.edit().putBoolean("from_notification_bool", false).apply();
                startActivity(dm);
                return super.onOptionsItemSelected(item);

            case R.id.menu_settings:
                context.sendBroadcast(new Intent("com.klinker.android.twitter.MARK_POSITION"));
                Intent settings = new Intent(context, SettingsActivity.class);
                finish();
                sharedPrefs.edit().putBoolean("should_refresh", false).apply();
                //overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);
                startActivity(settings);
                return super.onOptionsItemSelected(item);

            case R.id.menu_dismiss:
                dismissNotifications();

                return super.onOptionsItemSelected(item);

            case R.id.menu_notifications:
                if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    mDrawerLayout.closeDrawer(Gravity.LEFT);
                }

                if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                    mDrawerLayout.closeDrawer(Gravity.RIGHT);
                } else {
                    mDrawerLayout.openDrawer(Gravity.RIGHT);
                }

                return super.onOptionsItemSelected(item);

            case R.id.menu_to_first:
                context.sendBroadcast(new Intent("com.klinker.android.twitter.TOP_TIMELINE"));
                return super.onOptionsItemSelected(item);

            case R.id.menu_tweetmarker:
                context.sendBroadcast(new Intent("com.klinker.android.twitter.TWEETMARKER"));
                return super.onOptionsItemSelected(item);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void dismissNotifications() {
        InteractionsDataSource data = InteractionsDataSource.getInstance(context);
        data.markAllRead(DrawerActivity.settings.currentAccount);
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
        Cursor c = data.getUnreadCursor(DrawerActivity.settings.currentAccount);
        notificationAdapter = new InteractionsCursorAdapter(context, c);
        notificationList.setAdapter(notificationAdapter);

        try {
            if (c.getCount() == 0 && noInteractions.getVisibility() != View.VISIBLE) {
                noInteractions.setVisibility(View.VISIBLE);
                noInteractions.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
            } else if (noInteractions.getVisibility() != View.GONE) {
                noInteractions.setVisibility(View.GONE);
                noInteractions.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_out));
            }
        } catch (Exception e) {

        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void cancelTeslaUnread() {

        ShortcutBadger.removeCount(context);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ContentValues cv = new ContentValues();
                    cv.put("tag", "com.klinker.android.twitter_l/com.klinker.android.twitter_l.ui.MainActivity");
                    cv.put("count", 0); // back to zero

                    context.getContentResolver().insert(Uri
                                    .parse("content://com.teslacoilsw.notifier/unread_count"),
                            cv);
                } catch (IllegalArgumentException ex) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    int abOffset = -1;

    public void showBars() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getResources().getBoolean(R.bool.has_drawer)) {
                getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));
                if (statusBar != null && statusBar.getVisibility() != View.VISIBLE) {
                    statusBar.setVisibility(View.VISIBLE);
                }
            } else {
                getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            }
        }

        if (abOffset == -1) {
            abOffset = Utils.getStatusBarHeight(context) + Utils.getActionBarHeight(context);
        }

        if (toolbar == null || toolbar.getVisibility() == View.VISIBLE) {
            return;
        }

        ValueAnimator showStatus = ValueAnimator.ofInt(tranparentSystemBar, statusColor);
        showStatus.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                if (statusBar != null) {
                    statusBar.setBackgroundColor(val);
                }
            }
        });
        showStatus.setDuration(ANIM_DURATION);
        showStatus.setEvaluator(EVALUATOR);
        showStatus.start();


        if (toolbar != null && !MainActivity.isPopup) {
            if (toolBarVis == null) {
                toolBarVis = new Handler();
            }
            toolBarVis.removeCallbacksAndMessages(null);
            toolbar.setVisibility(View.VISIBLE);

            ValueAnimator showToolbar = ValueAnimator.ofInt(-1 * abOffset, 0);
            showToolbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int val = (Integer) animation.getAnimatedValue();
                    toolbar.setTranslationY(val);
                }
            });
            ObjectAnimator showToolbarAlpha = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 0f, 1f);

            showToolbar.setDuration(ANIM_DURATION);
            showToolbarAlpha.setDuration(ANIM_DURATION);

            showToolbar.setInterpolator(INTERPOLATOR);
            showToolbarAlpha.setInterpolator(INTERPOLATOR);

            //showToolbarAlpha.setEvaluator(EVALUATOR);

            showToolbar.start();
            showToolbarAlpha.start();
        }
    }

    private int tranparentSystemBar = -1;
    private int statusColor = -1;
    private ArgbEvaluator EVALUATOR = new ArgbEvaluator();
    public static final int ANIM_DURATION = 350;
    public static TimeInterpolator INTERPOLATOR = new DecelerateInterpolator();

    /*static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Path path = new Path();
            path.lineTo(0.25f, 0.25f);
            path.moveTo(0.25f, 0.5f);
            path.lineTo(1f, 1f);

            INTERPOLATOR = new PathInterpolator(path);
        } else {
            INTERPOLATOR = new DecelerateInterpolator();
        }
    }*/

    Handler toolBarVis;

    public void hideBars() {

        if (getResources().getBoolean(R.bool.has_drawer) && statusBar != null && statusBar.getVisibility() != View.VISIBLE) {
            statusBar.setVisibility(View.VISIBLE);
        }

        if (toolbar == null || toolbar.getVisibility() == View.GONE) {
            Log.v("talon_app_bar", "toolbar is null");
            return;
        }

        /*if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            toolbar.setElevation(0);
        }*/

        ValueAnimator hideStatus = ValueAnimator.ofInt(statusColor, tranparentSystemBar);
        hideStatus.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                if (statusBar != null) statusBar.setBackgroundColor(val);
            }
        });
        hideStatus.setDuration(ANIM_DURATION);
        hideStatus.setEvaluator(EVALUATOR);
        hideStatus.start();

        if (toolbar != null && !MainActivity.isPopup) {
            ValueAnimator hideToolbar = ValueAnimator.ofInt(0, -1 * abOffset);
            hideToolbar.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int val = (Integer) animation.getAnimatedValue();
                    toolbar.setTranslationY(val);
                }
            });
            ObjectAnimator hideToolbarAlpha = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 1f, 0f);

            hideToolbar.setDuration(ANIM_DURATION);
            hideToolbarAlpha.setDuration(ANIM_DURATION);

            hideToolbar.setInterpolator(INTERPOLATOR);
            hideToolbarAlpha.setInterpolator(INTERPOLATOR);

            //hideToolbarAlpha.setEvaluator(EVALUATOR);

            hideToolbar.start();
            hideToolbarAlpha.start();

            if (toolBarVis == null) {
                toolBarVis = new Handler();
            }
            toolBarVis.removeCallbacksAndMessages(null);
            toolBarVis.postDelayed(new Runnable() {
                @Override
                public void run() {
                    toolbar.setVisibility(View.GONE);
                }
            }, ANIM_DURATION);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (searchUtils.isShowing()) {
                searchUtils.hideSearch(true);
            } else if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
                mDrawerLayout.closeDrawer(GravityCompat.END);
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
