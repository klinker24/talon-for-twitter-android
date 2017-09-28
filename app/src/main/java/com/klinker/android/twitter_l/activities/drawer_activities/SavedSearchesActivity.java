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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.SavedSearchArrayAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Twitter;


public class SavedSearchesActivity extends DrawerActivity {

    public static ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.context = this;
        SavedSearchesActivity.context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        setUpTheme();
        setContentView(R.layout.twitter_lists_page);
        setUpDrawer(5, getResources().getString(R.string.saved_searches));

        actionBar.setTitle(getResources().getString(R.string.saved_searches));

        listView = (ListView) findViewById(R.id.listView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (getResources().getBoolean(R.bool.has_drawer)) {
                getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));

                kitkatStatusBar.setVisibility(View.VISIBLE);
                kitkatStatusBar.setBackgroundColor(settings.themeColors.primaryColorDark);
            } else {
                getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            }
        }

        nothing = (LinearLayout) findViewById(R.id.no_content);
        spinner = (LinearLayout) findViewById(R.id.list_progress);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getNavBarHeight(context) + (getResources().getBoolean(R.bool.has_drawer) ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        getSearches();

    }

    private boolean clicked = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        } catch (Exception e) {

        }

        return super.onOptionsItemSelected(item);
    }

    public static SavedSearchArrayAdapter adapter;
    public static LinearLayout nothing;
    public static LinearLayout spinner;
    public static Context context;

    public static void getSearches() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    final ResponseList<SavedSearch> searches = twitter.getSavedSearches();

                    Collections.sort(searches, new Comparator<SavedSearch>() {
                        public int compare(SavedSearch result1, SavedSearch result2) {
                            return result1.getQuery().compareTo(result2.getQuery());
                        }
                    });

                    final ArrayList<String> searchNames = new ArrayList<String>();

                    for (SavedSearch sear : searches) {
                        searchNames.add(sear.getQuery());
                    }

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (searchNames.size() > 0) {
                                adapter = new SavedSearchArrayAdapter(context, searchNames);
                                listView.setAdapter(adapter);
                                listView.setVisibility(View.VISIBLE);
                            } else {
                                try {
                                    nothing.setVisibility(View.VISIBLE);
                                } catch (Exception e) {

                                }
                                listView.setVisibility(View.GONE);
                            }

                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        final int DISMISS = 0;
        final int SEARCH = 1;
        final int COMPOSE = 2;
        final int NOTIFICATIONS = 3;
        final int DM = 4;
        final int SETTINGS = 5;
        final int TOFIRST = 6;

        menu.getItem(NOTIFICATIONS).setVisible(false);

        return true;
    }

    private boolean changedConfig = false;
    private boolean activityActive = true;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (activityActive) {
            restartActivity();
        } else {
            changedConfig = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activityActive = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (changedConfig) {
            restartActivity();
        }

        activityActive = true;
        changedConfig = false;
    }

    private void restartActivity() {
        overridePendingTransition(0, 0);
        finish();
        Intent restart = new Intent(context, SavedSearchesActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }
}