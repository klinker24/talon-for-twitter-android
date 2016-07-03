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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ChangelogAdapter;
import com.klinker.android.twitter_l.manipulations.ListTagHandler;
import com.klinker.android.twitter_l.utils.UpdateUtils;
import com.klinker.android.twitter_l.utils.XmlChangelogUtils;
import com.klinker.android.twitter_l.utils.XmlFaqUtils;

public class MainPrefFrag extends InAppBillingPreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.main_settings);

        setClicks();

        if (UpdateUtils.showSupporterDialog(getActivity())) {
            showSupporterDialog();
        }
    }

    String[] titles = new String[] {
            "app_style",
            "widget_customization",
            "swipable_pages_and_app_drawer",
            "in_app_browser",
            "background_refreshes",
            "notifications",
            "data_saving_options",
            "location",
            "mute_management",
            "app_memory",
            "other_options",
            "become_supporter",
            "faq",
            "whats_new",
    };

    public boolean mListStyled;

    @Override
    public void onResume() {
        super.onResume();
        if (!mListStyled) {
            View rootView = getView();
            if (rootView != null) {
                ListView list = (ListView) rootView.findViewById(android.R.id.list);
                list.setPadding(0, 0, 0, 0);
                list.setDivider(null);
                //any other styling call
                mListStyled = true;
            }
        }
    }

    public void setClicks() {

        for (int i = 0; i < titles.length; i++) {
            final Preference p = findPreference(titles[i]);

            final int num = i;
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                    if (titles[num].equals("become_supporter")) {
                        showSupporterDialog();
                    } else if (titles[num].equals("faq")) {
                        showGetHelp();
                    } else if (titles[num].equals("whats_new")) {
                        showWhatsNew();
                    } else {
                        showSettings(num, preference.getTitle().toString());
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
    }

    private void showWhatsNew() {
        final Context context = getActivity();
        final ListView list = new ListView(context);
        list.setDividerHeight(0);

        new AsyncTask<Spanned[], Void, Spanned[]>() {
            @Override
            public Spanned[] doInBackground(Spanned[]... params) {
                return XmlChangelogUtils.parse(context);
            }

            @Override
            public void onPostExecute(Spanned[] result) {
                list.setAdapter(new ChangelogAdapter(context, result));
            }
        }.execute();

        new android.app.AlertDialog.Builder(context)
                .setTitle(R.string.changelog)
                .setView(list)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showGetHelp() {
        XmlFaqUtils.showFaqDialog(getActivity());
    }

    private void showSupporterDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle("Love Talon?")
                .setMessage(Html.fromHtml("Talon has been available for almost a year and a half now, can you believe it? A LOT of work goes into this app every single day. I am just one person trying my best to keep up.<br><br>" +
                        "If you are like me and use Talon every day, chances are that you have gotten your $4 out of the app. Consider becoming a 2016 SUPPORTER to help out development!<br><br>" +
                        "I do want to be clear that becoming a supporter doesn't enhance the app or its feature set in any way. This is purely a voluntary contribution if you have enjoyed my work, like I know many of you have.<br><br>" +
                        "<b>So, why should I become a Supporter?</b>" +
                        "<ul>" +
                        "<li>You have been an every day user of Talon for awhile and you feel like you have gotten more than a cup of coffee's worth out of the app.</li>" +
                        "<li>You understand that development is hard. I put all my free time into this product, and I think it shows! I hope you agree.</li>" +
                        "<li>You want the warm-fuzzy feeling that comes with giving a little extra for something that you enjoy and use every day.</li><br>" +
                        "</ul><br>" +
                        "Even the $10 Supporter option is less than $1 per month for 2016. I am willing to bet that everyone throws away much more than that into products they use 10x less than Talon!<br><br><br>" +
                        "Continue enjoying Talon either way, but I love when my users show me their support :)", null, new ListTagHandler()))
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
