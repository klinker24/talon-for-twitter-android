package com.klinker.android.twitter_l.ui.drawer_activities.discover.trends;
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

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ArrayListLoader;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.FullScreenSwipeRefreshLayout;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.SwipeProgressBar;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.SettingsActivity;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class SearchedTrendsActivity extends AppCompatActivity {
    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private android.support.v7.app.ActionBar actionBar;

    private ActionBarDrawerToggle mDrawerToggle;

    private AsyncListView listView;
    private LinearLayout spinner;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

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
        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        settings = AppSettings.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        Utils.setUpTheme(context, settings);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.search));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(null);

        setContentView(R.layout.ptr_list_layout);

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

        listView = (AsyncListView) findViewById(R.id.listView);

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
        //listView.setTranslationY(Utils.getStatusBarHeight(context) + Utils.getActionBarHeight(context));

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

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

        Utils.setActionBar(context);

    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

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
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        } catch (Exception e) {
            // they closed i guess
        }
    }

    public String searchQuery = "";

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

            if (searchQuery.contains("#")) {
                suggestions.saveRecentQuery(searchQuery.replaceAll("\"", ""), null);
            } else {
                suggestions.saveRecentQuery(searchQuery, null);
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

            doSearch(query);
        }
    }

    private SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default

        int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
        ImageView view = (ImageView) searchView.findViewById(searchImgId);
        view.setImageResource(R.drawable.action_bar_search);

        if (!settings.darkTheme) {
            try {
                setSearchTextColour();
            } catch (Exception e) {

            }
            try {
                setSearchIcons();
            } catch (Exception e) {

            }
        }


        return super.onCreateOptionsMenu(menu);
    }

    private void setSearchTextColour() {
        int searchPlateId = searchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
        EditText searchPlate = (EditText) searchView.findViewById(searchPlateId);
        searchPlate.setTextColor(getResources().getColor(R.color.white));
        searchPlate.setHintTextColor(getResources().getColor(R.color.white));
        searchPlate.setBackgroundResource(android.R.color.transparent);
        searchPlate.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        int queryTextViewId = getResources().getIdentifier("android:id/search_src_text", null, null);
        View autoComplete = searchView.findViewById(queryTextViewId);

        try {
            Class<?> clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");

            SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
            stopHint.append(getString(R.string.search));

            // Add the icon as an spannable
            Drawable searchIcon = getResources().getDrawable(R.drawable.ic_action_search_dark);
            Method textSizeMethod = clazz.getMethod("getTextSize");
            Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
            int textSize = (int) (rawTextSize * 1.25);
            searchIcon.setBounds(0, 0, textSize, textSize);
            stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the new hint text
            Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
            setHintMethod.invoke(autoComplete, stopHint);
        } catch (Exception e) {

        }
    }


    private void setSearchIcons() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeBtn = (ImageView) searchField.get(searchView);
            closeBtn.setImageResource(R.drawable.tablet_close);

            searchField = SearchView.class.getDeclaredField("mVoiceButton");
            searchField.setAccessible(true);
            ImageView voiceBtn = (ImageView) searchField.get(searchView);
            voiceBtn.setImageResource(R.drawable.ic_voice_dark);

        } catch (NoSuchFieldException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e("SearchView", e.getMessage(), e);
        }
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

            case R.id.menu_compose_with_search:
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", searchQuery.replaceAll("\"", "") + " ");
                startActivity(compose);
                return  super.onOptionsItemSelected(item);

            case R.id.menu_save_search:
                Toast.makeText(context, getString(R.string.saving_search), Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
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
        finish();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void onRefreshStarted() {
        new Thread(new Runnable() {
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
                    QueryResult result = twitter.search(query);

                    tweets.clear();

                    for (twitter4j.Status status : result.getTweets()) {
                        tweets.add(status);
                    }

                    if (result.hasNext()) {
                        query = result.nextQuery();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);

                    Log.v("talon_search", "search query: " + mQuery);
                    query = new Query();

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

                    for (twitter4j.Status status : result.getTweets()) {
                        tweets.add(status);
                    }

                    if (result.hasNext()) {
                        query = result.nextQuery();
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Twitter twitter = Utils.getTwitter(context, settings);
                        QueryResult result = twitter.search(query);

                        for (twitter4j.Status status : result.getTweets()) {
                            tweets.add(status);
                        }

                        if (result.hasNext()) {
                            query = result.nextQuery();
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
}