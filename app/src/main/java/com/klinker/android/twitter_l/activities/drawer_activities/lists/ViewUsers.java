package com.klinker.android.twitter_l.activities.drawer_activities.lists;
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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;

import android.widget.ListView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.adapters.UserListMembersArrayAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.User;

public class ViewUsers extends WhiteToolbarActivity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ListView listView;
    private LinearLayout spinner;

    private boolean canRefresh = true;

    private long listId;
    private String listName;

    private long currCursor = -1;

    private boolean bigEnough = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = AppSettings.getInstance(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        listName = getIntent().getStringExtra("list_name");

        Utils.setUpTheme(this, settings);

        actionBar = getSupportActionBar();
        actionBar.setTitle(listName);

        setContentView(R.layout.list_view_activity);

        spinner = (LinearLayout) findViewById(R.id.list_progress);

        listView = (ListView) findViewById(R.id.listView);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getNavBarHeight(context) + Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        listView.setTranslationY(Utils.getStatusBarHeight(context) + Utils.getActionBarHeight(context));

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount) {
                    // Last item is fully visible.
                    if (canRefresh && bigEnough) {
                        new GetUsers().execute();
                    }

                    canRefresh = false;

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = true;
                        }
                    }, 4000);

                }
            }
        });

        listId = getIntent().getLongExtra("list_id", 0);

        new GetUsers().execute();

        Utils.setActionBar(context);
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    ArrayList<User> array;
    UserListMembersArrayAdapter people;

    class GetUsers extends AsyncTask<String, Void, ArrayList<User>> {

        protected ArrayList<User> doInBackground(String... urls) {

            if (array == null) {
                array = new ArrayList<User>();
            }

            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                PagableResponseList<User> users = twitter.getUserListMembers(listId, currCursor);

                currCursor = users.getNextCursor();

                for (User user : users) {
                    array.add(user);
                }

                bigEnough = users.size() > 16;

                return array;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<User> users) {
            if (users != null) {
                if (people == null) {
                    people = new UserListMembersArrayAdapter(context, users, listId);
                    listView.setAdapter(people);
                } else {
                    people.notifyDataSetChanged();
                }
            }

            spinner.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

}