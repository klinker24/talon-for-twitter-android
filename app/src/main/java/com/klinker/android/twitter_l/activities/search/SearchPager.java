package com.klinker.android.twitter_l.activities.search;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.adapters.SearchPagerAdapter;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.NavBarOverlayLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.settings.SettingsActivity;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.utils.SearchUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class SearchPager extends WhiteToolbarActivity {

    private SearchPagerAdapter mSectionsPagerAdapter;
    public AppSettings settings;
    public Activity context;
    public SharedPreferences sharedPrefs;
    public androidx.appcompat.app.ActionBar actionBar;
    public boolean translucent;
    public ViewPager mViewPager;

    private SearchUtils searchUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setTaskDescription(this);
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

        try {
            searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
        } catch (Exception e) {
            searchQuery = "";
        }

        String rawQuery = searchQuery;

        if (searchQuery == null) {
            searchQuery = "";
        }

        boolean done = handleIntent(getIntent());

        if (done) {
            return;
        }

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE || getResources().getBoolean(R.bool.isTablet))) {
            translucent = true;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            try {
                int immersive = android.provider.Settings.System.getInt(getContentResolver(), "immersive_mode");

                if (immersive == 1) {
                    translucent = false;
                }
            } catch (Exception e) {
            }
        } else {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && !getResources().getBoolean(R.bool.isTablet)) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            translucent = false;
        }

        Utils.setUpMainTheme(context, settings);

        setContentView(R.layout.search_pager);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(searchQuery.replace("-RT", ""));
        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(Color.WHITE);

        LinearLayout.LayoutParams toolParams = (LinearLayout.LayoutParams) toolbar.getLayoutParams();
        toolParams.height = Utils.getActionBarHeight(context);
        toolbar.setLayoutParams(toolParams);
        toolbar.setBackgroundColor(settings.themeColors.primaryColor);
        setTitle(getResources().getString(R.string.search));

        searchUtils = new SearchUtils(this);
        searchUtils.setUpSearch();
        searchUtils.setText(rawQuery);

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setElevation(0);

        View statusBar = findViewById(R.id.activity_status_bar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            statusBar.setBackgroundColor(settings.themeColors.primaryColorDark);
        } else {
            statusBar.setBackgroundColor(getResources().getColor(android.R.color.black));
        }

        mViewPager = (ViewPager) findViewById(R.id.pager);

        statusBar.setVisibility(View.VISIBLE);

        int statusBarHeight = Utils.getStatusBarHeight(context);
        int actionBarHeight = Utils.getActionBarHeight(context);

        LinearLayout.LayoutParams statusParams = (LinearLayout.LayoutParams) statusBar.getLayoutParams();
        statusParams.height = statusBarHeight;
        statusBar.setLayoutParams(statusParams);

        mSectionsPagerAdapter = new SearchPagerAdapter(getFragmentManager(), context, onlyStatus, onlyProfile, searchQuery, translucent);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOffscreenPageLimit(3);

        TabLayout strip = (TabLayout) findViewById(R.id.pager_tab_strip);
        strip.setBackgroundColor(settings.themeColors.primaryColor);
        strip.setSelectedTabIndicatorColor(settings.themeColors.accentColor);
        strip.setupWithViewPager(mViewPager);

        int color = AppSettings.isWhiteToolbar(this) ? lightStatusBarIconColor : Color.WHITE;
        strip.setTabTextColors(color, color);

        int height = Utils.getActionBarHeight(this);
        //strip.setTranslationY(height);
        //mViewPager.setTranslationY(height);

        mViewPager.setCurrentItem(1);

        Utils.setActionBar(context, true);

        if (onlyProfile) {
            mViewPager.setCurrentItem(2);
        }

        if (!settings.transpartSystemBars) {
            new NavBarOverlayLayout(this).show();
        }
    }

    public String searchQuery = "";
    private boolean onlyStatus = false;
    private boolean onlyProfile = false;

    public boolean handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

            if (searchQuery.contains("#")) {
                suggestions.saveRecentQuery(searchQuery.replaceAll("\"", "").replaceAll(" -RT", ""), null);
            } else {
                suggestions.saveRecentQuery(searchQuery.replaceAll(" -RT", ""), null);
            }

            searchQuery = searchQuery.replace(" -RT", "") + " -RT";
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String uriString = uri.toString();
            if (uriString.contains("redirect") && uri.getQueryParameter("url") != null) { // coming from an email probably
                String str = URLDecoder.decode(uri.getQueryParameter("url"));

                Log.v("talon_search", str);

                if (!str.contains("/status/") && str.contains("?")) {
                    String name = str.substring(str.indexOf(".com/"));
                    name = name.replace(".com/", "");
                    name = name.substring(0, name.indexOf("?"));

                    searchQuery = name;
                    onlyProfile = true;
                }

            } else if (uriString.contains("status/")) {
                Log.v("talon_search", "searching for status");

                long id;
                String replace = uriString.substring(uriString.indexOf("status")).replace("status/", "").replaceAll("photo/*", "");
                if (replace.contains("/")) {
                    replace = replace.substring(0, replace.indexOf("/"));
                } else if (replace.contains("?")) {
                    replace = replace.substring(0, replace.indexOf("?"));
                }
                try {
                    id = Long.parseLong(replace);
                } catch (Exception e) {
                    id = 0l;
                }
                searchQuery = id + "";
                onlyStatus = true;
            } else if (uriString.contains("/hashtag/")) {
                searchQuery = uriString.replace("https://twitter.com/hashtag/", "");
                if (searchQuery.contains("?")) {
                    searchQuery = searchQuery.substring(0, searchQuery.indexOf("?"));
                }

                searchQuery = "#" + searchQuery + " -RT";
            } else if (!uriString.contains("q=") && !uriString.contains("screen_name%3D") && !uriString.contains("/intent/tweet")) {
                Log.v("talon_search", "user search from query");

                // going to try searching for users i guess
                if (!uriString.contains(".com/")) {
                    return false;
                }
                String name = uriString.substring(uriString.indexOf(".com/"));
                name = name.replaceAll("/", "").replaceAll(".com", "");
                if (name.contains("?ref_src"))
                    name = name.substring(0, name.indexOf("?ref"));
                if (name.contains("?lang")) // something like ?lang=en
                    name = name.substring(0, name.indexOf("?lang"));

                searchQuery = name;
                onlyProfile = true;
            } else if (uriString.contains("q=")) {
                Log.v("talon_search", "searching for query");

                try {
                    String search = uri.getQueryParameter("q");

                    if (search != null) {
                        searchQuery = search;
                        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

                        if (searchQuery.contains("#")) {
                            suggestions.saveRecentQuery(searchQuery.replaceAll("\"", "").replaceAll(" -RT", ""), null);
                        } else {
                            suggestions.saveRecentQuery(searchQuery.replaceAll(" -RT", ""), null);
                        }

                        searchQuery += " -RT";
                    } else {
                        searchQuery = "";
                    }

                } catch (Exception e) {

                }
            } else if (uriString.contains("/intent/tweet")) {
                Log.v("talon_search", "searching for intent to tweet");
                try {
                    String text = "";
                    final Map<String, List<String>> query_pairs = new LinkedHashMap<>();
                    final String[] pairs = uri.getQuery().split("&");
                    for (String pair : pairs) {
                        final int idx = pair.indexOf("=");
                        final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;

                        if (!(key.equals("text") || key.equals("via"))) {
                            continue;
                        }

                        if (!query_pairs.containsKey(key)) {
                            query_pairs.put(key, new LinkedList<String>());
                        }

                        final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;

                        if (key.equals("via")) {
                            text += "via @" + value + ": ";
                        } else {
                            text += value + " ";
                        }
                    }

                    Intent compose = new Intent(this, ComposeActivity.class);
                    compose.setAction(Intent.ACTION_SEND);
                    compose.putExtra(Intent.EXTRA_TEXT, text);
                    compose.setType("text/plain");

                    startActivity(compose);

                    finish();

                    return true;
                } catch (Exception e) {

                }
            } else {
                try {
                    String search = uriString;

                    search = search.substring(search.indexOf("screen_name%3D") + 14);
                    search = search.substring(0, search.indexOf("%"));

                    if (search != null) {
                        searchQuery = search;

                        SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                                MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

                        if (searchQuery.contains("#")) {
                            suggestions.saveRecentQuery(searchQuery.replaceAll("\"", "").replaceAll(" -RT", ""), null);
                        } else {
                            suggestions.saveRecentQuery(searchQuery.replaceAll(" -RT", ""), null);
                        }

                        searchQuery += " -RT";
                    } else {
                        searchQuery = "";
                    }

                    onlyProfile = true;
                } catch (Exception e) {

                }
            }
        }

        return false;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        removeKeyboard();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(searchQuery.replace("-RT", ""));
        searchUtils.setText(searchQuery.replace("-RT", ""));
    }

    @Override
    public void onResume() {
        super.onResume();

        removeKeyboard();
    }

    public void removeKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        } catch (Exception e) {

        }
    }

    private androidx.appcompat.widget.SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    private boolean rtUnchecked = false;
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!rtUnchecked) {
            MenuItem i = menu.findItem(R.id.menu_remove_rt);
            i.setChecked(true);
        }

        rtUnchecked = false;

        return super.onPrepareOptionsMenu(menu);
    }

    public static final int SETTINGS_RESULT = 101;

    @Override
    public void onBackPressed() {
        if (searchUtils.isShowing()) {
            searchUtils.hideSearch(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                sharedPrefs.edit().putBoolean("should_refresh", false).apply();
                onBackPressed();
                return true;

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

            case R.id.menu_save_search:
                Toast.makeText(context, getString(R.string.saving_search), Toast.LENGTH_SHORT).show();
                new TimeoutThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
                            twitter.createSavedSearch(searchQuery);

                            (context).runOnUiThread(new Runnable() {
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

            case R.id.menu_compose_with_search:
                Intent compose = new Intent(context, ComposeActivity.class);
                searchQuery = searchQuery
                        .replace(" -RT", "")
                        .replace(" filter:links twitter.com", "")
                        .replace(" TOP", "");
                compose.putExtra("user", searchQuery);
                startActivity(compose);
                return  super.onOptionsItemSelected(item);

            case R.id.menu_search:
                searchUtils.showSearchView();
                return super.onOptionsItemSelected(item);

            case R.id.menu_pic_filter:
                if (!item.isChecked()) {
                    searchQuery += " filter:links twitter.com";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace("filter:links", "").replace("twitter.com", "");
                    item.setChecked(false);
                }

                Intent broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
                broadcast.putExtra("query", searchQuery);
                context.sendBroadcast(broadcast);

                return super.onOptionsItemSelected(item);

            case R.id.menu_remove_rt:
                if (!item.isChecked()) {
                    searchQuery += " -RT";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace(" -RT", "");
                    item.setChecked(false);

                    rtUnchecked = true;
                }

                broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
                broadcast.putExtra("query", searchQuery);
                context.sendBroadcast(broadcast);

                return super.onOptionsItemSelected(item);

            case R.id.menu_show_top_tweets:
                if (!item.isChecked()) {
                    searchQuery += " TOP";
                    item.setChecked(true);
                } else {
                    searchQuery = searchQuery.replace(" TOP", "");
                    item.setChecked(false);
                }

                broadcast = new Intent("com.klinker.android.twitter.NEW_SEARCH");
                broadcast.putExtra("query", searchQuery);
                context.sendBroadcast(broadcast);

                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, SearchPager.class);
        restart.putExtra(SearchManager.QUERY, searchQuery);
        restart.setAction(Intent.ACTION_SEARCH);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}
