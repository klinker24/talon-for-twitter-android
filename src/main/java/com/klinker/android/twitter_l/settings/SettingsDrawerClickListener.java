package com.klinker.android.twitter_l.settings;
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
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;


public class SettingsDrawerClickListener implements ListView.OnItemClickListener {

    public Context context;

    public DrawerLayout mDrawerLayout;
    public ListView mDrawerList;
    public LinearLayout mDrawer;
    public ViewPager viewPager;

    public SettingsDrawerClickListener(Context context, DrawerLayout drawerLayout, ListView drawerList, ViewPager vp, LinearLayout drawer) {
        this.context = context;
        this.mDrawerLayout = drawerLayout;
        this.mDrawerList = drawerList;
        this.mDrawer = drawer;
        this.viewPager = vp;
    }
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mDrawerLayout.closeDrawer(Gravity.START);
            }
        }, 300);

        viewPager.setCurrentItem(position, true);

    }
}