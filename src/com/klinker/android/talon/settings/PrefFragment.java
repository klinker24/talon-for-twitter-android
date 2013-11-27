package com.klinker.android.talon.settings;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.talon.R;
import com.klinker.android.talon.ui.ComposeActivity;
import com.klinker.android.talon.ui.widgets.HoloEditText;
import com.klinker.android.talon.ui.widgets.HoloTextView;
import com.klinker.android.talon.utils.IOUtils;

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

        Preference deviceFont = findPreference("device_font");
        deviceFont.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                HoloTextView.typeface = null;
                HoloEditText.typeface = null;
                return false;
            }
        });
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
                File des = new File(Environment.getExternalStorageDirectory() + "/Talon/backup.prefs");
                IOUtils.loadSharedPreferencesFromFile(des, context);

                Toast.makeText(context, context.getResources().getString(R.string.restore_success), Toast.LENGTH_LONG).show();

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

            cache.setSummary(getResources().getString(R.string.current_cache_size) + ": " + size / 1048576 + " MB");

            pDialog.dismiss();

            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.trim_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.trim_fail), Toast.LENGTH_SHORT).show();
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
            return IOUtils.trimDatabase(context);
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
        Preference gPlus = findPreference("google_plus");
        gPlus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.evolve_sms")));
                Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show();
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

                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"lklinker1@gmail.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Talon for Android");
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

    }
}