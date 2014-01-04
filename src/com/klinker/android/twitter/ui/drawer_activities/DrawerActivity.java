package com.klinker.android.twitter.ui.drawer_activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.listeners.MainDrawerClickListener;
import com.klinker.android.twitter.manipulations.MySuggestionsProvider;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.settings.SettingsPagerActivity;
import com.klinker.android.twitter.ui.compose.ComposeActivity;
import com.klinker.android.twitter.ui.compose.ComposeDMActivity;
import com.klinker.android.twitter.ui.LoginActivity;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class DrawerActivity extends Activity {

    public static AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

    public ActionBar actionBar;

    public static ViewPager mViewPager;

    public DrawerLayout mDrawerLayout;
    public LinearLayout mDrawer;
    public ListView drawerList;
    public ActionBarDrawerToggle mDrawerToggle;

    public AsyncListView listView;

    public boolean logoutVisible = false;
    public static boolean translucent;

    public static boolean canSwitch = true;

    public static View statusBar;
    public static int statusBarHeight;
    public static int navBarHeight;

    public void setUpDrawer(int number, final String actName) {

        MainDrawerArrayAdapter.current = number;

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.drawerIcon});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

        HoloTextView name = (HoloTextView) mDrawer.findViewById(R.id.name);
        HoloTextView screenName = (HoloTextView) mDrawer.findViewById(R.id.screen_name);
        final NetworkedCacheableImageView backgroundPic = (NetworkedCacheableImageView) mDrawer.findViewById(R.id.background_image);
        final NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) mDrawer.findViewById(R.id.profile_pic);
        final ImageButton showMoreDrawer = (ImageButton) mDrawer.findViewById(R.id.options);
        final LinearLayout logoutLayout = (LinearLayout) mDrawer.findViewById(R.id.logoutLayout);
        final Button logoutDrawer = (Button) mDrawer.findViewById(R.id.logoutButton);
        drawerList = (ListView) mDrawer.findViewById(R.id.drawer_list);

        try {
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    resource,  /* nav drawer icon to replace 'Up' caret */
                    R.string.app_name,  /* "open drawer" description */
                    R.string.app_name  /* "close drawer" description */
            ) {

                public void onDrawerClosed(View view) {
                    if (logoutVisible) {
                        Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                        ranim.setFillAfter(true);
                        showMoreDrawer.startAnimation(ranim);

                        logoutLayout.setVisibility(View.GONE);
                        drawerList.setVisibility(View.VISIBLE);

                        logoutVisible = false;
                    }

                    if (MainDrawerArrayAdapter.current > 2) {
                        actionBar.setTitle(actName);
                    } else {
                            int position = mViewPager.getCurrentItem();

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

                }

                public void onDrawerOpened(View drawerView) {
                    actionBar.setTitle(getResources().getString(R.string.app_name));
                }

                public void onDrawerSlide(View drawerView, float slideOffset) {
                    super.onDrawerSlide(drawerView, slideOffset);

                    if (!actionBar.isShowing()) {
                        actionBar.show();
                    }

                    if (translucent) {
                        statusBar.setVisibility(View.VISIBLE);
                    }
                }
            };

            // Set the drawer toggle as the DrawerListener
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        } catch (Exception e) {
            // landscape mode
        }

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        showMoreDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(logoutLayout.getVisibility() == View.GONE) {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    drawerList.setVisibility(View.GONE);
                    logoutLayout.setVisibility(View.VISIBLE);

                    logoutVisible = true;
                } else {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    logoutLayout.setVisibility(View.GONE);
                    drawerList.setVisibility(View.VISIBLE);

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

        backgroundPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    mDrawerLayout.closeDrawer(Gravity.START);
                } catch (Exception e) {

                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent viewProfile = new Intent(context, UserProfileActivity.class);
                        viewProfile.putExtra("name", sName);
                        viewProfile.putExtra("screenname", sScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);
                        viewProfile.putExtra("long_click", false);

                        context.startActivity(viewProfile);
                    }
                }, 400);

            }
        });

        backgroundPic.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                try {
                    mDrawerLayout.closeDrawer(Gravity.START);
                } catch (Exception e) {

                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent viewProfile = new Intent(context, UserProfileActivity.class);
                        viewProfile.putExtra("name", sName);
                        viewProfile.putExtra("screenname", sScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);
                        viewProfile.putExtra("long_click", true);

                        context.startActivity(viewProfile);
                    }
                }, 400);

                return false;
            }
        });

        name.setText(sName);
        screenName.setText("@" + sScreenName);
        name.setTextSize(15);
        screenName.setTextSize(15);

        if (!backgroundUrl.equals("")) {
            backgroundPic.loadImage(backgroundUrl, false, null, NetworkedCacheableImageView.BLUR);
        } else {
            Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.default_header_background);
            backgroundPic.setImageBitmap(ImageUtils.blur(b));
        }

        try {
            if(settings.roundContactImages) {
                profilePic.loadImage(profilePicUrl, false, null, NetworkedCacheableImageView.CIRCLE);
            } else {
                profilePic.loadImage(profilePicUrl, false, null);
            }
        } catch (Exception e) {
            // empty path again
        }

        MainDrawerArrayAdapter adapter = new MainDrawerArrayAdapter(context, new ArrayList<String>(Arrays.asList(MainDrawerArrayAdapter.getItems(context))));
        drawerList.setAdapter(adapter);

        drawerList.setOnItemClickListener(new MainDrawerClickListener(context, mDrawerLayout, mViewPager, settings.extraPages));

        // set up for the second account
        int count = 0; // number of accounts logged in

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }

        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        RelativeLayout secondAccount = (RelativeLayout) findViewById(R.id.second_profile);
        HoloTextView name2 = (HoloTextView) findViewById(R.id.name_2);
        HoloTextView screenname2 = (HoloTextView) findViewById(R.id.screen_name_2);
        NetworkedCacheableImageView proPic2 = (NetworkedCacheableImageView) findViewById(R.id.profile_pic_2);

        name2.setTextSize(15);
        screenname2.setTextSize(15);

        final int current = sharedPrefs.getInt("current_account", 1);

        if(count == 1){
            name2.setText(getResources().getString(R.string.new_account));
            screenname2.setText(getResources().getString(R.string.tap_to_setup));
            secondAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (canSwitch) {
                        if (current == 1) {
                            sharedPrefs.edit().putInt("current_account", 2).commit();
                        } else {
                            sharedPrefs.edit().putInt("current_account", 1).commit();
                        }
                        context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

                        Intent login = new Intent(context, LoginActivity.class);
                        finish();
                        startActivity(login);
                    }
                }
            });
        } else { // count is 2
            if (current == 1) {
                name2.setText(sharedPrefs.getString("twitter_users_name_2", ""));
                screenname2.setText("@" + sharedPrefs.getString("twitter_screen_name_2", ""));
                try {
                    if(settings.roundContactImages) {
                        proPic2.loadImage(sharedPrefs.getString("profile_pic_url_2", ""), true, null, NetworkedCacheableImageView.CIRCLE);
                    } else {
                        proPic2.loadImage(sharedPrefs.getString("profile_pic_url_2", ""), true, null);
                    }
                } catch (Exception e) {

                }

                secondAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

                            sharedPrefs.edit().putInt("current_account", 2).commit();
                            finish();
                            Intent next = new Intent(context, MainActivity.class);
                            startActivity(next);
                        }
                    }
                });
            } else {
                name2.setText(sharedPrefs.getString("twitter_users_name_1", ""));
                screenname2.setText("@" + sharedPrefs.getString("twitter_screen_name_1", ""));
                try {
                    if(settings.roundContactImages) {
                        proPic2.loadImage(sharedPrefs.getString("profile_pic_url_1", ""), true, null, NetworkedCacheableImageView.CIRCLE);
                    } else {
                        proPic2.loadImage(sharedPrefs.getString("profile_pic_url_1", ""), true, null);
                    }
                } catch (Exception e) {

                }
                secondAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

                            sharedPrefs.edit().putInt("current_account", 1).commit();
                            finish();
                            Intent next = new Intent(context, MainActivity.class);
                            startActivity(next);
                        }
                    }
                });
            }
        }

        statusBar = findViewById(R.id.activity_status_bar);

        statusBarHeight = Utils.getStatusBarHeight(context);
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

        if (translucent) {
            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                drawerList.addFooterView(footer);
                drawerList.setFooterDividersEnabled(false);
            }

            View drawerStatusBar = findViewById(R.id.drawer_status_bar);
            LinearLayout.LayoutParams status2Params = (LinearLayout.LayoutParams) drawerStatusBar.getLayoutParams();
            status2Params.height = statusBarHeight;
            drawerStatusBar.setLayoutParams(status2Params);
            drawerStatusBar.setVisibility(View.VISIBLE);

            statusBar.setVisibility(View.VISIBLE);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || getResources().getBoolean(R.bool.isTablet)) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    public void setUpTheme() {

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE || getResources().getBoolean(R.bool.isTablet)) && !MainActivity.isPopup) {
            translucent = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

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

        Utils.setUpTheme(context, settings);

        if (settings.addonTheme) {
            getWindow().getDecorView().setBackgroundColor(settings.backgroundColor);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        try {
            mDrawerToggle.syncState();
        } catch (Exception e) {
            // landscape mode
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
        e.commit();

        HomeDataSource homeSources = new HomeDataSource(context);
        homeSources.open();
        homeSources.deleteAllTweets(currentAccount);
        homeSources.close();

        MentionsDataSource mentionsSources = new MentionsDataSource(context);
        mentionsSources.open();
        mentionsSources.deleteAllTweets(currentAccount);
        mentionsSources.close();

        DMDataSource dmSource = new DMDataSource(context);
        dmSource.open();
        dmSource.deleteAllTweets(currentAccount);
        dmSource.close();

        FavoriteUsersDataSource favs = new FavoriteUsersDataSource(context);
        favs.open();
        favs.deleteAllUsers(currentAccount);
        favs.close();

        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
        suggestions.clearHistory();

        if (currentAccount == 1 && login2) {
            e.putInt("current_account", 2).commit();
            finish();
            Intent next = new Intent(context, MainActivity.class);
            startActivity(next);
        } else if (currentAccount == 2 && login1) {
            e.putInt("current_account", 1).commit();
            finish();
            Intent next = new Intent(context, MainActivity.class);
            startActivity(next);
        } else { // only the one account
            e.putInt("current_account", 1).commit();
            finish();
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        if (sharedPrefs.getBoolean("remake_me", false) && !MainActivity.isPopup) {
            sharedPrefs.edit().putBoolean("remake_me", false).commit();
            recreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // cancels the notifications when the app is opened
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        DrawerActivity.settings = new AppSettings(context);

        // for testing
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                NotificationUtils.refreshNotification(context);
                //NotificationUtils.notifySecondMentions(context, 1);
            }
        }).start();*/
    }

    private SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, Search.class)));
        searchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (MainActivity.isPopup) {
            menu.getItem(3).setVisible(false); // hide the settings button if the popup is up
        }

        return true;
    }

    public static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        } catch (Exception e) {
            // landscape
        }

        switch (item.getItemId()) {
            case R.id.menu_search:
                overridePendingTransition(0,0);
                finish();
                overridePendingTransition(0,0);
                return super.onOptionsItemSelected(item);

            case R.id.menu_compose:
                Intent compose = new Intent(context, ComposeActivity.class);
                sharedPrefs.edit().putBoolean("from_notification_bool", false).commit();
                startActivity(compose);
                return super.onOptionsItemSelected(item);

            case R.id.menu_direct_message:
                Intent dm = new Intent(context, ComposeDMActivity.class);
                sharedPrefs.edit().putBoolean("from_notification_bool", false).commit();
                startActivity(dm);
                return super.onOptionsItemSelected(item);

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsPagerActivity.class);
                finish();
                sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                startActivity(settings);
                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void showStatusBar() {
        DrawerActivity.statusBar.setVisibility(View.VISIBLE);
    }

    public void hideStatusBar() {
        DrawerActivity.statusBar.setVisibility(View.GONE);
    }
}
