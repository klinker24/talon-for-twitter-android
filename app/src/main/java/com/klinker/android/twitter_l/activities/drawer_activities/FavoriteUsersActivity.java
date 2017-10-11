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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.FavoriteUsersCursorAdapter;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.utils.Utils;

public class FavoriteUsersActivity extends DrawerActivity {

    private boolean landscape;
    private static ListView list;
    private static Context sContext;
    private static SharedPreferences sSharedPrefs;
    private static LinearLayout spinner;
    private static LinearLayout nothing;

    @Override
    public void onDestroy() {
        try {
            people.getCursor().close();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        context = this;
        sContext = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        sSharedPrefs = sharedPrefs;
        settings = AppSettings.getInstance(this);

        setUpTheme();
        setContentView(R.layout.retweets_activity);
        setUpDrawer(2, getResources().getString(R.string.favorite_users));

        actionBar.setTitle(getResources().getString(R.string.favorite_users));

        spinner = (LinearLayout) findViewById(R.id.list_progress);
        nothing = (LinearLayout) findViewById(R.id.no_content);

        listView = (ListView) findViewById(R.id.listView);
        list = listView;

        if (getResources().getBoolean(R.bool.has_drawer)) {
            View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
            listView.addHeaderView(viewHeader, null, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }

        listView.setHeaderDividersEnabled(false);
        if (settings.revampedTweetLayout) {
            listView.setDivider(null);
        }

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        View view = new View(context);
        view.setOnClickListener(null);
        view.setOnLongClickListener(null);
        ListView.LayoutParams params2 = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getStatusBarHeight(context));
        view.setLayoutParams(params2);
        listView.addHeaderView(view);
        listView.setFooterDividersEnabled(false);

        final boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                // show and hide the action bar
                if (firstVisibleItem != 0) {
                    if (MainActivity.canSwitch) {
                        // used to show and hide the action bar
                        if (firstVisibleItem > mLastFirstVisibleItem) {
                            if (!landscape && !isTablet) {
                                hideBars();
                            }
                        } else if (firstVisibleItem < mLastFirstVisibleItem) {
                            if(!landscape && !isTablet) {
                                showBars();
                            }
                        }

                        mLastFirstVisibleItem = firstVisibleItem;
                    }
                } else {
                    if(!landscape && !isTablet) {
                        showBars();
                    }
                }

                if (!getResources().getBoolean(R.bool.options_drawer) && DrawerActivity.statusBar.getVisibility() != View.GONE) {
                    DrawerActivity.statusBar.setVisibility(View.GONE);
                }
            }
        });

        LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        new GetFavUsers().execute();

    }

    private static FavoriteUsersCursorAdapter people;

    public static void refreshFavs() {
        new GetFavUsers().execute();
    }

    static class GetFavUsers extends AsyncTask<String, Void, Cursor> {

        protected Cursor doInBackground(String... urls) {
            try {
                return FavoriteUsersDataSource.getInstance(sContext).getCursor();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Cursor cursor) {

            if (cursor == null) {
                return;
            }


            try {
                Log.v("fav_users", cursor.getCount() + "");
            } catch (Exception e) {

                FavoriteUsersDataSource.dataSource = null;
                return;
            }

            if (cursor.getCount() > 0) {
                people = new FavoriteUsersCursorAdapter(sContext, cursor);
                list.setAdapter(people);
                list.setVisibility(View.VISIBLE);
            } else {
                try {
                    nothing.setVisibility(View.VISIBLE);
                } catch (Exception e) {

                }
                list.setVisibility(View.GONE);
            }

            spinner.setVisibility(View.GONE);
        }
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
        Intent restart = new Intent(context, FavoriteUsersActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}