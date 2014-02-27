package com.klinker.android.twitter.ui.drawer_activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.utils.MySuggestionsProvider;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.settings.SettingsPagerActivity;
import com.klinker.android.twitter.ui.compose.ComposeActivity;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class Search extends Activity implements OnRefreshListener {

    private AsyncListView listView;
    private LinearLayout spinner;

    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;
    private ActionBar actionBar;

    private boolean translucent;

    private PullToRefreshLayout mPullToRefreshLayout;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_zoom_enter, R.anim.slide_out_right);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = AppSettings.getInstance(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

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
            translucent = false;
        }

        Utils.setUpTheme(context, settings);

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.search));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setContentView(R.layout.ptr_list_layout);

        mPullToRefreshLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);

        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(this)
                // set up the scroll distance
                .options(Options.create().scrollDistance(.3f).build())
                        // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set the OnRefreshListener
                .listener(this)
                        // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);

        if (settings.addonTheme) {
            DefaultHeaderTransformer transformer = ((DefaultHeaderTransformer)mPullToRefreshLayout.getHeaderTransformer());
            transformer.setProgressBarColor(settings.accentInt);
        }

        try {
            searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
        } catch (Exception e) {
            searchQuery = "";
        }

        if (searchQuery == null) {
            searchQuery = "";
        }

        listView = (AsyncListView) findViewById(R.id.listView);

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;

                if(lastItem == totalItemCount && canRefresh) {
                    if (!searchQuery.contains("@")) {
                        getMore();
                    } else {
                        getMoreUsers(searchQuery.replace("@", ""));
                    }
                }
            }
        });

        if (searchQuery != null && !searchQuery.equals("") && !searchQuery.contains("@")) {
            BitmapLruCache cache = App.getInstance(context).getBitmapCache();
            ArrayListLoader loader = new ArrayListLoader(cache, context);

            ItemManager.Builder builder = new ItemManager.Builder(loader);
            builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
            builder.setThreadPoolSize(4);

            listView.setItemManager(builder.build());
        }

        View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);

        View footer = new View(context);
        footer.setOnClickListener(null);
        footer.setOnLongClickListener(null);
        ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, toDP(5));
        footer.setLayoutParams(params);
        listView.addFooterView(footer);
        listView.setFooterDividersEnabled(false);

        if (translucent) {
            if (Utils.hasNavBar(context)) {
                footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
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
            listView.setHeaderDividersEnabled(false);
        }

        //setUpDrawer(8, getResources().getString(R.string.search));

        spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        handleIntent(getIntent());

        Utils.setActionBar(context);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        removeKeyboard();
        actionBar.setDisplayShowHomeEnabled(false);
    }

    public void removeKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        } catch (Exception e) {

        }
    }

    public String searchQuery = "";

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);
            if (searchQuery.contains("@")) {
                String query = searchQuery.replace("@", "");
                doUserSearch(query);
            } else {
                String query = searchQuery;
                doSearch(query);
            }

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
            suggestions.saveRecentQuery(searchQuery, null);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            String uriString = uri.toString();
            if (uriString.contains("status/")) {
                long id = Long.parseLong(uriString.substring(uriString.indexOf("status")).replace("status/", "").replaceAll("photo/*", ""));
                findStatus(id);
            } else if (!uriString.contains("q=")) { // going to try searching for users i guess
                String name = uriString.substring(uriString.indexOf(".com/"));
                name = name.replaceAll("/", "").replaceAll(".com", "");
                Log.v("searching_twitter", "username: " + name);
                doUserSearch(name);
            } else {
                try {
                    String search = uri.getQueryParameter("q");
                    Log.v("searching_twitter", "" + search);

                    if (search != null) {
                        searchQuery = search;
                        if (searchQuery.contains("@")) {
                            String query = searchQuery.replace("@", "");
                            doUserSearch(query);
                        } else {
                            String query = searchQuery;
                            doSearch(query);
                        }
                    }

                    SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                    suggestions.saveRecentQuery(searchQuery, null);
                } catch (Exception e) {

                }
            }
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
        Log.v("searching_talon", getComponentName().toString());
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);

        if (searchQuery.contains("@")) {
            // user search and we should hide the filters
            menu.getItem(3).setVisible(false); // pictures
            menu.getItem(4).setVisible(false); // retweets
        }

        return true;
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
                Intent settings = new Intent(context, SettingsPagerActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

            case R.id.menu_save_search:
                Toast.makeText(context, getString(R.string.saving_search), Toast.LENGTH_SHORT).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
                            twitter.createSavedSearch(searchQuery);

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
                overridePendingTransition(0,0);
                finish();
                overridePendingTransition(0,0);
                return super.onOptionsItemSelected(item);

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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);
                    query = new Query(searchQuery.replace("@", "from:"));
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
                            adapter = new TimelineArrayAdapter(context, tweets);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);

                            spinner.setVisibility(View.GONE);

                            mPullToRefreshLayout.setRefreshComplete();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            mPullToRefreshLayout.setRefreshComplete();
                        }
                    });
                }
            }
        }).start();
    }

    public void doSearch(final String mQuery) {
        spinner.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);
                    query = new Query(mQuery);
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

    public ArrayList<User> users;
    public int userPage = 1;
    public PeopleArrayAdapter peopleAdapter;

    public void doUserSearch(final String mQuery) {
        listView.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        hasMore = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);
                    ResponseList<User> result = twitter.searchUsers(mQuery, userPage);

                    userPage++;

                    if (result.size() < 18) {
                        hasMore = false;
                    }

                    users = new ArrayList<User>();

                    for (User u : result) {
                        users.add(u);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peopleAdapter = new PeopleArrayAdapter(context, users);
                            listView.setAdapter(peopleAdapter);
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
                    hasMore = false;
                }
            }
        }).start();
    }

    public void findStatus(final long statusid) {
        listView.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        hasMore = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);
                    Status status = twitter.showStatus(statusid);

                    userPage++;

                    final ArrayList<Status> statuses = new ArrayList<Status>();

                    statuses.add(status);

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses);
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
                    hasMore = false;
                }
            }
        }).start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, Search.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public boolean canRefresh = true;
    public boolean hasMore;
    public TimelineArrayAdapter adapter;
    public Query query;
    public ArrayList<Status> tweets = new ArrayList<Status>();

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
                                mPullToRefreshLayout.setRefreshComplete();
                                canRefresh = true;
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPullToRefreshLayout.setRefreshComplete();
                                canRefresh = true;
                            }
                        });
                    }
                }
            }).start();
        }
    }

    public void getMoreUsers(final String mQuery) {
        if (hasMore) {
            canRefresh = false;
            mPullToRefreshLayout.setRefreshing(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Twitter twitter = Utils.getTwitter(context, settings);
                        ResponseList<User> result = twitter.searchUsers(mQuery, userPage);

                        userPage++;

                        for (User u : result) {
                            users.add(u);
                        }

                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                peopleAdapter.notifyDataSetChanged();
                                mPullToRefreshLayout.setRefreshComplete();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();

                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPullToRefreshLayout.setRefreshComplete();
                            }
                        });
                        hasMore = false;
                    }
                }
            }).start();
        }
    }
}