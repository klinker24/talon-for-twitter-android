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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import androidx.appcompat.app.AlertDialog;

import android.preference.PreferenceFragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ChangelogAdapter;
import com.klinker.android.twitter_l.utils.XmlChangelogUtils;
import com.klinker.android.twitter_l.utils.XmlFaqUtils;

public class MainPrefFrag extends PreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.main_settings);

        setClicks();

        if (AppSettings.getInstance(getActivity()).blackTheme) {
            getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        if(v != null) {
            ListView lv = (ListView) v.findViewById(android.R.id.list);
            lv.setPadding(0,0,0,0);
        }

        return v;
    }

    String[] titles = new String[] {
            "app_style",
            "widget_customization",
            "swipable_pages_and_app_drawer",
            "background_refreshes",
            "notifications",
            "data_saving_options",
            "location",
            "mute_management",
            "app_memory",
            "other_options",
            "faq",
            "whats_new",
    };

    public void setClicks() {

        for (int i = 0; i < titles.length; i++) {
            final Preference p = findPreference(titles[i]);

            final int num = i;
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                    if (titles[num].equals("faq")) {
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

}
