package com.klinker.android.twitter_l.views.popups.profile;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.*;

import java.util.ArrayList;
import java.util.List;

public abstract class ProfileListPopupLayout extends PopupLayout {

    protected ListView list;
    protected LinearLayout spinner;

    protected User user;

    public ArrayList<Status> tweets = new ArrayList<Status>();
    public Paging paging = new Paging(1, 20);
    public boolean canRefresh = false;
    public TimelineArrayAdapter adapter;

    protected boolean hasLoaded = false;

    public ProfileListPopupLayout(Context context, View main, User user) {
        super(context);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        //setTitle(getTitle());
        showTitle(false);
        setFullScreen();

        if (getResources().getBoolean(R.bool.isTablet)) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setWidthByPercent(.6f);
                setHeightByPercent(.8f);
            } else {
                setWidthByPercent(.85f);
                setHeightByPercent(.68f);
            }
            setCenterInScreen();
        }

        this.user = user;

        content.addView(main);

        setUpList();
    }

    @Override
    public View setMainLayout() {
        return null;
    }

    public void setUpList() {

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
        params.width = width;
        spinner.setLayoutParams(params);

        list.setOnScrollListener(new AbsListView.OnScrollListener() {
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

    public void findTweets() {
        list.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        TimeoutThread data = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

                    final List<Status> result = getData(twitter);

                    if (result == null) {
                        ((Activity)getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                                canRefresh = false;
                            }
                        });
                    }

                    tweets.clear();

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new TimelineArrayAdapter(getContext(), tweets);
                            adapter.setCanUseQuickActions(false);

                            list.setAdapter(adapter);
                            list.setVisibility(View.VISIBLE);

                            spinner.setVisibility(View.GONE);

                            if (!(ProfileListPopupLayout.this instanceof ProfileMentionsPopup)) {
                                if (result.size() > 17) {
                                    canRefresh = true;
                                } else {
                                    canRefresh = false;
                                }
                            } else {
                                canRefresh = true;
                            }

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
        });

        data.setPriority(8);
        data.start();
    }

    public void getMore() {
        canRefresh = false;

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

                    final boolean more = incrementQuery();

                    if (!more) {
                        canRefresh = false;
                        return;
                    }

                    final List<Status> result = getData(twitter);

                    for (twitter4j.Status status : result) {
                        tweets.add(status);
                    }

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();

                            if (!(ProfileListPopupLayout.this instanceof ProfileMentionsPopup)) {
                                if (result.size() > 17) {
                                    canRefresh = true;
                                } else {
                                    canRefresh = false;
                                }
                            } else {
                                canRefresh = true;
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = false;
                        }
                    });
                }

            }
        }).start();
    }

    @Override
    public void show() {
        super.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hasLoaded) {
                    hasLoaded = true;
                    findTweets();
                }
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME );

    }

    public abstract boolean incrementQuery();
    public abstract String getTitle();
    public abstract List<Status> getData(Twitter twitter);

}
