package com.klinker.android.twitter_l.manipulations;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TweetInteractionsPagerAdapter;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;

public class TweetInteractionsPopup extends PopupLayout {

    AppSettings settings;

    TabLayout tabs;
    ViewPager viewPager;

    public TweetInteractionsPopup(Context context) {
        super(context);

        showTitle(false);
        setFullScreen();
    }

    @Override
    public View setMainLayout() {
        settings = AppSettings.getInstance(getContext());

        View root = LayoutInflater.from(getContext()).inflate(R.layout.tweet_interactions_popup, (ViewGroup) getRootView(), false);

        tabs = (TabLayout) root.findViewById(R.id.pager_tab_strip);
        viewPager = (ViewPager) root.findViewById(R.id.pager);

        tabs.setBackgroundColor(settings.themeColors.primaryColor);
        tabs.setSelectedTabIndicatorColor(settings.themeColors.accentColor);
        tabs.setTabTextColors(Color.WHITE, Color.WHITE);

        return root;
    }

    public void setInfo(String screenname, long tweetId) {
        TweetInteractionsPagerAdapter adapter = new TweetInteractionsPagerAdapter(((Activity) getContext()), screenname, tweetId);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        tabs.setupWithViewPager(viewPager);
    }
}
