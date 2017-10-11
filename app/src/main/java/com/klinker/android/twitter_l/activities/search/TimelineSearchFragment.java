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
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class TimelineSearchFragment extends Fragment {

    private ListView listView;
    private LinearLayout spinner;

    private Context context;
    private AppSettings settings;

    public String searchQuery;
    public boolean translucent;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    public TimelineSearchFragment() {

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
    public View noContent;
    public TextView noContentText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, null);

        translucent = getArguments().getBoolean("translucent", false);
        searchQuery = getArguments().getString("search");

        settings = AppSettings.getInstance(context);

        inflater = LayoutInflater.from(context);
        layout = inflater.inflate(R.layout.ptr_list_layout, null);
        noContent = layout.findViewById(R.id.no_content);
        noContentText = (TextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getString(R.string.no_tweets));

        mPullToRefreshLayout = (MaterialSwipeRefreshLayout) layout.findViewById(R.id.swipe_refresh_layout);
        mPullToRefreshLayout.setOnRefreshListener(new MaterialSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onRefreshStarted();
            }
        });

        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int size = Utils.getActionBarHeight(context) + (landscape ? 0 : Utils.getStatusBarHeight(context));
        mPullToRefreshLayout.setProgressViewOffset(false, -1 * toDP(64), toDP(25));
        mPullToRefreshLayout.setColorSchemeColors(settings.themeColors.accentColor, settings.themeColors.primaryColor);

        listView = (ListView) layout.findViewById(R.id.listView);

        if (settings.revampedTweetLayout) {
            listView.setDivider(null);
        }

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (getResources().getBoolean(R.bool.has_drawer) ? Utils.getNavBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

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

        new TimeoutThread(new Runnable() {
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

                                try {
                                    listView.setAdapter(adapter);
                                } catch (Exception e) {

                                }

                                listView.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.GONE);

                                if (cursor.getCount() == 0) {
                                    noContent.setVisibility(View.VISIBLE);
                                } else {
                                    noContent.setVisibility(View.GONE);
                                }
                            }
                        });
                    } else {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                                noContent.setVisibility(View.VISIBLE);
                            }
                        });
                    }

                } catch (Exception e) {
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            noContent.setVisibility(View.VISIBLE);
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