package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ArrayListLoader;
import com.klinker.android.twitter_l.adapters.FollowersArrayAdapter;
import com.klinker.android.twitter_l.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.util.ArrayList;
import java.util.List;

public abstract class ProfileUsersPopup extends PopupLayout {
    protected AsyncListView list;
    protected LinearLayout spinner;

    protected User user;

    public ArrayList<User> users = new ArrayList<User>();
    public ArrayList<Long> followingIds = new ArrayList<Long>();

    public long cursor = -1;
    public boolean canRefresh = false;
    public ArrayAdapter<User> adapter;

    protected boolean hasLoaded = false;

    public ProfileUsersPopup(Context context, User user) {
        super(context);

        View main = ((Activity)context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);

        list = (AsyncListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(getTitle());

        setWidthByPercent(.6f);
        setHeightByPercent(.6f);

        this.user = user;

        content.addView(main);

        setUpList();
    }

    public ProfileUsersPopup(Context context, User user, boolean windowed) {
        super(context, windowed);

        View main = ((Activity)context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);

        list = (AsyncListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(getTitle());

        if (context.getResources().getBoolean(R.bool.isTablet)) {
            setWidthByPercent(.4f);
        } else {
            setWidthByPercent(.6f);
        }
        setHeightByPercent(.4f);

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

    public void findUsers() {
        list.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        Thread data = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

                    if (AppSettings.getInstance(getContext()).myId == user.getId() &&
                            followingIds == null &&
                            ProfileUsersPopup.this instanceof ProfileFollowersPopup) {

                        long currCursor = -1;
                        IDs idObject;
                        int rep = 0;

                        do {
                            idObject = twitter.getFriendsIDs(AppSettings.getInstance(getContext()).myId, currCursor);

                            long[] lIds = idObject.getIDs();
                            if (followingIds == null) {
                                followingIds = new ArrayList<Long>();
                            }
                            for (int i = 0; i < lIds.length; i++) {
                                followingIds.add(lIds[i]);
                            }

                            rep++;
                        } while ((currCursor = idObject.getNextCursor()) != 0 && rep < 3);
                    }

                    final PagableResponseList<User> result = getData(twitter, cursor);

                    if (result == null) {
                        ((Activity)getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                                canRefresh = false;
                            }
                        });

                        return;
                    }

                    users.clear();

                    for (twitter4j.User user : result) {
                        users.add(user);
                    }

                    if (result.hasNext()) {
                        cursor = result.getNextCursor();
                        canRefresh = true;
                    } else {
                        canRefresh = false;
                    }

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (followingIds == null) {
                                adapter = new PeopleArrayAdapter(getContext(), users);
                            } else {
                                adapter = new FollowersArrayAdapter(getContext(), users, followingIds);
                            }

                            list.setAdapter(adapter);

                            list.setVisibility(View.VISIBLE);
                            spinner.setVisibility(View.GONE);

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));

                    final PagableResponseList<User> result = getData(twitter, cursor);

                    for (twitter4j.User u : result) {
                        users.add(u);
                    }

                    if (result.hasNext()) {
                        cursor = result.getNextCursor();
                        canRefresh = true;
                    } else {
                        canRefresh = false;
                    }

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
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
                    findUsers();
                }
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME );

    }

    public abstract String getTitle();
    public abstract PagableResponseList<User> getData(Twitter twitter, long cursor);

}
