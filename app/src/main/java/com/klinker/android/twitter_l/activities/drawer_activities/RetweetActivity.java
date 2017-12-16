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
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;

public class RetweetActivity extends DrawerActivity {

    private boolean landscape;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        setUpTheme();
        setContentView(R.layout.retweets_activity);
        setUpDrawer(3, getResources().getString(R.string.retweets));

        actionBar.setTitle(getResources().getString(R.string.retweets));

        listView = (ListView) findViewById(R.id.listView);

        if (getResources().getBoolean(R.bool.has_drawer)) {
            View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
            listView.addHeaderView(viewHeader, null, false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }

        listView.setHeaderDividersEnabled(false);
        if (settings.revampedTweets()) {
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

                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount && canRefresh) {
                    getRetweets();
                }

                if (!landscape && !isTablet) {
                    // show and hide the action bar
                    if (firstVisibleItem != 0) {
                        if (MainActivity.canSwitch) {
                            // used to show and hide the action bar
                            if (firstVisibleItem > mLastFirstVisibleItem) {
                                hideAppBar();
                            } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                showAppBar();
                            }

                            mLastFirstVisibleItem = firstVisibleItem;
                        }
                    } else {
                        showAppBar();
                    }
                }

                if (!getResources().getBoolean(R.bool.options_drawer) && DrawerActivity.statusBar.getVisibility() != View.GONE) {
                    DrawerActivity.statusBar.setVisibility(View.GONE);
                }
            }
        });

        getRetweets();

    }

    private boolean hidden = false;
    private void showAppBar() {
        if (hidden)
            showBars();
        hidden = false;
    }

    private void hideAppBar() {
        if (!hidden)
            hideBars();
        hidden = true;
    }

    public boolean canRefresh = false;
    public Paging paging = new Paging(1, 20);
    public TimelineArrayAdapter adapter;
    public ArrayList<Status> statuses = new ArrayList<Status>();
    public boolean hasMore = true;

    public void getRetweets() {
        if (!hasMore) {
            return;
        }

        canRefresh = false;
        final LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    final ResponseList<twitter4j.Status> favs = twitter.getRetweetsOfMe(paging);

                    if (favs.size() < 17) {
                        hasMore = false;
                    }

                    paging.setPage(paging.getPage() + 1);

                    for (twitter4j.Status s : favs) {
                        statuses.add(s);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (adapter == null) {
                                if (statuses.size() > 0) {
                                    adapter = new TimelineArrayAdapter(context, statuses, TimelineArrayAdapter.RETWEET);
                                    listView.setAdapter(adapter);
                                    listView.setVisibility(View.VISIBLE);
                                } else {
                                    LinearLayout nothing = (LinearLayout) findViewById(R.id.no_content);
                                    try {
                                        nothing.setVisibility(View.VISIBLE);
                                    } catch (Exception e) {

                                    }
                                    listView.setVisibility(View.GONE);
                                }
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            spinner.setVisibility(View.GONE);
                            canRefresh = true;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            canRefresh = false;
                        }
                    });
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            canRefresh = false;
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
        Intent restart = new Intent(context, RetweetActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }
}