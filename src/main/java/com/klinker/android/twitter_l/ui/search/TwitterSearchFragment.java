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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.drawer_activities.discover.trends.SearchedTrendsActivity;
import com.klinker.android.twitter_l.utils.Expandable;
import com.klinker.android.twitter_l.utils.ExpansionViewHelper;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;

public class TwitterSearchFragment extends Fragment implements Expandable {

    private ListView listView;
    private LinearLayout spinner;

    private Context context;
    private AppSettings settings;

    private boolean translucent;

    public String searchQuery;
    public boolean onlyStatus;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    public TwitterSearchFragment() {

    }

    public boolean topTweets = false;

    private BroadcastReceiver newSearch = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            searchQuery = intent.getStringExtra("query");

            if (searchQuery.contains(" TOP")) {
                topTweets = true;
                searchQuery = searchQuery.replace(" TOP", "");
            } else {
                topTweets = false;
            }

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

        this.translucent = getArguments().getBoolean("translucent", false);
        this.searchQuery = getArguments().getString("search");
        this.onlyStatus = getArguments().getBoolean("only_status", false);

        settings = AppSettings.getInstance(context);

        inflater = LayoutInflater.from(context);
        layout = inflater.inflate(R.layout.ptr_list_layout, null);

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

        if (onlyStatus) {
            try {
                findStatus(Long.parseLong(searchQuery));
            } catch (Exception e) {

            }
        } else {
            doSearch(searchQuery);
        }

        return layout;
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
                    query.setCount(SearchedTrendsActivity.TWEETS_PER_REFRESH);

                    if (topTweets) {
                        query.setResultType(Query.POPULAR);
                    } else {
                        query.setResultType(null);
                    }
                    QueryResult result = twitter.search(query);

                    tweets.clear();

                    for (twitter4j.Status status : result.getTweets()) {
                        tweets.add(status);
                    }

                    if (result.getCount() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
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

                            adapter = new TimelineArrayAdapter(context, tweets, onlyStatus);
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

    public void doSearch(final String mQuery) {
        spinner.setVisibility(View.VISIBLE);

        if (listView.getVisibility() != View.GONE) {
            listView.setVisibility(View.GONE);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);
                    Log.v("talon_searching", "query in frag: " + mQuery);
                    query = new Query(mQuery);
                    query.setCount(SearchedTrendsActivity.TWEETS_PER_REFRESH);
                    if (topTweets) {
                        query.setResultType(Query.ResultType.popular);
                    } else {
                        query.setResultType(null);
                    }
                    QueryResult result = twitter.search(query);

                    tweets.clear();

                    for (twitter4j.Status status : result.getTweets()) {
                        tweets.add(status);
                    }

                    if (result.getCount() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
                        query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new TimelineArrayAdapter(context, tweets, onlyStatus);
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
                } catch (OutOfMemoryError e) {
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

                    final ArrayList<Status> statuses = new ArrayList<Status>();

                    statuses.add(status);

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TimelineArrayAdapter adapter = new TimelineArrayAdapter(context, statuses, onlyStatus);
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

                        if (result.getCount() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
                            query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));
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

    private int expandedDistanceFromTop = 0;

    @Override
    public void expandViewOpen(final int distanceFromTop, int position, View root, ExpansionViewHelper helper) {
        expandedDistanceFromTop = distanceFromTop;

        listView.smoothScrollBy(distanceFromTop - Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context), TimeLineCursorAdapter.ANIMATION_DURATION);
    }

    @Override
    public void expandViewClosed(int currentDistanceFromTop) {

        if (currentDistanceFromTop != -1) {
            listView.smoothScrollBy(-1 * expandedDistanceFromTop + currentDistanceFromTop, TimeLineCursorAdapter.ANIMATION_DURATION);
        }
    }
}