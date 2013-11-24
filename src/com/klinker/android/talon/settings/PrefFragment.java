package com.klinker.android.talon.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.talon.R;
import com.klinker.android.talon.utilities.IOUtils;

import java.io.File;

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
                addPreferencesFromResource(R.xml.advanced_settings);
                setUpAdvancedSettings();
                break;
            case 3:
                addPreferencesFromResource(R.xml.get_help_settings);
                setUpGetHelpSettings();
                break;
            case 4:
                addPreferencesFromResource(R.xml.other_apps_settings);
                setUpOtherAppSettings();
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

    public void setUpAdvancedSettings() {
        final Context context = getActivity();
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference backup = findPreference("backup");
        backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.backup_settings_dialog))
                        .setMessage(context.getResources().getString(R.string.backup_settings_dialog_summary))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                File des = new File(Environment.getExternalStorageDirectory() + "/Talon/backup.prefs");
                                IOUtils.saveSharedPreferencesToFile(des, context);

                                Toast.makeText(context, context.getResources().getString(R.string.backup_success), Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .create()
                        .show();

                return false;
            }

        });

        Preference restore = findPreference("restore");
        restore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                File des = new File(Environment.getExternalStorageDirectory() + "/EvolveSMS/backup.prefs");
                IOUtils.loadSharedPreferencesFromFile(des, context);

                Toast.makeText(context, context.getResources().getString(R.string.restore_success), Toast.LENGTH_LONG).show();

                return false;
            }

        });

    }

    public void setUpGetHelpSettings() {

        final Context context = getActivity();


    }

    public void setUpOtherAppSettings() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


    }
}