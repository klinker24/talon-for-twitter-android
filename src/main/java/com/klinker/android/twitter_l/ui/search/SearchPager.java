package com.klinker.android.twitter_l.ui.search;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.SearchPagerAdapter;
import com.klinker.android.twitter_l.manipulations.NavBarOverlayLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.settings.SettingsActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.utils.Utils;

import org.apache.http.NameValuePair;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class SearchPager extends AppCompatActivity {

    private SearchPagerAdapter mSectionsPagerAdapter;
    public AppSettings settings;
    public Activity context;
    public SharedPreferences sharedPrefs;
    public android.support.v7.app.ActionBar actionBar;
    public boolean translucent;
    public ViewPager mViewPager;

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

        try {
            searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
        } catch (Exception e) {
            searchQuery = "";
        }

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

        Utils.setUpTweetTheme(context, settings);

        setContentView(R.layout.search_pager);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.search));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
        actionBar.setBackgroundDrawable(new ColorDrawable(settings.themeColors.primaryColor));
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
        statusParams.height = statusBarHeight + actionBarHeight;
        statusBar.setLayoutParams(statusParams);

        mSectionsPagerAdapter = new SearchPagerAdapter(getFragmentManager(), context, onlyStatus, onlyProfile, searchQuery, translucent);

        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOffscreenPageLimit(3);

        PagerSlidingTabStrip strip = (PagerSlidingTabStrip) findViewById(R.id.pager_tab_strip);
        //PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
        strip.setShouldExpand(true);
        strip.setBackgroundColor(settings.themeColors.primaryColor);
        strip.setTextColorResource(R.color.white);
        strip.setIndicatorColor(settings.themeColors.accentColor);
        strip.setTextSize((int)getResources().getDimension(R.dimen.pager_tab_strip_text));
        strip.setViewPager(mViewPager);

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

    private boolean handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

            if (searchQuery.contains("#")) {
                suggestions.saveRecentQuery(searchQuery.replaceAll("\"", ""), null);
            } else {
                suggestions.saveRecentQuery(searchQuery, null);
            }

            searchQuery += " -RT";
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String uriString = uri.toString();
            if (uriString.contains("status/")) {
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
            } else if (!uriString.contains("q=") && !uriString.contains("screen_name%3D") && !uriString.contains("/intent/tweet")) {
                Log.v("talon_search", "user search from query");

                // going to try searching for users i guess
                if (!uriString.contains(".com/")) {
                    return false;
                }
                String name = uriString.substring(uriString.indexOf(".com/"));
                name = name.replaceAll("/", "").replaceAll(".com", "");
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
                            suggestions.saveRecentQuery(searchQuery.replaceAll("\"", ""), null);
                        } else {
                            suggestions.saveRecentQuery(searchQuery, null);
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
                    final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
                    final String[] pairs = uri.getQuery().split("&");
                    for (String pair : pairs) {
                        final int idx = pair.indexOf("=");
                        final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                        if (!query_pairs.containsKey(key)) {
                            query_pairs.put(key, new LinkedList<String>());
                        }
                        final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                        text += value + " ";
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
                            suggestions.saveRecentQuery(searchQuery.replaceAll("\"", ""), null);
                        } else {
                            suggestions.saveRecentQuery(searchQuery, null);
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

        Log.v("talon_searching", "on new intent, query: " + searchQuery);
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

    private android.support.v7.widget.SearchView searchView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_activity, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));

        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem i = menu.findItem(R.id.menu_remove_rt);
        i.setChecked(true);

        return super.onPrepareOptionsMenu(menu);
    }

    public static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                onBackPressed();
                return true;

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

            case R.id.menu_save_search:
                Toast.makeText(context, getString(R.string.saving_search), Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
                            twitter.createSavedSearch(searchQuery.replace(" -RT", "").replace(" TOP", ""));

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

            case R.id.menu_compose_with_search:
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", searchQuery);
                startActivity(compose);
                return  super.onOptionsItemSelected(item);

            case R.id.menu_search:
                //overridePendingTransition(0,0);
                //finish();
                //overridePendingTransition(0,0);
                //return super.onOptionsItemSelected(item);

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
