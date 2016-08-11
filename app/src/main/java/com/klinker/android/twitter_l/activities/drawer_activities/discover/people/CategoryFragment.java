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
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.CategoriesArrayAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Collections;
import java.util.Comparator;

import twitter4j.Category;
import twitter4j.ResponseList;
import twitter4j.Twitter;


public class CategoryFragment extends Fragment {

    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;

    private ListView listView;
    private View layout;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(context);

        layout = inflater.inflate(R.layout.trends_list_view, null);

        listView = (ListView) layout.findViewById(R.id.listView);

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        } else if ((getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT,
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        getSuggestions();

        return layout;
    }

    public void getSuggestions() {

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, DrawerActivity.settings);

                    int i = 0;

                    final ResponseList<Category> categories = twitter.getSuggestedUserCategories();

                    Collections.sort(categories, new Comparator<Category>() {
                        public int compare(Category result1, Category result2) {
                            return result1.getName().compareTo(result2.getName());
                        }
                    });

                    try {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listView.setAdapter(new CategoriesArrayAdapter(context, categories));
                                listView.setVisibility(View.VISIBLE);

                                LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                                spinner.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {

                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, getResources().getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                            } catch (IllegalStateException e) {
                                // not attached
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}