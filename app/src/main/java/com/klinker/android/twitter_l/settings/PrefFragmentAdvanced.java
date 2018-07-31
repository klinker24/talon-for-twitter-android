package com.klinker.android.twitter_l.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.services.background_refresh.ActivityRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.ListRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.MentionsRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.TimelineRefreshService;
import com.klinker.android.twitter_l.utils.Utils;

public class PrefFragmentAdvanced extends PrefFragment {

    @Override
    public void setPreferences(int position) {
        switch (position) {
            case 0: // advanced app style
                addPreferencesFromResource(R.xml.settings_advanced_app_style);
                setupAppStyle();
                break;
            case 1: // advanced widget customization
                break;
            case 2: // advanced swipable page and app drawer
                break;
            case 3: // advanced background refreshes
                addPreferencesFromResource(R.xml.settings_advanced_background_refreshes);
                setUpBackgroundRefreshes();
                break;
            case 4: // advanced notifications
                addPreferencesFromResource(R.xml.settings_advanced_notifications);
                setUpNotificationSettings();
                break;
            case 5: // data saving
                break;
            case 6: // location
                break;
            case 7: // mute management
                break;
            case 8: // app memory
                break;
            case 9: // other options
                break;
        }
    }

    @Override
    public void setupAppStyle() {

    }

    @Override
    public void setUpBackgroundRefreshes() {
        final Context context = getActivity();

        final AppSettings settings = AppSettings.getInstance(context);
        final SharedPreferences sharedPrefs = settings.sharedPrefs;

        int count = 0;
        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }
        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        final boolean mentionsChanges = count == 2;

        Preference sync = findPreference("sync_friends");
        sync.setOnPreferenceClickListener(arg0 -> {
            new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getString(R.string.sync_friends))
                    .setMessage(context.getResources().getString(R.string.sync_friends_summary))
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        try {
                            new SyncFriends(settings.myScreenName, sharedPrefs).execute();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton(R.string.no, (dialogInterface, i) -> {

                    })
                    .create()
                    .show();

            return false;
        });

        if(count != 2) {
            ((PreferenceGroup) findPreference("other_options")).removePreference(findPreference("sync_second_mentions"));
        }
    }

    @Override
    public void setUpNotificationSettings() {
        final Context context = getActivity();

        RingtonePreference ringtone = (RingtonePreference) findPreference("ringtone");
        ringtone.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AppSettings.getInstance(context).sharedPrefs.edit()
                        .putString("ringtone", newValue.toString())
                        .commit();
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                        .putString("ringtone", newValue.toString())
                        .commit();

                AppSettings.invalidate();

                return false;
            }
        });

        if (Utils.isAndroidO()) {
            getPreferenceScreen().removePreference(ringtone);
        }
    }
}
