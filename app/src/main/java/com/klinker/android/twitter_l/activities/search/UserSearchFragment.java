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
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material.MaterialSwipeRefreshLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.User;


public class UserSearchFragment extends Fragment {

    private ListView listView;
    private LinearLayout spinner;

    private Context context;
    private AppSettings settings;

    private boolean translucent;

    public String searchQuery;
    public boolean onlyProfile;

    private MaterialSwipeRefreshLayout mPullToRefreshLayout;

    public UserSearchFragment() {
        this.translucent = false;
        this.searchQuery = "";
        this.onlyProfile = false;
    }

    private BroadcastReceiver newSearch = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            searchQuery = intent.getStringExtra("query");
            searchQuery = searchQuery.replace(" -RT", "");

            doUserSearch(searchQuery);
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

    public View noContent;
    public TextView noContentText;
    public View layout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.translucent = getArguments().getBoolean("translucent", false);
        this.searchQuery = getArguments().getString("search").replaceAll("@", "");
        searchQuery = searchQuery.replace(" TOP", "");
        searchQuery = searchQuery.replace(" -RT", "");
        this.onlyProfile = getArguments().getBoolean("only_profile", false);

        settings = AppSettings.getInstance(context);

        inflater = LayoutInflater.from(context);
        layout = inflater.inflate(R.layout.ptr_list_layout, null);
        noContent = layout.findViewById(R.id.no_content);
        noContentText = (TextView) layout.findViewById(R.id.no_retweeters_text);

        noContentText.setText(getString(R.string.no_users));

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
                    getMoreUsers(searchQuery.replace("@", ""));
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

        doUserSearch(searchQuery);

        if (onlyProfile) {
            ProfilePager.start(context, searchQuery.replace("@", "").replaceAll(" ", ""));
            getActivity().finish();
        }

        return layout;
    }

    public void onRefreshStarted() {
        mPullToRefreshLayout.setRefreshing(false);
    }

    public ArrayList<User> users;
    public int userPage = 1;
    public PeopleArrayAdapter peopleAdapter;

    public void doUserSearch(final String mQuery) {
        listView.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
        hasMore = true;
        canRefresh = false;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {

                    Twitter twitter = Utils.getTwitter(context, settings);
                    ResponseList<User> result = twitter.searchUsers(mQuery, userPage);

                    userPage++;

                    if (result.size() < 18) {
                        hasMore = false;
                        canRefresh = false;
                    }

                    users = new ArrayList<User>();

                    for (User u : result) {
                        users.add(u);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peopleAdapter = new PeopleArrayAdapter(context, users, false);
                            listView.setAdapter(peopleAdapter);
                            listView.setVisibility(View.VISIBLE);

                            spinner.setVisibility(View.GONE);

                            if (peopleAdapter.getCount() == 0) {
                                noContent.setVisibility(View.VISIBLE);
                            } else {
                                noContent.setVisibility(View.GONE);
                            }

                            canRefresh = true;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            noContent.setVisibility(View.VISIBLE);
                        }
                    });
                    hasMore = false;

                    canRefresh = true;
                }
            }
        }).start();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public boolean canRefresh = true;
    public boolean hasMore;

    public void getMoreUsers(final String mQuery) {
        if (hasMore) {
            canRefresh = false;
            mPullToRefreshLayout.setRefreshing(true);
            new TimeoutThread(new Runnable() {
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
                                if (peopleAdapter != null) {
                                    peopleAdapter.notifyDataSetChanged();
                                }
                                mPullToRefreshLayout.setRefreshing(false);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();

                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mPullToRefreshLayout.setRefreshing(false);
                            }
                        });
                        hasMore = false;
                    }
                }
            }).start();
        }
    }
}