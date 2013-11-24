package com.klinker.android.talon.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;


public class SettingsDrawerClickListener implements ListView.OnItemClickListener {

    private Context context;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private LinearLayout mDrawer;
    private ViewPager viewPager;

    public SettingsDrawerClickListener(Context context, DrawerLayout drawerLayout, ListView drawerList, ViewPager vp, LinearLayout drawer) {
        this.context = context;
        this.mDrawerLayout = drawerLayout;
        this.mDrawerList = drawerList;
        this.mDrawer = drawer;
        this.viewPager = vp;
    }
    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {

        Intent intent;

        final int mPos = position;

        if (mPos < 5) { // one of the settings pages
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(Gravity.START);
                }
            }, 300);

            viewPager.setCurrentItem(mPos, true);
        } else if (mPos == 5) {

        } else if (mPos == 6) { // rate it option
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

                    try {
                        context.startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(context, "Couldn't launch the market", Toast.LENGTH_SHORT).show();
                    }
                }
            }, 200);

        }
    }
}