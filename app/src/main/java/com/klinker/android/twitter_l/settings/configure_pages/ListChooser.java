package com.klinker.android.twitter_l.settings.configure_pages;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Collections;
import java.util.Comparator;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.UserList;


public class ListChooser extends AppCompatActivity {

    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;
    private android.support.v7.app.ActionBar actionBar;

    private ListView listView;
    private ListChooserArrayAdapter arrayAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        Utils.setUpTheme(context, settings);
        setContentView(R.layout.list_chooser);

        actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    i = i - 1;
                }

                UserList list = arrayAdapter.getItem(i);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("listId", list.getId());
                returnIntent.putExtra("listName", list.getName());
                setResult(RESULT_OK,returnIntent);
                finish();
            }
        });

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            View status = findViewById(R.id.activity_status_bar);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) status.getLayoutParams();
            params.height = Utils.getStatusBarHeight(context);
            status.setLayoutParams(params);

            status.setVisibility(View.VISIBLE);

            View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
            listView.addHeaderView(viewHeader, null, false);
            listView.setHeaderDividersEnabled(false);
        }

        new GetLists().execute();
    }

    class GetLists extends AsyncTask<String, Void, ResponseList<UserList>> {

        protected ResponseList<UserList> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myId);

                Collections.sort(lists, new Comparator<UserList>() {
                    public int compare(UserList result1, UserList result2) {
                        return result1.getName().compareTo(result2.getName());
                    }
                });

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(ResponseList<UserList> lists) {

            if (lists != null) {
                arrayAdapter = new ListChooserArrayAdapter(context, lists);
                listView.setAdapter(arrayAdapter);
                listView.setVisibility(View.VISIBLE);
            }

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

}
