package com.klinker.android.twitter.ui.drawer_activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.manipulations.MySuggestionsProvider;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.settings.SettingsPagerActivity;
import com.klinker.android.twitter.ui.LoginActivity;
import com.klinker.android.twitter.ui.compose.ComposeActivity;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.net.URI;
import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Twitter;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

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

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        try {
            searchQuery = getIntent().getStringExtra(SearchManager.QUERY);
        } catch (Exception e) {
            searchQuery = "";
        }

        listView = (AsyncListView) findViewById(R.id.listView);

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

    String searchQuery = "";

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            searchQuery = intent.getStringExtra(SearchManager.QUERY);
            if (searchQuery.contains("@")) {
                String query = searchQuery.replace("@", "");
                new DoUserSearch(query).execute();
            } else {
                String query = searchQuery;
                new DoSearch(query).execute();
            }

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
            suggestions.saveRecentQuery(searchQuery, null);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            try {
                Uri uri = intent.getData();

                String search = uri.getQueryParameter("q");
                Log.v("searching_twitter", "" + search);

                if (search != null) {
                    searchQuery = search;
                    if (searchQuery.contains("@")) {
                        String query = searchQuery.replace("@", "");
                        new DoUserSearch(query).execute();
                    } else {
                        String query = searchQuery;
                        new DoSearch(query).execute();
                    }
                }

                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                        MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                suggestions.saveRecentQuery(searchQuery, null);
            } catch (Exception e) {

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

        return true;
    }

    public static final int SETTINGS_RESULT = 101;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /*if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }*/

        switch (item.getItemId()) {
            case android.R.id.home:
                sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                onBackPressed();
                return true;

            case R.id.menu_settings:
                Intent settings = new Intent(context, SettingsPagerActivity.class);
                startActivityForResult(settings, SETTINGS_RESULT);
                return true;

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

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRefreshStarted(View view) {
        new AsyncTask<Void, Void, ArrayList<twitter4j.Status>>() {

            @Override
            protected ArrayList<twitter4j.Status> doInBackground(Void... params) {
                try {
                    Log.v("inside_search", searchQuery);

                    if (searchQuery.contains("@")) {
                        new DoUserSearch(searchQuery).execute();
                        return null;
                    }

                    Twitter twitter = Utils.getTwitter(context, settings);
                    Query query = new Query(searchQuery.replace("@", "from:"));
                    QueryResult result = twitter.search(query);
                    QueryResult result2 = null;
                    QueryResult result3 = null;

                    if (result.hasNext()) {
                        result2 = twitter.search(result.nextQuery());
                        if (result2.hasNext()) {
                            result3 = twitter.search(result2.nextQuery());
                        }
                    }
                    Log.v("inside_search", "got data");

                    ArrayList<twitter4j.Status> tweets = new ArrayList<twitter4j.Status>();
                    for (twitter4j.Status status : result.getTweets()) {
                        tweets.add(status);
                    }
                    if (result2 != null) {
                        for (twitter4j.Status status : result2.getTweets()) {
                            tweets.add(status);
                        }
                        if (result3 != null) {
                            for (twitter4j.Status status : result3.getTweets()) {
                                tweets.add(status);
                            }
                        }
                    }

                    return tweets;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            protected void onPostExecute(ArrayList<twitter4j.Status> searches) {

                if (searches != null) {
                    listView.setAdapter(new TimelineArrayAdapter(context, searches));
                    listView.setVisibility(View.VISIBLE);
                }

                spinner.setVisibility(View.GONE);

                mPullToRefreshLayout.setRefreshComplete();
            }
        }.execute();
    }

    class DoSearch extends AsyncTask<String, Void, ArrayList<twitter4j.Status>> {

        String mQuery;

        public DoSearch(String query) {
            this.mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            listView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
        }

        protected ArrayList<twitter4j.Status> doInBackground(String... urls) {
            try {
                Log.v("inside_search", mQuery);

                Twitter twitter = Utils.getTwitter(context, settings);
                Query query = new Query(mQuery);
                QueryResult result = twitter.search(query);
                QueryResult result2 = null;
                QueryResult result3 = null;

                if (result.hasNext()) {
                    result2 = twitter.search(result.nextQuery());
                    if (result2.hasNext()) {
                        result3 = twitter.search(result2.nextQuery());
                    }
                }
                Log.v("inside_search", "got data");

                ArrayList<twitter4j.Status> tweets = new ArrayList<twitter4j.Status>();
                for (twitter4j.Status status : result.getTweets()) {
                    tweets.add(status);
                }
                if (result2 != null) {
                    for (twitter4j.Status status : result2.getTweets()) {
                        tweets.add(status);
                    }
                    if (result3 != null) {
                        for (twitter4j.Status status : result3.getTweets()) {
                            tweets.add(status);
                        }
                    }
                }

                return tweets;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.Status> searches) {

            if (searches != null) {
                listView.setAdapter(new TimelineArrayAdapter(context, searches));
                listView.setVisibility(View.VISIBLE);
            }

            spinner.setVisibility(View.GONE);
        }
    }

    class DoUserSearch extends AsyncTask<String, Void, ArrayList<User>> {

        String mQuery;

        public DoUserSearch(String query) {
            this.mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            listView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
        }

        protected ArrayList<User> doInBackground(String... urls) {
            try {
                Log.v("inside_user_search", mQuery);

                Twitter twitter = Utils.getTwitter(context, settings);
                ResponseList<User> result = twitter.searchUsers(mQuery, 1);

                if (result.size() == 20) {
                    result.addAll(twitter.searchUsers(mQuery, 2));

                    if (result.size() == 40) {
                        result.addAll(twitter.searchUsers(mQuery, 3));
                    }
                }

                ArrayList<User> users = new ArrayList<User>();

                for (User u : result) {
                    users.add(u);
                }

                return users;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<User> searches) {

            if (searches != null) {
                listView.setAdapter(new PeopleArrayAdapter(context, searches));
                listView.setVisibility(View.VISIBLE);
            }

            spinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        /*try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }*/

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
}