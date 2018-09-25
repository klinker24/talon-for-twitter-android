package com.klinker.android.twitter_l.activities.drawer_activities.discover.people;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.adapters.PeopleArrayAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.setup.material_login.MaterialLogin;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.User;

public class PeopleSearch extends WhiteToolbarActivity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private androidx.appcompat.app.ActionBar actionBar;

    private ListView listView;

    public String slug;

    private LinearLayout spinner;

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_up, R.anim.activity_slide_down);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        overridePendingTransition(R.anim.activity_slide_up, R.anim.activity_slide_down);

        slug = getIntent().getStringExtra("slug");

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        setUpWindow();

        Utils.setUpMainTheme(this, settings);

        setContentView(R.layout.ptr_list_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(settings.themeColors.primaryColor);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        actionBar = getSupportActionBar();

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, MaterialLogin.class);
            startActivity(login);
            finish();
        }

        spinner = (LinearLayout) findViewById(R.id.list_progress);

        listView = (ListView) findViewById(R.id.listView);

        View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);
        listView.setHeaderDividersEnabled(false);

        getPeople();
    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    public void getPeople() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    ResponseList<User> usersResponse = twitter.getUserSuggestions(slug);

                    users.clear();
                    for(User i : usersResponse) {
                        users.add(i);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new PeopleArrayAdapter(context, users);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);

                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();

                } catch (OutOfMemoryError e) {
                    e.printStackTrace();

                }
            }
        }).start();
    }

    private ArrayList<User> users = new ArrayList<User>();
    private PeopleArrayAdapter adapter;
}