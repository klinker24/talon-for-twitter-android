package com.klinker.android.twitter.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.widgets.HoloTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SettingsPagerActivity extends FragmentActivity {

    SectionsPagerAdapter mSectionsPagerAdapter;
    SharedPreferences sharedPrefs;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private LinearLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private boolean userKnows;
    public static boolean settingsLinksActive = true;
    public static boolean inOtherLinks = true;

    private String[] linkItems;

    public static ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpTheme();

        setContentView(R.layout.settings_main);

        DrawerArrayAdapter.current = 0;

        linkItems = new String[]{getResources().getString(R.string.theme_settings),
                getResources().getString(R.string.timelines_settings),
                getResources().getString(R.string.sync_settings),
                getResources().getString(R.string.notification_settings),
                getResources().getString(R.string.advanced_settings),
                getResources().getString(R.string.get_help_settings),
                getResources().getString(R.string.other_apps),
                getResources().getString(R.string.whats_new),
                getResources().getString(R.string.rate_it)};

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPrefs.getBoolean("version_1.20_1", true)) {
            // necessary because i didnt start out by using sets
            boolean led = sharedPrefs.getBoolean("led", true);
            boolean sound = sharedPrefs.getBoolean("sound", true);
            boolean vibrate = sharedPrefs.getBoolean("vibrate", true);
            boolean wakeScreen = sharedPrefs.getBoolean("wake", true);
            boolean timelineNot = sharedPrefs.getBoolean("timeline_notifications", true);
            boolean mentionsNot = sharedPrefs.getBoolean("mentions_notifications", true);
            boolean dmsNot = sharedPrefs.getBoolean("direct_message_notifications", true);
            boolean favoritesNot = sharedPrefs.getBoolean("favorite_notifications", true);
            boolean retweetNot = sharedPrefs.getBoolean("retweet_notifications", true);
            boolean followersNot = sharedPrefs.getBoolean("follower_notifications", true);

            Set<String> alert = sharedPrefs.getStringSet("alert_types", new HashSet<String>());
            alert.clear();
            if (vibrate) {
                alert.add("1");
            }
            if (led) {
                alert.add("2");
            }
            if (wakeScreen) {
                alert.add("3");
            }
            if (sound) {
                alert.add("4");
            }
            sharedPrefs.edit().putStringSet("alert_types", alert).commit();

            Set<String> timeline = sharedPrefs.getStringSet("timeline_set", new HashSet<String>());
            timeline.clear();
            if (timelineNot) {
                timeline.add("1");
            }
            if (mentionsNot) {
                timeline.add("2");
            }
            if (dmsNot) {
                timeline.add("3");
            }
            sharedPrefs.edit().putStringSet("timeline_set", timeline).commit();

            Set<String> interactions = sharedPrefs.getStringSet("interactions_set", new HashSet<String>());
            interactions.clear();
            if (favoritesNot) {
                interactions.add("1");
            }
            if (retweetNot) {
                interactions.add("2");
            }
            if (followersNot) {
                interactions.add("3");
            }
            sharedPrefs.edit().putStringSet("interactions_set", interactions).commit();

            sharedPrefs.edit().putBoolean("version_1.20_1", false).commit();

            recreate();
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getFragmentManager(), this, mDrawerList);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        mDrawerList = (ListView) findViewById(R.id.links_list);
        mDrawer = (LinearLayout) findViewById(R.id.drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerArrayAdapter(this,
                new ArrayList<String>(Arrays.asList(linkItems))));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new SettingsDrawerClickListener(this, mDrawerLayout, mDrawerList, mViewPager, mDrawer));

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.drawerIcon});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                resource,  /* nav drawer icon to replace 'Up' caret */
                R.string.app_name,  /* "open drawer" description */
                R.string.app_name  /* "close drawer" description */
        );

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        try {
            if (getIntent().getBooleanExtra("mms", false)) {
                mViewPager.setCurrentItem(6, true);
            }

            int pageNumber = getIntent().getIntExtra("page_number", 0);
            mViewPager.setCurrentItem(pageNumber, true);
        } catch (Exception e) {

        }

        userKnows = sharedPrefs.getBoolean("user_knows_navigation_drawer", false);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                DrawerArrayAdapter.current = position;
                mDrawerList.invalidateViews();
            }
        });

        if (!userKnows) {
            mDrawerLayout.openDrawer(mDrawer);
        }

        HoloTextView createdBy = (HoloTextView) findViewById(R.id.created_by);
        HoloTextView versionNumber = (HoloTextView) findViewById(R.id.version_number);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

            String text = getResources().getString(R.string.created_by) + " Luke Klinker";
            String text2 = getResources().getString(R.string.version) + " " + versionName;
            createdBy.setText(text);
            versionNumber.setText(text2);
        } catch (Exception e) {
            String text = getResources().getString(R.string.created_by) + " Luke Klinker";
            String text2 = getResources().getString(R.string.version) + " 0.00";
            createdBy.setText(text);
            versionNumber.setText(text2);
        }

        LinearLayout description = (LinearLayout) findViewById(R.id.created_by_layout);
        description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Klinker+Apps")));
            }
        });
    }

    public void setUpTheme() {

        AppSettings settings = new AppSettings(this);

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

        if (settings.addonTheme) {
            getWindow().getDecorView().setBackgroundColor(settings.backgroundColor);
        } else {
            TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
            int resource = a.getResourceId(0, 0);
            a.recycle();

            getWindow().getDecorView().setBackgroundResource(resource);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                if (mDrawerToggle.onOptionsItemSelected(item)) {
                    if (!userKnows) {
                        userKnows = true;

                        sharedPrefs.edit().putBoolean("user_knows_navigation_drawer", true).commit();
                    }
                    return true;
                }

                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onBackPressed() {
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        finish();
    }
}