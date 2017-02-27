package com.klinker.android.twitter_l.views.popups.profile;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ListsArrayAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.User;
import twitter4j.UserList;

public class ProfileUsersListsPopup extends PopupLayout {

    protected ListView list;
    protected LinearLayout spinner;

    protected User user;

    public List<UserList> lists = new ArrayList<>();
    public ListsArrayAdapter adapter;

    protected boolean hasLoaded = false;

    public ProfileUsersListsPopup(Context context, User user) {
        super(context);

        View main = LayoutInflater.from(context).inflate(R.layout.convo_popup_layout, null, false);

        list = (ListView) main.findViewById(R.id.listView);
        spinner = (LinearLayout) main.findViewById(R.id.spinner);

        setTitle(context.getString(R.string.lists));
        showTitle(true);
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
    }

    public void findLists() {
        list.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);

        TimeoutThread data = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter = Utils.getTwitter(getContext(), AppSettings.getInstance(getContext()));
                    final List<twitter4j.UserList> result = twitter.getUserLists(user.getId());

                    if (result == null) {
                        ((Activity)getContext()).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    }

                    lists.clear();
                    lists.addAll(result);

                    ((Activity)getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new ListsArrayAdapter(getContext(), lists);

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
                        }
                    });

                }
            }
        });

        data.setPriority(8);
        data.start();
    }

    @Override
    public void show() {
        super.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!hasLoaded) {
                    hasLoaded = true;
                    findLists();
                }
            }
        }, 2 * LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME );

    }

}
