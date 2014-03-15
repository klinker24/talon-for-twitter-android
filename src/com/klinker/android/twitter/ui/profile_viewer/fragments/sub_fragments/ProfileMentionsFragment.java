package com.klinker.android.twitter.ui.profile_viewer.fragments.sub_fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;


public class ProfileMentionsFragment extends Fragment {

    public View layout;
    public Context context;
    public AppSettings settings;
    public SharedPreferences sharedPrefs;

    public AsyncListView listView;
    public LinearLayout spinner;

    public LayoutInflater inflater;

    public String screenName;

    public ProfileMentionsFragment(String screenName) {
        this.screenName = screenName;
    }

    public ProfileMentionsFragment() {
        this.screenName = "";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        settings = AppSettings.getInstance(context);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        this.inflater = LayoutInflater.from(context);

        layout = inflater.inflate(R.layout.list_fragment, null);

        listView = (AsyncListView) layout.findViewById(R.id.listView);
        spinner = (LinearLayout) layout.findViewById(R.id.spinner);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;

                if(lastItem == totalItemCount && canRefresh && hasMore) {
                    getMore();
                }
            }
        });

        doSearch();

        return layout;
    }

    public ArrayList<Status> tweets = new ArrayList<Status>();
    public boolean hasMore = true;
    public boolean canRefresh = false;
    public TimelineArrayAdapter adapter;
    public Query query;

    public void doSearch() {
        spinner.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);

                    query = new Query("@" + screenName + " -RT");
                    query.sinceId(1);
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
                            canRefresh = true;

                            if (!hasMore) {
                                View footer = inflater.inflate(R.layout.mentions_footer, null);
                                listView.addFooterView(footer);
                                listView.setFooterDividersEnabled(false);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            spinner.setVisibility(View.GONE);
                            canRefresh = false;

                            if (!hasMore) {
                                View footer = inflater.inflate(R.layout.mentions_footer, null);
                                listView.addFooterView(footer);
                                listView.setFooterDividersEnabled(false);
                            }
                        }
                    });

                }

            }
        }).start();
    }

    public void getMore() {
        canRefresh = false;

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
                            canRefresh = true;

                            if (!hasMore) {
                                View footer = inflater.inflate(R.layout.mentions_footer, null);
                                listView.addFooterView(footer);
                                listView.setFooterDividersEnabled(false);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = false;
                            hasMore = false;
                        }
                    });

                }

            }
        }).start();
    }
}
