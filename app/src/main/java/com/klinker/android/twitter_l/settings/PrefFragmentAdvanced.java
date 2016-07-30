package com.klinker.android.twitter_l.settings;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.services.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.TimelineRefreshService;
import com.klinker.android.twitter_l.activities.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.MentionsFragment;

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
            case 3: // in app browser
                break;
            case 4: // advanced background refreshes
                addPreferencesFromResource(R.xml.settings_advanced_background_refreshes);
                setUpBackgroundRefreshes();
                break;
            case 5: // advanced notifications
                addPreferencesFromResource(R.xml.settings_advanced_notifications);
                setUpNotificationSettings();
                break;
            case 6: // data saving
                break;
            case 7: // location
                break;
            case 8: // mute management
                break;
            case 9: // app memory
                break;
            case 10: // other options
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

        final Preference fillGaps = findPreference("fill_gaps");
        fillGaps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new FillGaps().execute();
                return false;
            }
        });

        final Preference noti = findPreference("show_pull_notification");
        noti.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                return true;
            }
        });

        final Preference stream = findPreference("talon_pull");
        stream.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));

                if (o.equals("2")) {
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    PendingIntent pendingIntent1 = PendingIntent.getService(context, HomeFragment.HOME_REFRESH_ID, new Intent(context, TimelineRefreshService.class), 0);
                    PendingIntent pendingIntent2 = PendingIntent.getService(context, MentionsFragment.MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);
                    PendingIntent pendingIntent3 = PendingIntent.getService(context, DMFragment.DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);

                    am.cancel(pendingIntent1);
                    am.cancel(pendingIntent2);
                    am.cancel(pendingIntent3);

                    SharedPreferences.Editor e = sharedPrefs.edit();
                    if (sharedPrefs.getBoolean("live_streaming", true)) {
                        e.putString("timeline_sync_interval", "0");
                    }
                    e.putString("mentions_sync_interval", "0");
                    e.putString("dm_sync_interval", "0");
                    e.apply();
                }

                return true;
            }
        });

        Preference sync = findPreference("sync_friends");
        sync.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.sync_friends))
                        .setMessage(context.getResources().getString(R.string.sync_friends_summary))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    new SyncFriends(settings.myScreenName, sharedPrefs).execute();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
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

        if(count != 2) {
            ((PreferenceGroup) findPreference("other_options")).removePreference(findPreference("sync_second_mentions"));
        }
    }

    @Override
    public void setUpNotificationSettings() {
        final Context context = getActivity();

        Preference interactionsSet = findPreference("interactions_set");
        interactionsSet.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                return true;
            }
        });

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
    }
}
