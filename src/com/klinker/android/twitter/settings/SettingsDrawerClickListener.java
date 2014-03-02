package com.klinker.android.twitter.settings;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.utils.IOUtils;


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