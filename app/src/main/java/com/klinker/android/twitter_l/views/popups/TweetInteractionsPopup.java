package com.klinker.android.twitter_l.views.popups;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TweetInteractionsPagerAdapter;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;
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

        if (AppSettings.isWhiteToolbar(getContext())) {
            tabs.setTabTextColors(ColorStateList.valueOf(getResources().getColor(R.color.light_status_bar_color)));
        } else {
            tabs.setTabTextColors(ColorStateList.valueOf(Color.WHITE));
        }

        return root;
    }

    public void setInfo(String screenname, long tweetId) {
        TweetInteractionsPagerAdapter adapter = new TweetInteractionsPagerAdapter(((AppCompatActivity) getContext()), screenname, tweetId);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(3);
        tabs.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(1);
    }
}
