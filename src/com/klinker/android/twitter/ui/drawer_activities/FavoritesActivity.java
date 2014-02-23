package com.klinker.android.twitter.ui.drawer_activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.setup.LoginActivity;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class FavoritesActivity extends DrawerActivity {

    private boolean landscape;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = AppSettings.getInstance(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setUpTheme();
        setContentView(R.layout.retweets_activity);
        setUpDrawer(7, getResources().getString(R.string.favorite_tweets));

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.favorite_tweets));


        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);

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

            if (!MainActivity.isPopup) {
                View view = new View(context);
                view.setOnClickListener(null);
                view.setOnLongClickListener(null);
                ListView.LayoutParams params2 = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getStatusBarHeight(context));
                view.setLayoutParams(params2);
                listView.addHeaderView(view);
                listView.setFooterDividersEnabled(false);
            }
        }

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
                    getFavorites();
                }

                // show and hide the action bar
                if (firstVisibleItem != 0) {
                    if (MainActivity.canSwitch) {
                        // used to show and hide the action bar
                        if (firstVisibleItem > mLastFirstVisibleItem) {
                            if(!landscape && !isTablet) {
                                actionBar.hide();
                            }
                        } else if (firstVisibleItem < mLastFirstVisibleItem) {
                            if(!landscape && !isTablet) {
                                actionBar.show();
                            }
                            if (translucent) {
                                statusBar.setVisibility(View.VISIBLE);
                            }
                        }

                        mLastFirstVisibleItem = firstVisibleItem;
                    }
                } else {
                    if(!landscape && !isTablet) {
                        actionBar.show();
                    }
                }

                if (MainActivity.translucent && actionBar.isShowing()) {
                    showStatusBar();
                } else if (MainActivity.translucent) {
                    hideStatusBar();
                }
            }
        });

        getFavorites();

        final LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.VISIBLE);

    }

    public boolean canRefresh = false;
    public Paging paging = new Paging(1, 20);
    public TimelineArrayAdapter adapter;
    public ArrayList<Status> statuses = new ArrayList<Status>();
    public boolean hasMore = true;

    public void getFavorites() {
        if (!hasMore) {
            return;
        }
        canRefresh = false;
        final LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    final ResponseList<twitter4j.Status> favs = twitter.getFavorites(paging);

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
                                    adapter = new TimelineArrayAdapter(context, statuses, TimelineArrayAdapter.FAVORITE);
                                    listView.setAdapter(adapter);
                                    listView.setVisibility(View.VISIBLE);
                                } else {
                                    LinearLayout nothing = (LinearLayout) findViewById(R.id.no_content);
                                    nothing.setVisibility(View.VISIBLE);
                                    listView.setVisibility(View.GONE);
                                    hasMore = false;
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, FavoritesActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}