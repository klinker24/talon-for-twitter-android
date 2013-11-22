package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.klinker.android.talon.adapters.*;
import com.klinker.android.talon.R;
import com.klinker.android.talon.listeners.MainDrawerClickListener;
import com.klinker.android.talon.manipulations.BlurTransform;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;
    private ActionBar actionBar;

    private TimelinePagerAdapter mSectionsPagerAdapter;
    public static ViewPager mViewPager;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawer;
    private ListView drawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private boolean logoutVisible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        setUpTheme();

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

        setUpDrawer();

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
    }

    public void setUpDrawer() {

        MainDrawerArrayAdapter.current = 0;

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.drawerIcon});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

        TextView name = (TextView) mDrawer.findViewById(R.id.name);
        TextView screenName = (TextView) mDrawer.findViewById(R.id.screen_name);
        NetworkedCacheableImageView backgroundPic = (NetworkedCacheableImageView) mDrawer.findViewById(R.id.background_image);
        NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) mDrawer.findViewById(R.id.profile_pic);
        final ImageButton showMoreDrawer = (ImageButton) mDrawer.findViewById(R.id.options);
        final LinearLayout logoutLayout = (LinearLayout) mDrawer.findViewById(R.id.logoutLayout);
        final Button logoutDrawer = (Button) mDrawer.findViewById(R.id.logoutButton);
        drawerList = (ListView) mDrawer.findViewById(R.id.drawer_list);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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

                    logoutDrawer.setVisibility(View.GONE);
                    drawerList.setVisibility(View.VISIBLE);

                    logoutVisible = false;
                }

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

            public void onDrawerOpened(View drawerView) {
                actionBar.setTitle(getResources().getString(R.string.app_name));
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        showMoreDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(logoutDrawer.getVisibility() == View.GONE) {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    logoutDrawer.setVisibility(View.VISIBLE);
                    drawerList.setVisibility(View.GONE);

                    logoutVisible = true;
                } else {
                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                    ranim.setFillAfter(true);
                    showMoreDrawer.startAnimation(ranim);

                    logoutDrawer.setVisibility(View.GONE);
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
                mDrawerLayout.closeDrawer(Gravity.START);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent viewProfile = new Intent(context, UserProfileActivity.class);
                        viewProfile.putExtra("name", sName);
                        viewProfile.putExtra("screenname", sScreenName);
                        viewProfile.putExtra("proPic", profilePicUrl);
                        viewProfile.putExtra("tweetid", 0);
                        viewProfile.putExtra("retweet", false);

                        context.startActivity(viewProfile);
                    }
                }, 400);

            }
        });

        Log.v("twitter_drawer", profilePicUrl);

        name.setText(sName);
        screenName.setText("@" + sScreenName);

        // Keeping picasso right now because of the transforms...
        // Don't know how to do them yet with the manual caching
        try {
            Picasso.with(context)
                    .load(backgroundUrl)
                    .transform(new BlurTransform(context))
                    .into(backgroundPic);
            //backgroundPic.loadImage(backgroundUrl, false, null, NetworkedCacheableImageView.BLUR);
        } catch (Exception e) {
            // empty path for some reason
        }

        try {
            Picasso.with(context)
                    .load(profilePicUrl)
                    .transform(new CircleTransform())
                    .into(profilePic);
            //backgroundPic.loadImage(profilePicUrl, false, null, NetworkedCacheableImageView.CIRCLE);
        } catch (Exception e) {
            // empty path again
        }

        MainDrawerArrayAdapter adapter = new MainDrawerArrayAdapter(context, new ArrayList<String>(Arrays.asList(MainDrawerArrayAdapter.getItems(context))));
        drawerList.setAdapter(adapter);

        drawerList.setOnItemClickListener(new MainDrawerClickListener(context, mDrawerLayout, mViewPager));

    }

    public void setUpTheme() {

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
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void logoutFromTwitter() {
        // Clear the shared preferences
        SharedPreferences.Editor e = sharedPrefs.edit();
        e.remove("authentication_token");
        e.remove("authentication_token_secret");
        e.remove("is_logged_in");
        e.commit();

        HomeDataSource homeSources = new HomeDataSource(context);
        homeSources.open();
        homeSources.deleteAllTweets();
        homeSources.close();

        MentionsDataSource mentionsSources = new MentionsDataSource(context);
        mentionsSources.open();
        mentionsSources.deleteAllTweets();
        mentionsSources.close();

        DMDataSource dmSource = new DMDataSource(context);
        dmSource.open();
        dmSource.deleteAllTweets();
        dmSource.close();

        Intent login = new Intent(context, LoginActivity.class);
        startActivity(login);
    }

    @Override
    protected void onDestroy() {
        Crouton.cancelAllCroutons();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}