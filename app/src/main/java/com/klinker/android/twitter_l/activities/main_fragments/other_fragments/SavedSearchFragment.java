package com.klinker.android.twitter_l.activities.main_fragments.other_fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.trends.SearchedTrendsActivity;
import com.klinker.android.twitter_l.activities.main_fragments.MainFragment;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;

public class SavedSearchFragment extends MainFragment {

    private String search;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        search = getArguments().getString("saved_search", "");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void setUpListScroll() {

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
    }

    public ArrayList<twitter4j.Status> tweets = new ArrayList<Status>();
    public TimelineArrayAdapter adapter;
    public Query query;
    public boolean hasMore;

    @Override
    public void getCursorAdapter(boolean showSpinner) {
        if (showSpinner) {
            listView.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);
        }

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
                    query = new Query(search);
                    query.setCount(SearchedTrendsActivity.TWEETS_PER_REFRESH);
                    QueryResult result = twitter.search(query);

                    tweets.clear();

                    for (twitter4j.Status status : result.getTweets()) {
                        tweets.add(status);
                    }

                    if (tweets.size() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
                        query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    try {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if (!isAdded()) {
                                    return;
                                }

                                int top = 0;
                                for (int i = 0; i < tweets.size(); i++) {
                                    if (tweets.get(i).getId() == topId) {
                                        top = i;
                                        break;
                                    }
                                }

                                adapter = new TimelineArrayAdapter(context, tweets);
                                listView.setAdapter(adapter);
                                listView.setVisibility(View.VISIBLE);
                                listView.setSelection(top);

                                spinner.setVisibility(View.GONE);

                                refreshLayout.setRefreshing(false);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                                refreshLayout.setRefreshing(false);
                            }
                        });
                    } catch (Exception x) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public boolean canRefresh = true;
    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            refreshLayout.setRefreshing(true);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Twitter twitter = Utils.getTwitter(context, settings);
                        QueryResult result = twitter.search(query);

                        List<Status> statuses = result.getTweets();
                        for (twitter4j.Status status : statuses) {
                            tweets.add(status);
                        }

                        if (statuses.size() == SearchedTrendsActivity.TWEETS_PER_REFRESH) {
                            query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));
                            hasMore = true;
                        } else {
                            hasMore = false;
                        }

                        try {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (adapter != null) {
                                        adapter.notifyDataSetChanged();
                                    }

                                    refreshLayout.setRefreshing(false);
                                    canRefresh = true;
                                }
                            });
                        } catch (Exception e) {

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    refreshLayout.setRefreshing(false);
                                    canRefresh = true;
                                }
                            });
                        }catch (Exception x) {

                        }
                    }
                }
            }).start();
        }
    }
}
