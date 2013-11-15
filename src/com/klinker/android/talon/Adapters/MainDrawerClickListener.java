package com.klinker.android.talon.Adapters;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;

public class MainDrawerClickListener implements AdapterView.OnItemClickListener {

    private Context context;
    private DrawerLayout drawer;
    private ViewPager viewPager;

    public MainDrawerClickListener(Context context, DrawerLayout drawer, ViewPager viewPager) {
        this.context = context;
        this.drawer = drawer;
        this.viewPager = viewPager;
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (i < 3) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    drawer.closeDrawer(Gravity.START);
                }
            }, 300);

            viewPager.setCurrentItem(i, true);
        } else {
            final int pos = i;
            drawer.closeDrawer(Gravity.START);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = null;

                    switch (pos) {

                    }

                    try {
                        Thread.sleep(400);
                    } catch (Exception e) {

                    }
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {

                    }

                }
            }).start();
        }

    }
}
