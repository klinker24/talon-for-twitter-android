package com.klinker.android.talon.ui.drawer_activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.MainDrawerArrayAdapter;
import com.klinker.android.talon.listeners.MainDrawerClickListener;
import com.klinker.android.talon.manipulations.BlurTransform;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.manipulations.MySuggestionsProvider;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.settings.SettingsPagerActivity;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.ComposeActivity;
import com.klinker.android.talon.ui.ComposeDMActivity;
import com.klinker.android.talon.ui.LoginActivity;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.MainActivityPopup;
import com.klinker.android.talon.ui.UserProfileActivity;
import com.klinker.android.talon.ui.widgets.HoloTextView;
import com.klinker.android.talon.utils.ImageUtils;
import com.klinker.android.talon.utils.Utils;
import com.squareup.picasso.Picasso;

import org.lucasr.smoothie.AsyncListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import uk.co.senab.bitmapcache.BitmapLruCache;


/**
 * Created by luke on 11/28/13.
 */
public abstract class DrawerActivity extends Activity {

    public AppSettings settings;
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

        TextView name = (TextView) mDrawer.findViewById(R.id.name);
        TextView screenName = (TextView) mDrawer.findViewById(R.id.screen_name);
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

        // Keeping picasso right now because of the transforms...
        // Don't know how to do them yet with the manual caching

        try {
            backgroundPic.loadImage(backgroundUrl, false, null, NetworkedCacheableImageView.BLUR);

        } catch (Exception e) {
            // empty path for some reason
        }

        try {
            profilePic.loadImage(profilePicUrl, false, null, NetworkedCacheableImageView.CIRCLE);

        } catch (Exception e) {
            // empty path again
        }

        MainDrawerArrayAdapter adapter = new MainDrawerArrayAdapter(context, new ArrayList<String>(Arrays.asList(MainDrawerArrayAdapter.getItems(context))));
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

        RelativeLayout secondAccount = (RelativeLayout) findViewById(R.id.second_profile);
        HoloTextView name2 = (HoloTextView) findViewById(R.id.name_2);
        HoloTextView screenname2 = (HoloTextView) findViewById(R.id.screen_name_2);
        ImageView proPic2 = (ImageView) findViewById(R.id.profile_pic_2);

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
                    Picasso.with(context)
                            .load(sharedPrefs.getString("profile_pic_url_2", ""))
                            .transform(new CircleTransform())
                            .into(proPic2);
                } catch (Exception e) {

                }

                secondAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
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
                    Picasso.with(context)
                            .load(sharedPrefs.getString("profile_pic_url_1", ""))
                            .transform(new CircleTransform())
                            .into(proPic2);
                } catch (Exception e) {

                }
                secondAccount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (canSwitch) {
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
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            drawerList.addFooterView(footer);
            drawerList.setFooterDividersEnabled(false);

            View drawerStatusBar = findViewById(R.id.drawer_status_bar);
            LinearLayout.LayoutParams status2Params = (LinearLayout.LayoutParams) drawerStatusBar.getLayoutParams();
            status2Params.height = statusBarHeight;
            drawerStatusBar.setLayoutParams(status2Params);
            drawerStatusBar.setVisibility(View.VISIBLE);

            statusBar.setVisibility(View.VISIBLE);
        }
    }

    public void setUpTheme() {

        if (Build.VERSION.SDK_INT > 18 && !MainActivity.isPopup && settings.uiExtras && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE || getResources().getBoolean(R.bool.isTablet))) {
            translucent = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            translucent = false;
        }

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack);
                break;
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
    protected void onResume() {
        super.onResume();

        // cancels the notifications when the app is opened
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();

        // for testing
        /*RemoteViews remoteView = new RemoteViews("com.klinker.android.talon", R.layout.custom_notification);
        Intent popup = new Intent(context, MainActivityPopup.class);
        popup.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("from_notification", true);
        PendingIntent popupPending =
                PendingIntent.getActivity(
                        this,
                        0,
                        popup,
                        0
                );
        remoteView.setOnClickPendingIntent(R.id.popup_button, popupPending);
        remoteView.setTextViewText(R.id.content, "test");

        remoteView.setImageViewResource(R.id.icon, R.drawable.timeline_dark);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_action_accept_dark)
                        .setContent(remoteView);
        //.setContentTitle(getResources().getString(R.string.app_name))
        //.setContentText(numberNew + " new tweets");

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("from_notification", true);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        0
                );

        mBuilder.setContentIntent(resultPendingIntent);
        mNotificationManager.notify(4, mBuilder.build());*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
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
            case R.id.menu_compose:
                Intent compose = new Intent(context, ComposeActivity.class);
                startActivity(compose);
                return true;

            case R.id.menu_direct_message:
                Intent dm = new Intent(context, ComposeDMActivity.class);
                startActivity(dm);
                return true;

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsPagerActivity.class);
                finish();
                startActivity(settings);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void showStatusBar() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DrawerActivity.statusBar.setVisibility(View.VISIBLE);
            }
        }, 000);
    }

    public void hideStatusBar() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DrawerActivity.statusBar.setVisibility(View.GONE);
            }
        }, 000); // 200 would be better
    }
}
