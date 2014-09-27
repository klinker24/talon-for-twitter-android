package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.PicturesArrayAdapter;
import com.klinker.android.twitter_l.adapters.PicturesGridAdapter;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.User;

/**
 * Created by luke on 9/26/14.
 */
public class PicturesPopup extends PopupLayout {

    GridView listView;
    LinearLayout spinner;

    private User user;

    public PicturesPopup(Context context, User user) {
        super(context);

        this.user = user;

        setUp();
    }

    public PicturesPopup(Context context, boolean windowed, User user) {
        super(context, windowed);

        this.user = user;

        setUp();
    }

    @Override
    public View setMainLayout() {
        return null;
    }

    public ArrayList<Status> tweets = new ArrayList<Status>();
    public ArrayList<String> pics = new ArrayList<String>();
    public ArrayList<Status> tweetsWithPics = new ArrayList<Status>();
    public Paging paging = new Paging(1, 60);
    public boolean canRefresh = false;
    public PicturesGridAdapter adapter;

    private void setUp() {
        setFullScreen();
        setTitle(getContext().getString(R.string.pictures));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
        params.width = width;
        spinner.setLayoutParams(params);

        View root = ((Activity)getContext()).getLayoutInflater().inflate(R.layout.picture_popup_layout, null, false);

        listView = (GridView) root.findViewById(R.id.gridView);
        spinner = (LinearLayout) root.findViewById(R.id.spinner);

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

        spinner.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);

        doSearch();

        content.addView(root);
    }

    public void doSearch() {
        spinner.setVisibility(View.VISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

                    ResponseList<Status> result;
                    try {
                        result = twitter.getUserTimeline(user.getScreenName(), paging);
                    } catch (OutOfMemoryError e) {
                        return;
                    }

                    tweets.clear();

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    for (Status s : tweets) {
                        String[] links = TweetLinkUtils.getLinksInStatus(s);
                        if (!links[1].equals("")) {
                            pics.add(links[1]);
                            tweetsWithPics.add(s);
                        }
                    }

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            int numColumns;

                            int currentOrientation = getResources().getConfiguration().orientation;
                            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                                numColumns = 5;
                            } else {
                                numColumns = 3;
                            }

                            adapter = new PicturesGridAdapter(getContext(), pics, tweetsWithPics, width / numColumns);
                            listView.setNumColumns(numColumns);
                            listView.setAdapter(adapter);

                            if (tweetsWithPics.size() > 0) {
                                listView.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.GONE);
                            }
                            canRefresh = true;

                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)getContext()).runOnUiThread(new Runnable() {
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
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

                    paging.setPage(paging.getPage() + 1);

                    ResponseList<Status> result = twitter.getUserTimeline(user.getScreenName(), paging);

                    tweets.clear();

                    for (Status status : result) {
                        tweets.add(status);
                    }

                    boolean update = false;

                    for (Status s : tweets) {
                        String[] links = TweetLinkUtils.getLinksInStatus(s);
                        if (!links[1].equals("")) {
                            pics.add(links[1]);
                            tweetsWithPics.add(s);
                            update = true;
                        }
                    }

                    if (update) {
                        ((Activity)getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                canRefresh = true;

                                listView.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        canRefresh = true;
                    }

                    try {
                        Thread.sleep(250);
                    } catch (Exception e) {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = false;

                            try {
                                adapter.notifyDataSetChanged();
                            } catch (Exception e) {

                            }

                            spinner.setVisibility(View.GONE);
                        }
                    });

                }

            }
        }).start();
    }

}
