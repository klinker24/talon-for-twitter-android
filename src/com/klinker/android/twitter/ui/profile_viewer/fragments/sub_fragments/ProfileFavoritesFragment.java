package com.klinker.android.twitter.ui.profile_viewer.fragments.sub_fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

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

public class ProfileFavoritesFragment extends Fragment {

    public View layout;
    public Context context;
    public AppSettings settings;
    public SharedPreferences sharedPrefs;

    public AsyncListView listView;
    public LinearLayout spinner;

    public String screenName;

    public ProfileFavoritesFragment(String screenName) {
        this.screenName = screenName;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        settings = new AppSettings(context);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        inflater = LayoutInflater.from(context);

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

                if(lastItem == totalItemCount && canRefresh) {
                    getMore();
                }
            }
        });

        doSearch();

        return layout;
    }

    public ArrayList<Status> tweets = new ArrayList<Status>();
    public Paging paging = new Paging(1, 20);
    public boolean hasMore = true;
    public boolean canRefresh = false;
    public TimelineArrayAdapter adapter;

    public void doSearch() {
        spinner.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(context, settings);

                    ResponseList<Status> result = twitter.getFavorites(screenName, paging);

                    tweets.clear();

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    if (result.size() > 17) {
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

                    paging.setPage(paging.getPage() + 1);

                    ResponseList<Status> result = twitter.getFavorites(screenName, paging);

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    if (result.size() > 17) {
                        hasMore = true;
                    } else {
                        hasMore = false;
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                            canRefresh = true;
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
