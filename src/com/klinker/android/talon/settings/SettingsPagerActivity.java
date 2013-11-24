package com.klinker.android.talon.settings;

import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.widget.*;
import com.klinker.android.talon.R;

import java.util.ArrayList;
import java.util.Arrays;

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
        setContentView(R.layout.settings_main);

        DrawerArrayAdapter.current = 0;

        linkItems = new String[]{getResources().getString(R.string.theme_settings),
                getResources().getString(R.string.sync_settings),
                getResources().getString(R.string.notification_settings),
                getResources().getString(R.string.advanced_settings),
                getResources().getString(R.string.get_help_settings),
                getResources().getString(R.string.other_apps),
                getResources().getString(R.string.whats_new),
                getResources().getString(R.string.rate_it)};

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getFragmentManager(), this, mDrawerList);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.links_list);
        mDrawer = (LinearLayout) findViewById(R.id.drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerArrayAdapter(this,
                new ArrayList<String>(Arrays.asList(linkItems))));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new SettingsDrawerClickListener(this, mDrawerLayout, mDrawerList, mViewPager, mDrawer));

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
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

    /*@Override
    public void onBackPressed() {
//        Intent i = new Intent(this, MainActivity.class);
//        startActivity(i);
        finish();
        setResult(Activity.RESULT_OK);
        //overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
    }*/
}