package com.klinker.android.twitter_l.ui.search;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.CursorListLoader;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.FullScreenSwipeRefreshLayout;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.SwipeProgressBar;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import uk.co.senab.bitmapcache.BitmapLruCache;

public class TimelineSearchFragment extends Fragment {

    private AsyncListView listView;
    private LinearLayout spinner;

    private Context context;
    private AppSettings settings;

    public String searchQuery;
    public boolean translucent;

    private FullScreenSwipeRefreshLayout mPullToRefreshLayout;

    public TimelineSearchFragment(String query, boolean translucent) {
        this.searchQuery = query;
        this.translucent = translucent;
    }

    public TimelineSearchFragment() {
        this.searchQuery = "";
        translucent = false;
    }

    private BroadcastReceiver newSearch = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            searchQuery = intent.getStringExtra("query");
            searchQuery = searchQuery.replace(" TOP", "");
            doSearch(searchQuery);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.NEW_SEARCH");
        context.registerReceiver(newSearch, filter);
    }

    @Override
    public void onPause() {
        context.unregisterReceiver(newSearch);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public View layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, null);

        settings = AppSettings.getInstance(context);

        inflater = LayoutInflater.from(context);
        layout = inflater.inflate(R.layout.ptr_list_layout, null);

        mPullToRefreshLayout = (FullScreenSwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout);
        mPullToRefreshLayout.setFullScreen(false);
        mPullToRefreshLayout.setOnRefreshListener(new FullScreenSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });

        mPullToRefreshLayout.setColorScheme(settings.themeColors.primaryColor,
                SwipeProgressBar.COLOR2,
                settings.themeColors.primaryColorLight,
                SwipeProgressBar.COLOR3);

        listView = (AsyncListView) layout.findViewById(R.id.listView);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        CursorListLoader loader = new CursorListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        listView.setItemManager(builder.build());

        doSearch(searchQuery);

        return layout;
    }

    public void onRefreshStarted() {
        mPullToRefreshLayout.setRefreshing(false);
    }

    public void doSearch(final String mQuery) {
        spinner.setVisibility(View.VISIBLE);

        if (listView.getVisibility() != View.GONE) {
            listView.setVisibility(View.GONE);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String query = mQuery;

                boolean noRetweets = false;
                boolean onlyPics = false;

                if (mQuery.contains("-RT")) {
                    query = query.replace(" -RT", "");
                    noRetweets = true;
                }

                if (mQuery.contains("filter:links twitter.com")) {
                    query = query.replace(" filter:links twitter.com", "");
                    onlyPics = true;
                }

                String where = HomeSQLiteHelper.COLUMN_ACCOUNT + " = " + settings.currentAccount +
                        " AND " + HomeSQLiteHelper.COLUMN_TEXT + " LIKE '%" + query + "%'";

                if (onlyPics) {
                    where += " AND " + HomeSQLiteHelper.COLUMN_PIC_URL + " LIKE '%ht%'";
                }
                if (noRetweets) {
                    where += " AND " + HomeSQLiteHelper.COLUMN_RETWEETER + " = '' OR " + HomeSQLiteHelper.COLUMN_RETWEETER + " is NULL";
                }

                Log.v("talon_timeline_search", where);

                final Cursor cursor;
                try {
                    cursor = HomeDataSource.getInstance(context).getSearchCursor(where);

                    if (cursor != null) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter = new TimeLineCursorAdapter(context, cursor, false);

                                listView.setAdapter(adapter);

                                listView.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    }

                } catch (Exception e) {
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

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public TimeLineCursorAdapter adapter;
}