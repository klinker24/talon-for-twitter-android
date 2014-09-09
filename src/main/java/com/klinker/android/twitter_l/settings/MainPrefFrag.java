package com.klinker.android.twitter_l.settings;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.klinker.android.twitter_l.R;

/**
 * Created by lucasklinker on 9/7/14.
 */
public class MainPrefFrag extends PreferenceFragment {

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
            "memory_management"
    };

    public void setClicks() {

        for (int i = 0; i < titles.length; i++) {
            final Preference p = findPreference(titles[i]);
            final int num = i;
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showSettings(num, preference.getTitle().toString());
                    return false;
                }
            });

            /*Drawable d = p.getIcon();
            if (d != null) {
                d.setColorFilter(AppSettings.getInstance(getActivity()).themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
                p.setIcon(d);
            }*/
        }
        /*findPreference("ui_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(0, preference.getTitle().toString());
                return false;
            }
        });

        findPreference("timeline_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(1, preference.getTitle().toString());
                return false;
            }
        });

        findPreference("sync_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(2, preference.getTitle().toString());
                return false;
            }
        });

        findPreference("notification_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(3, preference.getTitle().toString());
                return false;
            }
        });

        findPreference("browser_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(4, preference.getTitle().toString());
                return false;
            }
        });

        findPreference("advanced_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(5, preference.getTitle().toString());
                return false;
            }
        });

        findPreference("memory_management").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showSettings(6, preference.getTitle().toString());
                return false;
            }
        });*/
    }

    private void showSettings(int position, String title) {
        startActivity(new Intent(getActivity(), PrefActivity.class)
                .putExtra("position", position)
                .putExtra("title", title));

        getActivity().finish();
    }
}
