package com.klinker.android.twitter.listeners;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.AdapterView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.widgets.NotificationDrawerLayout;

/**
 * Created by luke on 1/8/14.
 */
public class InteractionClickListener implements AdapterView.OnItemClickListener {

    private Context context;
    private NotificationDrawerLayout drawer;
    private ViewPager viewPager;
    private boolean noWait;
    private boolean extraPages;

    private SharedPreferences sharedPreferences;

    public InteractionClickListener(Context context, NotificationDrawerLayout drawer, ViewPager viewPager, boolean extraPages) {
        this.context = context;
        this.drawer = drawer;
        this.viewPager = viewPager;
        this.extraPages = extraPages;
        this.noWait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ||
                context.getResources().getBoolean(R.bool.isTablet);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
