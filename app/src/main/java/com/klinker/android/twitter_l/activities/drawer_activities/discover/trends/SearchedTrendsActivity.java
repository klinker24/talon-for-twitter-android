package com.klinker.android.twitter_l.activities.drawer_activities.discover.trends;
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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.SettingsActivity;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.utils.SearchUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class SearchedTrendsActivity extends WhiteToolbarActivity {
    public static final int TWEETS_PER_REFRESH = 30;

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ActionBarDrawerToggle mDrawerToggle;

    private ListView listView;
    private LinearLayout spinner;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    private SearchUtils searchUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setSharedContentTransition(this);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        Utils.setUpMainTheme(context, settings);
        setContentView(R.layout.searched_trends_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(settings.themeColors.primaryColor);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.search));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setElevation(1);

        searchUtils = new SearchUtils(this);
        searchUtils.setUpSearch(false);

        mPullToRefreshLayout = (MaterialSwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
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

        listView = (ListView) findViewById(R.id.listView);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context) );
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        View header = new View(context);
        header.setOnClickListener(null);
        header.setOnLongClickListener(null);
        ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                Utils.getActionBarHeight(context));
        header.setLayoutParams(params);
        listView.addHeaderView(header);
        listView.setHeaderDividersEnabled(false);

        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount && canRefresh) {
                    getMore();
                }
            }
        });

        spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        removeKeyboard();
    }

    public void removeKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        try {
            imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
        } catch (Exception e) {
            // they closed i guess
        }
    }

    public String searchQuery = "";

    public void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY).replaceAll("\"", "");

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

            if (searchQuery.contains("#")) {
                suggestions.saveRecentQuery(searchQuery.replaceAll("\"", "").replaceAll(" -RT", ""), null);
            } else {
                suggestions.saveRecentQuery(searchQuery.replaceAll(" -RT", ""), null);
            }

            if (searchQuery.contains("#")) {
                // we want to add it to the userAutoComplete
                HashtagDataSource source = HashtagDataSource.getInstance(context);

                if (source != null) {
                    source.deleteTag(searchQuery.replaceAll("\"", ""));
                    source.createTag(searchQuery.replaceAll("\"", ""));
                }
            }

            if (!searchQuery.contains("-RT")) {
                searchQuery += " -RT";
            }
            String query = searchQuery;

            getSupportActionBar().setTitle(query.replace("-RT", ""));

            doSearch(query);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem i = menu.findItem(R.id.menu_remove_rt);
        i.setChecked(true);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        try {
            if (mDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            }
        } catch (Exception e) {

        }

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return super.onOptionsItemSelected(item);

            case R.id.menu_search:
                searchUtils.showSearchView();
                return super.onOptionsItemSelected(item);

            case R.id.menu_compose_with_search:
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", searchQuery.replaceAll("\"", "") + " ");
                startActivity(compose);
                return  super.onOptionsItemSelected(item);

            case R.id.menu_save_search:
                Toast.makeText(context, getString(R.string.saving_search), Toast.LENGTH_SHORT).show();
                new TimeoutThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
                            twitter.createSavedSearch(searchQuery.replaceAll("\"", ""));

                            ((Activity)context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, getString(R.string.success), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (TwitterException e) {
                            // something went wrong
                        }
                    }
                }).start();
                return super.onOptionsItemSelected(item);

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

            case R.id.menu_pic_filter:
                listView.setVisibility(View.GONE);
                if (!item.isChecked()) {
                    searchQuery += " filter:links twitter.com";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace("filter:links", "").replace("twitter.com", "");
                    item.setChecked(false);
                }
                doSearch(searchQuery);
                return super.onOptionsItemSelected(item);

            case R.id.menu_remove_rt:
                listView.setVisibility(View.GONE);
                if (!item.isChecked()) {
                    searchQuery += " -RT";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace(" -RT", "");
                    item.setChecked(false);
                }
                doSearch(searchQuery);
                return super.onOptionsItemSelected(item);

            case R.id.menu_show_top_tweets:
                if (!item.isChecked()) {
                    searchQuery += " TOP";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace(" TOP", "");
                    item.setChecked(false);
                }

                doSearch(searchQuery);

                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent returnIntent) {
        recreate();
    }


    @Override
    public void onBackPressed() {
        if (searchUtils.isShowing()) {
            searchUtils.hideSearch(true);
        } else {
            super.onBackPressed();
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void onRefreshStarted() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {

                final long topId;
                if (tweets.size() > 0) {
                    topId = tweets.get(0).getId();
                } else {
                    topId = 0;
                }

                try {
                    Twitter twitter = Utils.getTwitter(context, settings);
                    query = new Query(searchQuery);
                    query.setCount(TWEETS_PER_REFRESH);
                    QueryResult result = twitter.search(query);

                    if (searchQuery.contains(" TOP")) {
                        query.setResultType(Query.POPULAR);
                    } else {
                        query.setResultType(Query.RECENT);
                    }

                    query.setQuery(searchQuery.replace(" TOP", ""));

                    tweets.clear();

                    List<Status> statuses = result.getTweets();
                    for (twitter4j.Status status : statuses) {
                        tweets.add(status);
                    }

                    if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                        query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int top = 0;
                            for (int i = 0; i < tweets.size(); i++) {
                                if (tweets.get(i).getId() == topId) {
                                    top = i;
                                    break;
                                }
                            }

                            adapter = new TimelineArrayAdapter(context, tweets);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);
                            listView.setSelection(top);

                            spinner.setVisibility(View.GONE);

                            mPullToRefreshLayout.setRefreshing(false);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            mPullToRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
    }

    public ArrayList<twitter4j.Status> tweets = new ArrayList<Status>();
    public TimelineArrayAdapter adapter;
    public Query query;
    public boolean hasMore;

    public void doSearch(final String mQuery) {
        spinner.setVisibility(View.VISIBLE);

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);

                    Log.v("talon_search", "search mentionsQuery: " + mQuery);
                    query = new Query();
                    query.setCount(TWEETS_PER_REFRESH);

                    if (mQuery.contains(" TOP")) {
                        query.setResultType(Query.ResultType.popular);
                    }

                    query.setQuery(mQuery.replace(" TOP", ""));
                    QueryResult result;
                    try {
                        result = twitter.search(query);
                    } catch (OutOfMemoryError e) {
                        return;
                    }

                    tweets.clear();

                    List<Status> statuses = result.getTweets();
                    for (twitter4j.Status status : statuses) {
                        tweets.add(status);
                    }

                    if (statuses.size() >= SearchedTrendsActivity.TWEETS_PER_REFRESH - 10) {
                        query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new TimelineArrayAdapter(context, tweets);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);

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

    public boolean canRefresh = true;
    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            mPullToRefreshLayout.setRefreshing(true);
            new TimeoutThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Twitter twitter = Utils.getTwitter(context, settings);
                        QueryResult result = twitter.search(query);

                        List<Status> statuses = result.getTweets();
                        for (twitter4j.Status status : statuses) {
                            tweets.add(status);
                        }

                        if (statuses.size() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
                            query.setMaxId(getMaxIdFromList(tweets));
                            hasMore = true;
                        } else {
                            hasMore = false;
                        }

                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                mPullToRefreshLayout.setRefreshing(false);
                                canRefresh = true;
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPullToRefreshLayout.setRefreshing(false);
                                canRefresh = true;
                            }
                        });
                    }
                }
            }).start();
        }
    }

    public static long getMaxIdFromList(List<Status> statuses) {
        return statuses.get(statuses.size() - 1).getId() - 1;
    }
}