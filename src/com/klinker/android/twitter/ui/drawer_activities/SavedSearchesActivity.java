package com.klinker.android.twitter.ui.drawer_activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ListsArrayAdapter;
import com.klinker.android.twitter.adapters.TrendsArrayAdapter;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import twitter4j.ResponseList;
import twitter4j.SavedSearch;
import twitter4j.Twitter;
import twitter4j.UserList;

/**
 * Created by luke on 2/21/14.
 */
public class SavedSearchesActivity extends DrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = AppSettings.getInstance(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setUpTheme();
        setContentView(R.layout.twitter_lists_page);
        setUpDrawer(8, getResources().getString(R.string.saved_searches));

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.saved_searches));

        listView = (AsyncListView) findViewById(R.id.listView);
        View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);
        listView.setHeaderDividersEnabled(false);

        if (DrawerActivity.translucent) {
            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                listView.addFooterView(footer);
                listView.setFooterDividersEnabled(false);
            }
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

    public void getSearches() {
        new Thread(new Runnable() {
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

                    Log.v("talon_searches", "got saved searches");

                    for (SavedSearch sear : searches) {
                        Log.v("talon_searches", sear.getName());
                        searchNames.add(sear.getName());
                    }

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setAdapter(new TrendsArrayAdapter(context, searchNames));
                            listView.setVisibility(View.VISIBLE);

                            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
                            spinner.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, SavedSearchesActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }
}