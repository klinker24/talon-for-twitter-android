
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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

import android.view.*;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.WhiteToolbarActivity;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;


public class ConfigurePagerActivity extends WhiteToolbarActivity {

    private ConfigurationPagerAdapter chooserAdapter;
    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;
    private androidx.appcompat.app.ActionBar actionBar;
    private ViewPager mViewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppSettings.getInstance(this).blackTheme) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);

        settings = AppSettings.getInstance(this);

        Utils.setUpTweetTheme(context, settings);
        setContentView(R.layout.configuration_activity);

        setUpDoneDiscard();

        actionBar = getSupportActionBar();
        actionBar.setElevation(0);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        chooserAdapter = new ConfigurationPagerAdapter(getFragmentManager(), context);

        mViewPager.setAdapter(chooserAdapter);
        mViewPager.setOverScrollMode(ViewPager.OVER_SCROLL_NEVER);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setBackgroundColor(settings.themeColors.primaryColor);
        tabLayout.setSelectedTabIndicatorColor(settings.themeColors.accentColor);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(mViewPager);

        if (AppSettings.isWhiteToolbar(this)) {
            tabLayout.setTabTextColors(ColorStateList.valueOf(lightStatusBarIconColor));
        } else {
            tabLayout.setTabTextColors(Color.WHITE, Color.WHITE);
        }

        mViewPager.setOffscreenPageLimit(6);

        if (sharedPrefs.getBoolean("show_performance_tip", true)) {
            new AlertDialog.Builder(context)
                    .setTitle("Timeline Tip")
                    .setMessage("With this version of Talon, you can completely customize your swipable timelines." +
                            "\n\n" +
                            "You can place up to 8 swipeable pages on the main screen of Talon, including lists, mentions, direct messages, your 'home' timeline, and some filtered timelines.")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Don't Show Again", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sharedPrefs.edit().putBoolean("show_performance_tip", false).apply();
                        }
                    })
                    .create().show();
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            View status = findViewById(R.id.settings_status);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) status.getLayoutParams();
            params.height = Utils.getActionBarHeight(this) + Utils.getStatusBarHeight(this);

            status.setLayoutParams(params);

            View nav = findViewById(R.id.settings_nav);
            params = (LinearLayout.LayoutParams) nav.getLayoutParams();
            params.height = Utils.hasNavBar(this) ? Utils.getNavBarHeight(this) : 0;

            nav.setLayoutParams(params);
        }
    }

    public void setUpDoneDiscard() {
        LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_done_discard, null);

        TextView doneButton = (TextView) customActionBarView.findViewById(R.id.done);
        if (AppSettings.isWhiteToolbar(this)) {
            doneButton.setTextColor(ColorStateList.valueOf(lightStatusBarIconColor));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                doneButton.setCompoundDrawableTintList(ColorStateList.valueOf(lightStatusBarIconColor));
            }
        }

        TextView discardButton = (TextView) customActionBarView.findViewById(R.id.discard);
        if (AppSettings.isWhiteToolbar(this)) {
            discardButton.setTextColor(ColorStateList.valueOf(lightStatusBarIconColor));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                discardButton.setCompoundDrawableTintList(ColorStateList.valueOf(lightStatusBarIconColor));
            }
        }

        doneButton.setText(getResources().getString(R.string.done_label));
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int currentAccount = sharedPrefs.getInt("current_account", 1);

                        SharedPreferences.Editor editor = sharedPrefs.edit();

                        for (int i = 0; i < chooserAdapter.getCount(); i++) {
                            if (chooserAdapter.getItem(i) instanceof ChooserFragment) {
                                ChooserFragment f = (ChooserFragment) chooserAdapter.getItem(i);

                                int num = i + 1;
                                editor.putInt("account_" + currentAccount + "_page_" + num, f.type);
                                editor.putLong("account_" + currentAccount + "_list_" + num + "_long", f.listId);
                                editor.putLong("account_" + currentAccount + "_user_tweets_" + num + "_long", f.userId);
                                editor.putString("account_" + currentAccount + "_name_" + num, f.name);
                                editor.putString("account_" + currentAccount + "_search_" + num, f.searchQuery);

                                if (f.check != null && f.check.isChecked()) {
                                    editor.putInt("default_timeline_page_" + currentAccount, i);
                                }
                            }
                        }

                        editor.apply();

                        onBackPressed();
                    }
                });
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });

        // Show the custom action bar view and hide the normal Home icon and question.
        final androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new androidx.appcompat.app.ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.configuration_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_select_number_of_pages:
                final NumberPicker picker = new NumberPicker(context);
                FrameLayout.LayoutParams params =
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                picker.setLayoutParams(params);
                picker.setMaxValue(TimelinePagerAdapter.MAX_EXTRA_PAGES);
                picker.setMinValue(0);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.menu_number_of_pages);
                builder.setView(picker);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPrefs.edit().putInt("number_of_extra_pages", picker.getValue()).apply();
                        dialog.dismiss();
                        recreate();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builder.create().show();

                return true;

            default:
                return true;
        }
    }

}
