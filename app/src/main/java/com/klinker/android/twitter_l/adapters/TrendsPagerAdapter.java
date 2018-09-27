package com.klinker.android.twitter_l.adapters;
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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import androidx.legacy.app.FragmentPagerAdapter;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.NearbyTweets;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.people.CategoryFragment;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.trends.LocalTrends;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.trends.WorldTrends;

public class TrendsPagerAdapter extends FragmentPagerAdapter {

    private Context context;

    public TrendsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                LocalTrends local = new LocalTrends();
                return local;
            case 1:
                WorldTrends world = new WorldTrends();
                return world;
            case 2:
                NearbyTweets nearby = new NearbyTweets();
                return nearby;
            case 3:
                CategoryFragment people = new CategoryFragment();
                return people;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getResources().getString(R.string.local_trends);
            case 1:
                return context.getResources().getString(R.string.world_trends);
            case 2:
                return context.getResources().getString(R.string.nearby_tweets);
            case 3:
                return context.getResources().getString(R.string.discover_people);
        }
        return null;
    }
}
