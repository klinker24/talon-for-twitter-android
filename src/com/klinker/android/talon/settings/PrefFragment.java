package com.klinker.android.talon.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.ListView;

import com.klinker.android.talon.R;

import java.util.ArrayList;
import java.util.Arrays;

public class PrefFragment extends PreferenceFragment {

    private Context context;

    public int position;
    public String[] linkItems;
    public ListView mDrawerList;

    public PrefFragment(ListView drawerList, Context context) {
        mDrawerList = drawerList;

        linkItems = new String[]{context.getResources().getString(R.string.theme_settings),
                context.getResources().getString(R.string.sync_settings),
                context.getResources().getString(R.string.notification_settings),
                context.getResources().getString(R.string.advanced_settings),
                context.getResources().getString(R.string.get_help_settings),
                context.getResources().getString(R.string.other_apps),
                context.getResources().getString(R.string.whats_new),
                context.getResources().getString(R.string.rate_it)};

        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        position = args.getInt("position");

        DrawerArrayAdapter.current = position - 1;
        //mDrawerList.setAdapter(new DrawerArrayAdapter(getActivity(),
                //new ArrayList<String>(Arrays.asList(linkItems))));

        switch (position) {
            case 0:
                addPreferencesFromResource(R.xml.theme_settings);
                setUpThemeSettings();
                break;
            case 1:
                addPreferencesFromResource(R.xml.sync_settings);
                setUpNotificationSettings();
                break;
            case 2:
                addPreferencesFromResource(R.xml.notification_settings);
                setUpPopupSettings();
                break;
            case 3:
                addPreferencesFromResource(R.xml.advanced_settings);
                setUpSlideOverSettings();
                break;
            case 4:
                addPreferencesFromResource(R.xml.get_help_settings);
                setUpMessageSettings();
                break;
            case 5:
                addPreferencesFromResource(R.xml.other_apps_settings);
                setUpConversationSettings();
                break;
        }
    }

    public void setUpThemeSettings() {
        final Context context = getActivity();
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


    }

    public void setUpNotificationSettings() {
        final Context context = getActivity();

    }

    public void setUpPopupSettings() {
        final Context context = getActivity();
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

    }

    public void setUpSlideOverSettings() {

        final Context context = getActivity();


    }

    public void setUpMessageSettings() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


    }

    public void setUpConversationSettings() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


    }
}