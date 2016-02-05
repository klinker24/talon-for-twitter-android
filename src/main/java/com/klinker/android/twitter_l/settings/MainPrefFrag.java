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

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.XmlFaqUtils;

public class MainPrefFrag extends InAppBillingPreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.main_settings);

        setClicks();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        ListView list = (ListView) v.findViewById(android.R.id.list);
        list.setDivider(new ColorDrawable(getResources().getColor(android.R.color.transparent))); // or some other color int
        list.setDividerHeight(0);

        return v;
    }

    String[] titles = new String[] {
            "ui_settings",
            "timeline_settings",
            "sync_settings",
            "notification_settings",
            "browser_settings",
            "advanced_settings",
            "main_pages_and_drawer",
            "memory_management",
            "get_help",
            "become_supporter"
    };

    public void setClicks() {

        for (int i = 0; i < titles.length; i++) {
            final Preference p = findPreference(titles[i]);
            final int num = i;
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (titles[num].equals("become_supporter")) {
                        showSupporterDialog();
                    } else if (!titles[num].equals("get_help")) {
                        showSettings(num, preference.getTitle().toString());
                    } else {
                        showGetHelp();
                    }

                    return false;
                }
            });
        }
    }

    private void showSettings(int position, String title) {
        startActivity(new Intent(getActivity(), PrefActivity.class)
                .putExtra("position", position)
                .putExtra("title", title));

        getActivity().finish();
    }

    private void showGetHelp() {
        XmlFaqUtils.showFaqDialog(getActivity());
    }

    private void showSupporterDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Love Talon?")
                .setMessage("Talon has been available for almost a year and a half now, can you believe it? A LOT of work goes into this app every single day. I am just one person trying my best to keep up.\n\n" +
                        "If you are like me and use Talon every day, chances are that you have gotten your $4 out of the app. Consider becoming a 2016 SUPPORTER to help out development!\n\n" +
                        "I do want to be clear that becoming a supporter doesn't enhance the app or its feature set in any way. This is purely a voluntary contribution if you have enjoyed my work, like I know many of you have.\n\n" +
                        "Continue enjoying Talon either way, but I love when my users show me their support :)")
                .setPositiveButton("$10", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        start2016SupporterPurchase("10");
                    }
                })
                .setNeutralButton("$3", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        start2016SupporterPurchase("3");
                    }
                })
                .setNegativeButton("$6", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        start2016SupporterPurchase("6");
                    }
                }).create().show();
    }
}
