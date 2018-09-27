package com.klinker.android.twitter_l.activities.drawer_activities.lists;
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
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.widget.Toolbar;
import android.util.TypedValue;
import android.view.*;
import android.widget.AbsListView;
import android.widget.LinearLayout;

import android.widget.ListView;
import android.widget.RelativeLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.NavBarOverlayLayout;
import com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;

public class ChoosenListActivity extends WhiteToolbarActivity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ListView listView;

    private long listId;
    private String listName;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;
    private LinearLayout spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setSharedContentTransition(this);

        settings = AppSettings.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        Utils.setUpMainTheme(this, settings);

        setContentView(R.layout.ptr_list_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(settings.themeColors.primaryColor);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);
        toolbar.setPadding(0, Utils.getStatusBarHeight(this), 0, 0);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(null);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            View kitkatStatusBar = findViewById(R.id.kitkat_status_bar);

            if (kitkatStatusBar != null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) kitkatStatusBar.getLayoutParams();
                params.height = Utils.getStatusBarHeight(context);
                kitkatStatusBar.setLayoutParams(params);

                kitkatStatusBar.setVisibility(View.VISIBLE);
                kitkatStatusBar.setBackgroundColor(getResources().getColor(android.R.color.black));
            }
        }

        mPullToRefreshLayout = (MaterialSwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        spinner = (LinearLayout) findViewById(R.id.list_progress);
        listView = (ListView) findViewById(R.id.listView);

        mPullToRefreshLayout.setOnRefreshListener(new MaterialSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int size = Utils.getActionBarHeight(context) + (landscape ? 0 : Utils.getStatusBarHeight(context));
        mPullToRefreshLayout.setProgressViewOffset(false, 0, size + toDP(25));
        mPullToRefreshLayout.setColorSchemeColors(settings.themeColors.accentColor, settings.themeColors.primaryColor);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getNavBarHeight(context) + Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        View header = new View(context);
        header.setOnClickListener(null);
        header.setOnLongClickListener(null);
        ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
        header.setLayoutParams(params);
        listView.addHeaderView(header);
        listView.setHeaderDividersEnabled(false);

        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }
        //listView.setTranslationY(Utils.getStatusBarHeight(context) + Utils.getActionBarHeight(context));

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount && canRefresh) {
                    getLists();
                }
            }
        });

        listName = getIntent().getStringExtra("list_name");

        listId = Long.parseLong(getIntent().getStringExtra("list_id"));
        actionBar.setTitle(listName);

        getLists();

        Utils.setActionBar(context);
        if (!settings.transpartSystemBars) {
            new NavBarOverlayLayout(this).show();
        }

    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    boolean justRefreshed = false;
    public void onRefreshStarted() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    justRefreshed = true;
                    currentPage = 1;
                    paging.setPage(currentPage);

                    ResponseList<Status> lists = twitter.getUserListStatuses(listId, paging);

                    currentPage = 2;
                    paging.setPage(currentPage);

                    statuses.clear();
                    statuses.addAll(lists);
                    stripDuplicates();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new TimelineArrayAdapter(context, statuses);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);

                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                } catch (OutOfMemoryError e) {
                    e.printStackTrace();

                }

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPullToRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    public Paging paging = new Paging(1, 20);
    private int currentPage = 1;
    private ArrayList<Status> statuses = new ArrayList<Status>();
    private TimelineArrayAdapter adapter;
    private boolean canRefresh = false;

    public void getLists() {
        canRefresh = false;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    ResponseList<twitter4j.Status> lists = twitter.getUserListStatuses(listId, paging);

                    currentPage++;
                    paging.setPage(currentPage);

                    statuses.addAll(lists);
                    stripDuplicates();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter == null) {
                                adapter = new TimelineArrayAdapter(context, statuses);
                                listView.setAdapter(adapter);
                                listView.setVisibility(View.VISIBLE);
                            } else {
                                adapter.notifyDataSetChanged();
                            }

                            spinner.setVisibility(View.GONE);
                            canRefresh = true;
                        }
                    });
                } catch (Exception | OutOfMemoryError e) {
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.choosen_list, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_to_first:
                try {
                    listView.setSelectionFromTop(0,0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;

            default:
                return true;
        }
    }

    private void stripDuplicates() {
        Map<Long, Status> map = new LinkedHashMap<>();
        for (Status status : statuses) {
            map.put(status.getId(), status);
        }
        statuses.clear();
        statuses.addAll(map.values());
    }
}
