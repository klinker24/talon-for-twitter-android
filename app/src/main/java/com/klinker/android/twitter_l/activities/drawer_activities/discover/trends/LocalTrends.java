package com.klinker.android.twitter_l.activities.drawer_activities.discover.trends;
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
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TrendsArrayAdapter;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;

import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Trend;
import twitter4j.Twitter;


public class LocalTrends extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private boolean connected = false;

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

        buildGoogleApiClient();

        getTrends();

        return layout;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    Location mLastLocation;

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("location", "connected");
        connected = true;
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
    }

    public void getTrends() {

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, DrawerActivity.settings);

                    int i = 0;
                    while (!connected && i < 5) {
                        try {
                            Thread.sleep(1500);
                        } catch (Exception e) {

                        }

                        i++;
                    }

                    twitter4j.Trends trends;

                    if (sharedPrefs.getBoolean("manually_config_location", false)) {
                        trends = twitter.getPlaceTrends(sharedPrefs.getInt("woeid", 2379574)); // chicago to default
                    } else {
                        Location location = mLastLocation;

                        ResponseList<twitter4j.Location> locations = twitter.getClosestTrends(new GeoLocation(location.getLatitude(),location.getLongitude()));
                        trends = twitter.getPlaceTrends(locations.get(0).getWoeid());
                    }

                    final ArrayList<String> currentTrends = new ArrayList<String>();

                    for(Trend t: trends.getTrends()){
                        String name = t.getName();
                        currentTrends.add(name);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (currentTrends != null) {
                                    listView.setAdapter(new TrendsArrayAdapter(context, currentTrends));
                                    listView.setVisibility(View.VISIBLE);
                                } else {
                                    Toast.makeText(context, getResources().getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                                }

                                LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                                spinner.setVisibility(View.GONE);
                            } catch (Exception e) {
                                // not attached to activity
                            }
                        }
                    });

                    HashtagDataSource source = HashtagDataSource.getInstance(context);

                    for (String s : currentTrends) {
                        Log.v("talon_hashtag", "trend: " + s);
                        if (s.contains("#")) {
                            // we want to add it to the auto complete
                            Log.v("talon_hashtag", "adding: " + s);

                            // could be much more efficient by querying and checking first, but I
                            // just didn't feel like it when there is only ever 10 of them here
                            source.deleteTag(s);

                            // add it to the userAutoComplete database
                            source.createTag(s);
                        }
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, getResources().getString(R.string.no_location), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                // not attached to activity
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