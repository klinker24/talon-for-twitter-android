package com.klinker.android.twitter.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.Item;
import com.klinker.android.twitter.services.DirectMessageRefreshService;
import com.klinker.android.twitter.services.MentionsRefreshService;
import com.klinker.android.twitter.services.TimelineRefreshService;
import com.klinker.android.twitter.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter.ui.ComposeActivity;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.UserProfileActivity;
import com.klinker.android.twitter.ui.fragments.DMFragment;
import com.klinker.android.twitter.ui.fragments.HomeFragment;
import com.klinker.android.twitter.ui.fragments.MentionsFragment;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.EmojiUtils;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.Utils;

import java.io.File;
import java.util.Date;
import java.util.List;

import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.User;

public class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

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
                setUpSyncSettings();
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

        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference deviceFont = findPreference("device_font");
        deviceFont.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                HoloTextView.typeface = null;
                HoloEditText.typeface = null;
                return false;
            }
        });

        final Preference nightMode = findPreference("night_mode");
        nightMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!((CheckBoxPreference) nightMode).isChecked()) {
                    com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                            sharedPrefs.edit().putInt("night_start_hour", hourOfDay).putInt("night_start_min", minute).commit();

                            com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                                    sharedPrefs.edit().putInt("day_start_hour", hourOfDay).putInt("day_start_min", minute).commit();

                                    new AlertDialog.Builder(getActivity())
                                            .setTitle(R.string.night_mode_theme)
                                            .setItems(getResources().getStringArray(R.array.choose_theme), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    sharedPrefs.edit().putInt("night_theme", i).commit();
                                                }
                                            })
                                            .show();
                                }
                            }, 6, 0, sharedPrefs.getBoolean("military_time", false), getString(R.string.night_mode_day));
                            dialog.show(getFragmentManager(), "night_mode_day");
                        }
                    }, 22, 0, sharedPrefs.getBoolean("military_time", false), getString(R.string.night_mode_night));
                    dialog.setThemeDark(true);
                    dialog.show(getFragmentManager(), "night_mode_night");
                }

                return true;
            }

        });

        final Preference emojis = findPreference("use_emojis");
        emojis.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!((CheckBoxPreference) emojis).isChecked() && !EmojiUtils.checkEmojisEnabled(context)) {
                    new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.enable_emojis) + ":")
                            .setMessage(context.getResources().getString(R.string.emoji_dialog_summary))
                            .setPositiveButton(R.string.get_android, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard_trial")));
                                }
                            })
                            .setNegativeButton(R.string.get_ios, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard_trial_ios")));
                                }
                            })
                            .create()
                            .show();
                }

                return true;
            }

        });

        final Preference addonTheme = findPreference("addon_themes");

        String pack = sharedPrefs.getString("addon_theme_package", null);
        if (pack != null) {
            try {
                addonTheme.setSummary(context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pack, 0)));
            } catch (Exception e) {
                sharedPrefs.edit().putBoolean("addon_theme", false).putString("addon_theme_package", null).commit();
            }
        }

        addonTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (sharedPrefs.getBoolean("addon_themes", false)) {
                    sharedPrefs.edit().putBoolean("addon_themes", false).commit();
                    sharedPrefs.edit().putString("addon_theme_package", null).commit();
                    addonTheme.setSummary(sharedPrefs.getString("addon_theme_package", null));
                    return true;
                }

                final PackageManager pm = context.getPackageManager();
                final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

                for (int i = 0; i < packages.size(); i++) {
                    Bundle metaData = packages.get(i).metaData;
                    if (metaData == null) {
                        packages.remove(i--);
                        continue;
                    }

                    try {
                        boolean theme = metaData.getString("evolve_theme").startsWith("version");
                        if (!theme) {
                            packages.remove(i--);
                        }
                    } catch (Exception e) {
                        packages.remove(i--);
                    }
                }

                final Item[] items = new Item[packages.size()];

                for (int i = 0; i < items.length; i++) {
                    items[i] = new Item(packages.get(i).loadLabel(pm).toString(), pm.getApplicationIcon(packages.get(i)));
                }

                ListAdapter adapter = new ArrayAdapter<Item>(
                        context,
                        android.R.layout.select_dialog_item,
                        android.R.id.text1,
                        items) {
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView tv = (TextView) v.findViewById(android.R.id.text1);
                        tv.setCompoundDrawablesWithIntrinsicBounds(items[position].actualIcon, null, null, null);
                        tv.setCompoundDrawablePadding((int) (5 * getResources().getDisplayMetrics().density + 0.5f));
                        tv.setText(items[position].text);
                        return v;
                    }
                };

                AlertDialog.Builder attachBuilder = new AlertDialog.Builder(context);
                attachBuilder.setTitle(R.string.addon_themes);
                attachBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sharedPrefs.edit().putString("addon_theme_package", packages.get(arg1).packageName).commit();
                        try {
                            String pack = packages.get(arg1).packageName;
                            addonTheme.setSummary(context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pack, 0)));
                        } catch (Exception e) {
                            sharedPrefs.edit().putBoolean("addon_theme", false).putString("addon_theme_package", null).commit();
                        }
                    }

                });

                attachBuilder.create().show();
                return true;
            }
        });
    }

    public void setUpSyncSettings() {
        final Context context = getActivity();

        final AppSettings settings = new AppSettings(context);
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

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

        // remove the mobile data one if they have a tablet
        /*if (context.getResources().getBoolean(R.bool.isTablet)) {
            getPreferenceScreen().removePreference(getPreferenceManager().findPreference("sync_mobile_data"));
        }*/
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
                File des = new File(Environment.getExternalStorageDirectory() + "/Talon/backup.prefs");

                String authenticationToken1 = sharedPrefs.getString("authentication_token_1", "none");
                String authenticationTokenSecret1 = sharedPrefs.getString("authentication_token_secret_1", "none");
                String myScreenName1 = sharedPrefs.getString("twitter_screen_name_1", "");
                String myName1 = sharedPrefs.getString("twitter_users_name_1", "");
                String myBackgroundUrl1 = sharedPrefs.getString("twitter_background_url_1", "");
                String myProfilePicUrl1 = sharedPrefs.getString("profile_pic_url_1", "");
                String favoriteUserNames1 = sharedPrefs.getString("favorite_user_names_1", "");
                long lastTweetId1 = sharedPrefs.getLong("last_tweet_id_1", 0);
                long secondLastTweetId1 = sharedPrefs.getLong("second_last_tweet_id_1", 0);
                long lastMentionId1 = sharedPrefs.getLong("last_mention_id_1", 0);
                long lastDMId1 = sharedPrefs.getLong("last_dm_id_1", 0);

                String authenticationToken2 = sharedPrefs.getString("authentication_token_2", "none");
                String authenticationTokenSecret2 = sharedPrefs.getString("authentication_token_secret_2", "none");
                String myScreenName2 = sharedPrefs.getString("twitter_screen_name_2", "");
                String myName2 = sharedPrefs.getString("twitter_users_name_2", "");
                String myBackgroundUrl2 = sharedPrefs.getString("twitter_background_url_2", "");
                String myProfilePicUrl2 = sharedPrefs.getString("profile_pic_url_2", "");
                String favoriteUserNames2 = sharedPrefs.getString("favorite_user_names_2", "");
                long lastTweetId2 = sharedPrefs.getLong("last_tweet_id_2", 0);
                long secondLastTweetId2 = sharedPrefs.getLong("second_last_tweet_id_2", 0);
                long lastMentionId2 = sharedPrefs.getLong("last_mention_id_2", 0);
                long lastDMId2 = sharedPrefs.getLong("last_dm_id_2", 0);

                IOUtils.loadSharedPreferencesFromFile(des, context);

                Toast.makeText(context, context.getResources().getString(R.string.restore_success), Toast.LENGTH_LONG).show();

                SharedPreferences.Editor e = sharedPrefs.edit();

                e.putString("authentication_token_1", authenticationToken1);
                e.putString("authentication_token_secret_1", authenticationTokenSecret1);
                e.putString("twitter_screen_name_1", myScreenName1);
                e.putString("twitter_users_name_1", myName1);
                e.putString("twitter_background_url_1", myBackgroundUrl1);
                e.putString("profile_pic_url_1", myProfilePicUrl1);
                e.putString("favorite_user_names_1", "");
                e.putLong("last_tweet_id_1", lastTweetId1);
                e.putLong("second_last_tweet_id_1", secondLastTweetId1);
                e.putLong("last_mention_id_1", lastMentionId1);
                e.putLong("last_dm_id_1", lastDMId1);

                e.putString("authentication_token_2", authenticationToken2);
                e.putString("authentication_token_secret_2", authenticationTokenSecret2);
                e.putString("twitter_screen_name_2", myScreenName2);
                e.putString("twitter_users_name_2", myName2);
                e.putString("twitter_background_url_2", myBackgroundUrl2);
                e.putString("profile_pic_url_2", myProfilePicUrl2);
                e.putString("favorite_user_names_2", "");
                e.putLong("last_tweet_id_2", lastTweetId2);
                e.putLong("second_last_tweet_id_2", secondLastTweetId2);
                e.putLong("last_mention_id_2", lastMentionId2);
                e.putLong("last_dm_id_2", lastDMId2);

                e.commit();

                return false;
            }

        });

        final Preference cache = findPreference("delete_cache");
        long size = IOUtils.dirSize(context.getCacheDir());
        cache.setSummary(getResources().getString(R.string.current_cache_size) + ": " + size / 1048576 + " MB");
        cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.cache_dialog))
                        .setMessage(context.getResources().getString(R.string.cache_dialog_summary))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    new TrimCache(cache).execute();
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

        Preference trim = findPreference("trim_now");
        trim.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.trim_dialog))
                        .setMessage(context.getResources().getString(R.string.cache_dialog_summary))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    new TrimDatabase().execute();
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

    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPrefs, String key) {

        Log.v("alarm_date", "key: " + key);

        if (key.equals("timeline_sync_interval")) {

            long refreshRate = Long.parseLong(sharedPrefs.getString("timeline_sync_interval", "1800000"));

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + refreshRate;

            Log.v("alarm_date", "timeline " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, HomeFragment.HOME_REFRESH_ID, new Intent(context, TimelineRefreshService.class), 0);

            if (refreshRate != 0)
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, refreshRate, pendingIntent);
            else
                am.cancel(pendingIntent);
        } else if (key.equals("mentions_sync_interval")) {

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long refreshRate = Long.parseLong(sharedPrefs.getString("mentions_sync_interval", "1800000"));

            long now = new Date().getTime();
            long alarm = now + refreshRate;

            Log.v("alarm_date", "mentions " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, MentionsFragment.MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);

            if (refreshRate != 0)
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, refreshRate, pendingIntent);
            else
                am.cancel(pendingIntent);
        } else if (key.equals("dm_sync_interval")) {

            long refreshRate = Long.parseLong(sharedPrefs.getString("dm_sync_interval", "1800000"));

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + refreshRate;

            Log.v("alarm_date", "direct message " + new Date(alarm).toString());

            PendingIntent pendingIntent = PendingIntent.getService(context, DMFragment.DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);

            if (refreshRate != 0)
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, refreshRate, pendingIntent);
            else
                am.cancel(pendingIntent);
        } else if (key.equals("layout")) {
            new TrimCache(null).execute();
        }
    }

    class TrimCache extends AsyncTask<String, Void, Boolean> {

        private Preference cache;
        private ProgressDialog pDialog;

        public TrimCache(Preference cache) {
            this.cache = cache;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.trimming));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected Boolean doInBackground(String... urls) {
            IOUtils.trimCache(context);
            return true;
        }

        protected void onPostExecute(Boolean deleted) {

            long size = IOUtils.dirSize(context.getCacheDir());

            boolean fin = false;

            if (cache != null) {
                cache.setSummary(getResources().getString(R.string.current_cache_size) + ": " + size / 1048576 + " MB");
                if (deleted) {
                    Toast.makeText(context, context.getResources().getString(R.string.trim_success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getResources().getString(R.string.trim_fail), Toast.LENGTH_SHORT).show();
                }
            } else {
                fin = true;
            }

            pDialog.dismiss();

            if (fin) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.themeing_complete))
                        .setMessage(context.getResources().getString(R.string.themeing_complete_summary))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                ((Activity)context).finish();
                            }
                        })
                        .create()
                        .show();
            }


        }
    }

    class SyncFriends extends AsyncTask<String, Void, Boolean> {

        private ProgressDialog pDialog;
        private String screenName;
        private SharedPreferences sharedPrefs;

        public SyncFriends(String name, SharedPreferences sharedPreferences) {
            this.screenName = name;
            this.sharedPrefs = sharedPreferences;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.syncing_user));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected Boolean doInBackground(String... urls) {
            FollowersDataSource followers = new FollowersDataSource(context);
            followers.open();

            followers.deleteAllUsers(sharedPrefs.getInt("current_account", 1));

            try {

                Twitter twitter = Utils.getTwitter(context);

                int currentAccount = sharedPrefs.getInt("current_account", 1);
                PagableResponseList<User> friendsPaging = twitter.getFriendsList(screenName, -1);

                for (User friend : friendsPaging) {
                    followers.createUser(friend, currentAccount);
                }

                long nextCursor = friendsPaging.getNextCursor();

                while (nextCursor != -1) {
                    friendsPaging = twitter.getFriendsList(screenName, nextCursor);

                    for (User friend : friendsPaging) {
                        followers.createUser(friend, currentAccount);
                    }

                    nextCursor = friendsPaging.getNextCursor();
                }

            } catch (Exception e) {
                // something wrong haha
            }

            followers.close();
            return true;
        }

        protected void onPostExecute(Boolean deleted) {

            pDialog.dismiss();

            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.sync_failed), Toast.LENGTH_SHORT).show();
            }


        }
    }

    class TrimDatabase extends AsyncTask<String, Void, Boolean> {

        ProgressDialog pDialog;

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.trimming));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        protected Boolean doInBackground(String... urls) {
            return IOUtils.trimDatabase(context, 1) && IOUtils.trimDatabase(context, 2);
        }

        protected void onPostExecute(Boolean deleted) {
            pDialog.dismiss();
            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.trim_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.trim_fail), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setUpGetHelpSettings() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Preference tutorial = findPreference("tutorial");
        tutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent tutorial = new Intent(context, MainActivity.class);
                tutorial.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                tutorial.putExtra("tutorial", true);
                sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                sharedPrefs.edit().putBoolean("done_tutorial", false).commit();
                startActivity(tutorial);
                return false;
            }
        });

        Preference youtube = findPreference("youtube");
        youtube.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=-wEgkt7OXTY")));
                //Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference gPlus = findPreference("google_plus");
        gPlus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://goo.gl/KCXlZk")));
                //Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference xda = findPreference("xda_thread");
        xda.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.evolve_sms")));
                Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Preference email = findPreference("email_me");
        email.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"klinkerapps@gmail.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Talon for Twitter");
                emailIntent.setType("plain/text");

                startActivity(emailIntent);
                return false;
            }
        });

        Preference tweet = findPreference("tweet_me");
        tweet.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent tweet = new Intent(getActivity(), ComposeActivity.class);
                tweet.putExtra("user", "@lukeklinker");
                startActivity(tweet);
                return false;
            }
        });

        Preference followTalon = findPreference("follow_talon");
        followTalon.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent profile = new Intent(getActivity(), UserProfileActivity.class);
                profile.putExtra("screenname", "TalonAndroid");
                profile.putExtra("proPic", "");
                startActivity(profile);
                return false;
            }
        });

        Preference credits = findPreference("credits");
        credits.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String license = IOUtils.readAsset(context, "license.txt");
                ScrollView scrollView = new ScrollView(context);
                TextView changeView = new TextView(context);
                changeView.setText(Html.fromHtml(license));
                int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics());
                changeView.setPadding(padding, padding, padding, padding);
                changeView.setTextSize(12);
                scrollView.addView(changeView);

                new AlertDialog.Builder(context)
                        .setTitle(R.string.credits)
                        .setView(scrollView)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            }
        });

    }

    public void setUpOtherAppSettings() {

        Preference evolve = findPreference("evolvesms");
        evolve.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.evolve_sms")));
                return false;
            }
        });

        Preference sm = findPreference("sliding_messaging");
        sm.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.messaging_donate")));
                return false;
            }
        });

        Preference smTheme = findPreference("theme_engine");
        smTheme.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.messaging_theme")));
                return false;
            }
        });

        Preference keyboard = findPreference("emoji_keyboard");
        keyboard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard_trial")));
                return false;
            }
        });

        Preference keyboardios = findPreference("emoji_keyboard_ios");
        keyboardios.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard_trial_ios")));
                return false;
            }
        });

        Preference keyboardUnlock = findPreference("emoji_keyboard_unlock");
        keyboardUnlock.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard")));
                return false;
            }
        });

        Preference halopop = findPreference("halopop");
        halopop.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.halopop")));
                return false;
            }
        });

        Preference floatingwindows = findPreference("floating_windows");
        floatingwindows.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.floating_window")));
                return false;
            }
        });

        Preference slideover = findPreference("slideover_messaging");
        slideover.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.lklinker.android.slideovermessaging")));
                return false;
            }
        });

    }
}