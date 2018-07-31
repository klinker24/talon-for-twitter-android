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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.SearchRecentSuggestions;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.AccentPickerAdapter;
import com.klinker.android.twitter_l.adapters.ChangelogAdapter;
import com.klinker.android.twitter_l.adapters.ColorPickerAdapter;
import com.klinker.android.twitter_l.adapters.MainDrawerArrayAdapter;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.ThemeColor;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.utils.ServiceUtils;
import com.klinker.android.twitter_l.utils.text.EmojiInitializer;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.utils.LocalTrendsUtils;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.settings.configure_pages.ConfigurePagerActivity;
import com.klinker.android.twitter_l.activities.compose.ComposeActivity;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefEditText;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.XmlChangelogUtils;
import com.klinker.android.twitter_l.utils.XmlFaqUtils;
import com.klinker.android.twitter_l.widget.WidgetProvider;

import java.io.File;
import java.util.*;

import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class PrefFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context;

    public int position;

    public boolean mListStyled;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity();

        Bundle args = getArguments();
        position = args.getInt("position");

        setPreferences(position);
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

    public void setPreferences(final int position) {
        switch (position) {
            case 0: // app style
                addPreferencesFromResource(R.xml.settings_app_style);
                setupAppStyle();
                break;
            case 1: // widget customization
                addPreferencesFromResource(R.xml.settings_widget_customization);
                setUpWidgetCustomization();
                break;
            case 2: // swipable page and app drawer
                addPreferencesFromResource(R.xml.settings_swipable_pages_and_app_drawer);
                setUpSwipablePages();
                break;
            /*case 3: // in app browser
                addPreferencesFromResource(R.xml.settings_browser);
                setUpBrowser();
                break;*/
            case 3: // background refreshes
                addPreferencesFromResource(R.xml.settings_background_refreshes);
                setUpBackgroundRefreshes();
                break;
            case 4: // notifications
                addPreferencesFromResource(R.xml.settings_notifications);
                setUpNotificationSettings();
                break;
            case 5: // data saving
                addPreferencesFromResource(R.xml.settings_data_savings);
                setUpDataSaving();
                break;
            case 6: // location
                addPreferencesFromResource(R.xml.settings_location);
                setUpLocationSettings();
                break;
            case 7: // mute management
                addPreferencesFromResource(R.xml.settings_mutes);
                setUpMuteSettings();
                break;
            case 8: // app memory
                addPreferencesFromResource(R.xml.settings_app_memory);
                setUpAppMemorySettings();
                break;
            case 9: // other options
                addPreferencesFromResource(R.xml.settings_other_options);
                setUpOtherOptions();
                break;
            case 10: // get help (from overflow)
                addPreferencesFromResource(R.xml.settings_get_help);
                setUpGetHelpSettings();
                break;
            case 11: // other apps (from overflow)
                addPreferencesFromResource(R.xml.settings_other_apps);
                setUpOtherAppSettings();
                break;
        }

        Preference advanced = findPreference("advanced");
        if (advanced != null) {
            advanced.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent advanced = new Intent(context, PrefActivityAdvanced.class);
                    advanced.putExtra("position", position);

                    startActivity(advanced);

                    return true;
                }
            });
        }

    }

    public void setUpWidgetCustomization() {
        final AppSettings settings = AppSettings.getInstance(getActivity());

        final Preference account = findPreference("account");
        account.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final CharSequence[] items = settings.numberOfAccounts == 1 ?
                        new CharSequence[] { "@" + settings.myScreenName } :
                        new CharSequence[] { "@" + settings.myScreenName, "@" + settings.secondScreenName };

                new AlertDialog.Builder(getActivity())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settings.sharedPrefs.edit().putString("widget_account", items[which] + "").apply();
                                account.setSummary(items[which]);
                            }
                        })
                        .create().show();
                return true;
            }
        });

        account.setSummary(settings.sharedPrefs.getString("widget_account", ""));
    }

    public void setUpDataSaving() {

    }

    public void setUpBrowser() {
        final SharedPreferences sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(context);
        Preference customTabs = findPreference("chrome_custom_tabs");
        customTabs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.custom_tab_title)
                        .setMessage(R.string.custom_tab_message)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sharedPreferences.edit().putBoolean("is_chrome_default", true).apply();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sharedPreferences.edit().putBoolean("is_chrome_default", false).apply();
                            }
                        })
                        .setNeutralButton(R.string.learn_more, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("http://android-developers.blogspot.com/2015/09/chrome-custom-tabs-smooth-transition.html"));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                            }
                        })
                        .create().show();
                return false;
            }
        });
    }

    public void setUpSwipablePages() {
        Preference pages = findPreference("pages");
        pages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent configurePages = new Intent(context, ConfigurePagerActivity.class);
                startActivity(configurePages);
                return false;
            }
        });

        Preference drawerItems = findPreference("drawer_elements");
        drawerItems.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

                List<Integer> pageTypes = new ArrayList<Integer>();
                List<String> pageNames = new ArrayList<String>();
                List<String> searches = new ArrayList<String>();
                List<String> text = new ArrayList<String>();

                final int currentAccount = sharedPrefs.getInt("current_account", 1);

                for (int i = 0; i < TimelinePagerAdapter.MAX_EXTRA_PAGES; i++) {
                    String pageIdentifier = "account_" + currentAccount + "_page_" + (i + 1);
                    String nameIdentifier = "account_" + currentAccount + "_name_" + (i + 1);
                    String searchIdentifier = "account_" + currentAccount + "_search_" + (i + 1);

                    int type = sharedPrefs.getInt(pageIdentifier, AppSettings.PAGE_TYPE_NONE);

                    if (type != AppSettings.PAGE_TYPE_NONE) {
                        pageTypes.add(type);
                        pageNames.add(sharedPrefs.getString(nameIdentifier, ""));
                        searches.add(sharedPrefs.getString(searchIdentifier, ""));
                    }
                }

                for (int i = 0; i < pageTypes.size(); i++) {
                    switch (pageTypes.get(i)) {
                        case AppSettings.PAGE_TYPE_HOME:
                            text.add(context.getResources().getString(R.string.timeline));
                            break;
                        case AppSettings.PAGE_TYPE_MENTIONS:
                            text.add(context.getResources().getString(R.string.mentions));
                            break;
                        case AppSettings.PAGE_TYPE_DMS:
                            text.add(context.getResources().getString(R.string.direct_messages));
                            break;
                        case AppSettings.PAGE_TYPE_SECOND_MENTIONS:
                            text.add("@" + AppSettings.getInstance(context).secondScreenName);
                            break;
                        case AppSettings.PAGE_TYPE_WORLD_TRENDS:
                            text.add(getString(R.string.world_trends));
                            break;
                        case AppSettings.PAGE_TYPE_LOCAL_TRENDS:
                            text.add(getString(R.string.local_trends));
                            break;
                        case AppSettings.PAGE_TYPE_SAVED_SEARCH:
                            text.add(searches.get(i));
                            break;
                        case AppSettings.PAGE_TYPE_ACTIVITY:
                            text.add(getString(R.string.activity));
                            break;
                        case AppSettings.PAGE_TYPE_FAVORITE_STATUS:
                            text.add(getString(R.string.favorite_tweets));
                            break;
                        default:
                            text.add(getName(pageNames.get(i), pageTypes.get(i)));
                            break;
                    }
                }

                for (String s : MainDrawerArrayAdapter.getItems(context)) {
                    text.add(s);
                }

                String[] strings = new String[text.size()];
                boolean[] bools = new boolean[text.size()];

                final Set<String> set = sharedPrefs.getStringSet("drawer_elements_shown_" + currentAccount, new HashSet<String>());

                for (int i = 0; i < strings.length; i++) {
                    strings[i] = text.get(i);
                    if (set.contains(i + "")) {
                        bools[i] = true;
                    } else {
                        bools[i] = false;
                    }
                }

                new AlertDialog.Builder(context)
                        .setTitle(preference.getTitle())
                        .setMultiChoiceItems(strings, bools, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (!isChecked) {
                                    // remove it
                                    set.remove(which + "");
                                } else {
                                    set.add("" + which);
                                }
                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sharedPrefs.edit().putStringSet("drawer_elements_shown_" + currentAccount, set).apply();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                return false;
            }
        });
    }

    public String getName(String listName, int type) {
        switch (type) {
            case AppSettings.PAGE_TYPE_USER_TWEETS:
            case AppSettings.PAGE_TYPE_LIST:
                return listName;
            case AppSettings.PAGE_TYPE_LINKS:
                return context.getResources().getString(R.string.links);
            case AppSettings.PAGE_TYPE_PICS:
                return context.getResources().getString(R.string.pictures);
            case AppSettings.PAGE_TYPE_FAV_USERS:
                return context.getString(R.string.favorite_users);
            case AppSettings.PAGE_TYPE_SAVED_TWEETS:
                return context.getString(R.string.saved_tweets);
        }

        return null;
    }

    public void setUpAppMemorySettings() {
        final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        Preference clearSearch = findPreference("clear_searches");
        clearSearch.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                        MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);
                suggestions.clearHistory();
                return false;
            }
        });

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

                                Toast.makeText(context, context.getResources().getString(R.string.backup_success) + ": /Talon/backup.prefs", Toast.LENGTH_LONG).show();
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
                long lastTweetId1 = sharedPrefs.getLong("last_tweet_id_1", 0);
                long secondLastTweetId1 = sharedPrefs.getLong("second_last_tweet_id_1", 0);
                long lastMentionId1 = sharedPrefs.getLong("last_mention_id_1", 0);
                long lastDMId1 = sharedPrefs.getLong("last_dm_id_1", 0);
                long twitterId1 = sharedPrefs.getLong("twitter_id_1", 0);
                boolean isloggedin1 = sharedPrefs.getBoolean("is_logged_in_1", false);
                int keyVersion1 = sharedPrefs.getInt("key_version_1", 1);

                String authenticationToken2 = sharedPrefs.getString("authentication_token_2", "none");
                String authenticationTokenSecret2 = sharedPrefs.getString("authentication_token_secret_2", "none");
                String myScreenName2 = sharedPrefs.getString("twitter_screen_name_2", "");
                String myName2 = sharedPrefs.getString("twitter_users_name_2", "");
                String myBackgroundUrl2 = sharedPrefs.getString("twitter_background_url_2", "");
                String myProfilePicUrl2 = sharedPrefs.getString("profile_pic_url_2", "");
                long lastTweetId2 = sharedPrefs.getLong("last_tweet_id_2", 0);
                long secondLastTweetId2 = sharedPrefs.getLong("second_last_tweet_id_2", 0);
                long lastMentionId2 = sharedPrefs.getLong("last_mention_id_2", 0);
                long lastDMId2 = sharedPrefs.getLong("last_dm_id_2", 0);
                long twitterId2 = sharedPrefs.getLong("twitter_id_2", 0);
                boolean isloggedin2 = sharedPrefs.getBoolean("is_logged_in_2", false);
                int keyVersion2 = sharedPrefs.getInt("key_version_2", 1);

                String key = sharedPrefs.getString("consumer_key_2", "");

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
                e.putLong("twitter_id_1", twitterId1);
                e.putBoolean("is_logged_in_1", isloggedin1);
                e.putInt("key_version_1", keyVersion1);

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
                e.putLong("twitter_id_2", twitterId2);
                e.putBoolean("is_logged_in_2", isloggedin2);
                e.putInt("key_version_2", keyVersion2);

                e.putString("consumer_key_2", key);

                e.remove("new_notifications");
                e.remove("new_retweets");
                e.remove("new_favorites");
                e.remove("new_followers");
                e.remove("new_quotes");

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                e.remove("last_activity_refresh_" + currentAccount);
                e.remove("original_activity_refresh_" + currentAccount);
                e.remove("activity_follower_count_" + currentAccount);
                e.remove("activity_latest_followers_" + currentAccount);

                e.apply();

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

    public void setUpMuteSettings() {
        final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(getActivity());

        final Preference showHandle = findPreference("display_screen_name");
        if (sharedPrefs.getBoolean("both_handle_name", false) && showHandle != null) {
            showHandle.setEnabled(false);
        }

        final Preference muffle = findPreference("manage_muffles");
        muffle.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final Set<String> users = sharedPrefs.getStringSet("muffled_users", new HashSet<String>());
                final String[] set = new String[users.size()];
                final Object[] array = users.toArray();
                for (int i = 0; i < set.length; i++) {
                    set[i] = (String) array[i];
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(set, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        users.remove(set[item]);
                        sharedPrefs.edit().putStringSet("muffled_users", users).apply();

                        dialog.dismiss();
                    }
                });

                AlertDialog alert = builder.create();

                if (set.length == 0) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_users), Toast.LENGTH_SHORT).show();
                } else {
                    alert.show();
                }

                return false;
            }
        });
        final Preference newRegexMute = findPreference("mute_regex");
        newRegexMute.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.insert_regex_dialog);
                dialog.setTitle(getResources().getString(R.string.mute_expression) + ":");

                final FontPrefEditText expTV = (FontPrefEditText) dialog.findViewById(R.id.expression);

                Button cancel = (Button) dialog.findViewById(R.id.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                Button change = (Button) dialog.findViewById(R.id.ok);
                change.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String exp = expTV.getText().toString();
                        if (!exp.equals("")) {
                            String newRegex = sharedPrefs.getString("muted_regex", "") + exp + "   ";
                            sharedPrefs.edit().putString("muted_regex", newRegex).apply();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(context, getResources().getString(R.string.no_expression), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                dialog.show();
                return false;
            }
        });

        Preference mutedRegex = findPreference("manage_regex_mute");
        mutedRegex.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] exps = sharedPrefs.getString("muted_regex", "").split("   ");

                if (exps.length == 0 || (exps.length == 1 && exps[0].equals(""))) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_expression), Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(exps, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String newExps = "";

                            for (int i = 0; i < exps.length; i++) {
                                if (i != item) {
                                    newExps += exps[i] + "   ";
                                }
                            }

                            sharedPrefs.edit().putString("muted_regex", newExps).apply();

                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return false;
            }
        });

        Preference muted = findPreference("manage_mutes");
        muted.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] users = sharedPrefs.getString("muted_users", "").split(" ");

                if (users.length == 0 || (users.length == 1 && users[0].equals(""))) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_users), Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(users, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            ProfilePager.start(context, users[item].replace("@", "").replace(" ", ""));
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return false;
            }
        });

        Preference mutedRT = findPreference("manage_mutes_rt");
        mutedRT.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] users = sharedPrefs.getString("muted_rts", "").split(" ");

                if (users.length == 0 || (users.length == 1 && users[0].equals(""))) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_users), Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(users, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            ProfilePager.start(context, users[item].replace("@", "").replace(" ", ""));
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return false;
            }
        });

        Preference hashtags = findPreference("manage_mutes_hashtags");
        hashtags.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] tags = sharedPrefs.getString("muted_hashtags", "").split(" ");

                for (int i = 0; i < tags.length; i++) {
                    tags[i] = "#" + tags[i];
                }

                if (tags.length == 0 || (tags.length == 1 && tags[0].equals("#"))) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_hashtags), Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(tags, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String newTags = "";

                            for (int i = 0; i < tags.length; i++) {
                                if (i != item) {
                                    newTags += tags[i].replace("#", "") + " ";
                                }
                            }

                            sharedPrefs.edit().putString("muted_hashtags", newTags).apply();

                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return false;
            }
        });

        Preference clients = findPreference("manage_muted_clients");
        clients.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String[] tags = sharedPrefs.getString("muted_clients", "").split("   ");

                if (tags.length == 0 || (tags.length == 1 && tags[0].equals(""))) {
                    Toast.makeText(context, context.getResources().getString(R.string.no_clients), Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(tags, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String newClients = "";

                            for (int i = 0; i < tags.length; i++) {
                                if (i != item) {
                                    newClients += tags[i] + "   ";
                                }
                            }

                            sharedPrefs.edit().putString("muted_clients", newClients).apply();

                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                return false;
            }
        });
    }

    protected void showAccentPickerDialog(SharedPreferences sharedPrefs) {
        ScrollView scrollParent = new ScrollView(getActivity());
        LinearLayout colorPickerLayout = new LinearLayout(getActivity());
        colorPickerLayout.setOrientation(LinearLayout.VERTICAL);

        int cols = 4;
        float gridWidth = Utils.toPx(280, getActivity());
        GridView grid = getGridView();
        grid.setNumColumns(cols);

        final List<ThemeColor> colors = getAccentColors();
        final AlertDialog dialog = buildColorPickerDialog(scrollParent, getString(R.string.accent_color));

        AccentPickerAdapter adapter = new AccentPickerAdapter(getActivity(), colors, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accentPickerItemClicked(v, colors, dialog);
            }
        });
        grid.setAdapter(adapter);

        int rows = (int) Math.ceil(colors.size() / (double) cols);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams((int) gridWidth,
                (int) (gridWidth/4 * rows));
        gridParams.gravity = Gravity.CENTER;
        gridParams.topMargin = (int) Utils.toPx(16, getActivity());
        gridParams.bottomMargin = (int) Utils.toPx(16, getActivity());

        colorPickerLayout.addView(grid, gridParams);
        scrollParent.addView(colorPickerLayout);
        dialog.show();
    }

    protected void showColorPickerDialog(SharedPreferences sharedPrefs) {
        ScrollView scrollParent = new ScrollView(getActivity());
        LinearLayout colorPickerLayout = new LinearLayout(getActivity());
        colorPickerLayout.setOrientation(LinearLayout.VERTICAL);

        int cols = 4;
        float gridWidth = Utils.toPx(280, getActivity());
        GridView grid = getGridView();
        grid.setNumColumns(cols);

        final List<ThemeColor> colors = getThemeColors();
        final AlertDialog dialog = buildColorPickerDialog(scrollParent, getString(R.string.theme));

        ColorPickerAdapter adapter = new ColorPickerAdapter(getActivity(), colors, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                themePickerItemClicked(v, colors, dialog);
            }
        });
        grid.setAdapter(adapter);

        int rows = (int) Math.ceil(colors.size() / (double) cols);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams((int) gridWidth,
                (int) (gridWidth/4 * rows));
        gridParams.gravity = Gravity.CENTER;
        gridParams.topMargin = (int) Utils.toPx(16, getActivity());


        int padding = getResources().getDimensionPixelSize(R.dimen.settings_text_padding);
        Spinner mainTheme = getSpinner(sharedPrefs);
        LinearLayout.LayoutParams darkThemeParams = getLayoutParams(padding);
        gridParams.bottomMargin = padding;

        colorPickerLayout.addView(grid, gridParams);
        //colorPickerLayout.addView(mainTheme, darkThemeParams);
        scrollParent.addView(colorPickerLayout);
        dialog.show();
    }

    List<ThemeColor> getThemeColors() {
        String[] themePrefixes = getResources().getStringArray(R.array.theme_colors);
        final List<ThemeColor> colors = new ArrayList<ThemeColor>();
        for (String prefix : themePrefixes) {
            colors.add(new ThemeColor(prefix, getActivity(), true));
        }
        return colors;
    }

    List<ThemeColor> getAccentColors() {
        String[] themePrefixes = getResources().getStringArray(R.array.theme_colors);
        final List<ThemeColor> colors = new ArrayList<ThemeColor>();
        for (String prefix : themePrefixes) {
            colors.add(new ThemeColor(prefix, getActivity(), true));
        }

        // some of the accents are duplicated. Lets remove those.
        int i = 0;
        while (i < colors.size()) {
            boolean matched = false;
            int accent = colors.get(i).accentColor;
            for (int j = 0; j < colors.size(); j++) {
                if (j != i && accent == colors.get(j).accentColor) {
                    colors.remove(j);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                i++;
            }
        }

        return colors;
    }

    GridView getGridView() {
        return new GridView(getActivity());
    }

    AlertDialog buildColorPickerDialog(View layout, String title) {
        return new AlertDialog.Builder(getActivity())
                .setView(layout)
                .setTitle(title)
                .create();
    }

    void themePickerItemClicked(View v, List<ThemeColor> colors, Dialog dialog) {
        int position = (Integer) v.getTag();

        AppSettings settings = AppSettings.getInstance(getActivity());

        settings.setValue("material_theme_" + settings.currentAccount, position, getActivity());
        settings.setValue("material_accent_" + settings.currentAccount, -1, getActivity());
        settings.setValue("material_accent_light_" + settings.currentAccount, -1, getActivity());

        dialog.dismiss();
        AppSettings.invalidate();
        getActivity().recreate();
    }

    void accentPickerItemClicked(View v, List<ThemeColor> colors, Dialog dialog) {
        int position = (Integer) v.getTag();

        AppSettings settings = AppSettings.getInstance(getActivity());

        settings.setValue("material_accent_" + settings.currentAccount, colors.get(position).accentColor, getActivity());
        settings.setValue("material_accent_light_" + settings.currentAccount, colors.get(position).accentColorLight, getActivity());

        dialog.dismiss();
        AppSettings.invalidate();
        getActivity().recreate();
    }

    Spinner getSpinner(SharedPreferences sharedPrefs) {
        Spinner spinner = new Spinner(getActivity());

        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                getActivity().getResources().getStringArray(R.array.choose_theme));
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppSettings.getInstance(getActivity()).setValue("main_theme", position, getActivity());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && sharedPrefs.getInt("main_theme", AppSettings.DEFAULT_MAIN_THEME) != 0) {
            spinner.setPopupBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.dark_background)));
        }

        spinner.setSelection(sharedPrefs.getInt("main_theme", AppSettings.DEFAULT_MAIN_THEME));

        return spinner;
    }

    LinearLayout.LayoutParams getLayoutParams(int padding) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.rightMargin = padding;
        params.leftMargin = padding;
        return params;
    }

    public void setupAppStyle() {

        final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(getActivity());

        if (EmojiInitializer.INSTANCE.isAlreadyUsingGoogleAndroidO()) {
            getPreferenceScreen().removePreference(findPreference("emoji_style"));
        } else {
            findPreference("emoji_style").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    new Handler().postDelayed(new Runnable() {
                          @Override
                          public void run() {
                              AppSettings.invalidate();
                              EmojiInitializer.INSTANCE.initializeEmojiCompat(getActivity());
                          }
                    }, 500);
                    return true;
                }
            });
        }

        final Preference baseTheme = findPreference("main_theme_string");
        baseTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                AppSettings.invalidate();
                getActivity().recreate();
                return true;
            }
        });

        final Preference themePicker = findPreference("material_theme");
        themePicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showColorPickerDialog(sharedPrefs);
                return false;
            }
        });

        final Preference accentPicker = findPreference("accent_color");
        accentPicker.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showAccentPickerDialog(sharedPrefs);
                return false;
            }
        });

        final Preference deviceFont = findPreference("font_type");
        deviceFont.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                FontPrefTextView.typeface = null;
                FontPrefEditText.typeface = null;

                return true;
            }
        });


        final Preference nightMode = findPreference("night_mode");
        if (sharedPrefs.getBoolean("night_mode", false) && sharedPrefs.getInt("night_start_hour", 22) != -1) {
            nightMode.setSummary(getTime(sharedPrefs.getInt("night_start_hour", 22), sharedPrefs.getInt("night_start_min", 0), sharedPrefs.getBoolean("military_time", false)) +
                    " - " +
                    getTime(sharedPrefs.getInt("day_start_hour", 6), sharedPrefs.getInt("day_start_min", 0), sharedPrefs.getBoolean("military_time", false)));
        } else {
            nightMode.setSummary("");
        }
        nightMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!((SwitchPreference) nightMode).isChecked()) {
                    com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                            sharedPrefs.edit().putInt("night_start_hour", hourOfDay).putInt("night_start_min", minute).apply();

                            com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                                    sharedPrefs.edit().putInt("day_start_hour", hourOfDay).putInt("day_start_min", minute).apply();

                                    nightMode.setSummary(getTime(sharedPrefs.getInt("night_start_hour", 22), sharedPrefs.getInt("night_start_min", 0), sharedPrefs.getBoolean("military_time", false)) +
                                            " - " +
                                            getTime(sharedPrefs.getInt("day_start_hour", 6), sharedPrefs.getInt("day_start_min", 0), sharedPrefs.getBoolean("military_time", false)));

                                    new AlertDialog.Builder(context)
                                            .setTitle(R.string.night_mode_theme)
                                            .setItems(new String[]{context.getString(R.string.theme_dark), context.getString(R.string.theme_black)}, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    if (i == 0) {
                                                        sharedPrefs.edit().putBoolean("night_mode_black", false).commit();
                                                    } else {
                                                        sharedPrefs.edit().putBoolean("night_mode_black", true).commit();
                                                    }
                                                }
                                            }).show();

                                }
                            }, 6, 0, sharedPrefs.getBoolean("military_time", false), getString(R.string.night_mode_day));
                            dialog.show(getFragmentManager(), "night_mode_day");
                        }
                    }, 22, 0, sharedPrefs.getBoolean("military_time", false), getString(R.string.night_mode_night));
                    dialog.setThemeDark(true);
                    dialog.show(getFragmentManager(), "night_mode_night");
                } else {
                    nightMode.setSummary("");
                }

                return true;
            }

        });

        /*Preference download = findPreference("download_portal");
        download.setSummary(context.getResources().getString(R.string.download_portal_summary) + "\n\n" + context.getResources().getString(R.string.currently_in_beta));
        download.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    File f = new File(Environment.getExternalStorageDirectory() + "/Download/" + "klinker-apps-portal.apk");
                    f.delete();
                } catch (Exception e) {

                }
                final DownloadManager dm = (DownloadManager) context.getSystemService(Activity.DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse("http://klinkerapps.com/dev-upload/repository/lklinker/klinker-apps-portal.apk"));
                final long enqueue = dm.enqueue(request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI|DownloadManager.Request.NETWORK_MOBILE)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "klinker-apps-portal.apk"));

                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                            long downloadId = intent.getLongExtra(
                                    DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                            DownloadManager.Query mentionsQuery = new DownloadManager.Query();
                            mentionsQuery.setFilterById(enqueue);
                            Cursor c = dm.mentionsQuery(mentionsQuery);
                            if (c.moveToFirst()) {
                                int columnIndex = c
                                        .getColumnIndex(DownloadManager.COLUMN_STATUS);
                                if (DownloadManager.STATUS_SUCCESSFUL == c
                                        .getInt(columnIndex)) {

                                    Intent install = new Intent(Intent.ACTION_VIEW);
                                    install.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Download/" + "klinker-apps-portal.apk")), "application/vnd.android.package-archive");
                                    startActivity(install);
                                }
                            }

                        }
                    }
                };

                context.registerReceiver(receiver, new IntentFilter(
                        DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                return false;
            }
        });*/

    }

    public String getTime(int hours, int mins, boolean militaryTime) {
        String hour;
        String min;
        boolean pm = false;

        if (!militaryTime) {
            if (hours > 12) {
                pm = true;

                int x = hours - 12;
                hour = x + "";
            } else {
                hour = hours + "";
            }

            if (mins < 10) {
                min = "0" + mins;
            } else {
                min = mins + "";
            }

            return hour + ":" + min + (pm ? " PM" : " AM");
        } else {
            hour = hours < 10 ? "0" + hours : hours + "";

            if (mins < 10) {
                min = "0" + mins;
            } else {
                min = mins + "";
            }

            return hour + ":" + min;
        }
    }

    public void setUpNotificationSettings() {

        final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(getActivity());

        final Preference quietHours = findPreference("quiet_hours");
        if(sharedPrefs.getBoolean("quiet_hours", false)) {
            quietHours.setSummary(getTime(sharedPrefs.getInt("quiet_start_hour", 22), sharedPrefs.getInt("quiet_start_min", 0), sharedPrefs.getBoolean("military_time", false)) +
                    " - " +
                    getTime(sharedPrefs.getInt("quiet_end_hour", 6), sharedPrefs.getInt("quiet_end_min", 0), sharedPrefs.getBoolean("military_time", false)));
        } else {
            quietHours.setSummary("");
        }
        quietHours.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!((SwitchPreference) quietHours).isChecked()) {
                    com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                            sharedPrefs.edit().putInt("quiet_start_hour", hourOfDay).putInt("quiet_start_min", minute).apply();

                            com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                                    sharedPrefs.edit().putInt("quiet_end_hour", hourOfDay).putInt("quiet_end_min", minute).apply();

                                    quietHours.setSummary(getTime(sharedPrefs.getInt("quiet_start_hour", 22), sharedPrefs.getInt("quiet_start_min", 0), sharedPrefs.getBoolean("military_time", false)) +
                                            " - " +
                                            getTime(sharedPrefs.getInt("quiet_end_hour", 6), sharedPrefs.getInt("quiet_end_min", 0), sharedPrefs.getBoolean("military_time", false)));
                                }
                            }, 6, 0, sharedPrefs.getBoolean("military_time", false), getString(R.string.night_mode_day));
                            dialog.show(getFragmentManager(), "quiet_hours_end");
                        }
                    }, 22, 0, sharedPrefs.getBoolean("military_time", false), getString(R.string.night_mode_night));
                    dialog.setThemeDark(true);
                    dialog.show(getFragmentManager(), "quiet_hours_start");
                } else {
                    quietHours.setSummary("");
                }

                return true;
            }
        });

        findPreference("notification_channels").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                startActivity(intent);
                return false;
            }
        });

        Preference timelineSet = findPreference("timeline_set");
        timelineSet.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                return true;
            }
        });

        Preference interactionsSet = findPreference("interactions_set");
        interactionsSet.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                return true;
            }
        });

        Preference alertTypes = findPreference("alert_types");
        alertTypes.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                return true;
            }
        });

        Preference.OnPreferenceChangeListener click = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
                return true;
            }
        };

        Preference users = findPreference("favorite_users_notifications");
        users.setOnPreferenceChangeListener(click);

        Preference notification = findPreference("notification_options");
        notification.setOnPreferenceChangeListener(click);

        if (Utils.isAndroidO()) {
            //((PreferenceCategory) findPreference("advanced-notifications")).removePreference(findPreference("alert_types"));
        } else {
            ((PreferenceCategory) findPreference("advanced-notifications")).removePreference(findPreference("notification_channels"));
        }
    }

    public void setUpBackgroundRefreshes() {

    }

    public void setUpOtherOptions() {

    }

    public void setUpLocationSettings() {
        final Context context = getActivity();
        final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


        final Preference cities = findPreference("city");

        if (sharedPrefs.getBoolean("manually_config_location", false)) {
            cities.setSummary(sharedPrefs.getString("location", "Chicago"));
        }
        cities.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String country = sharedPrefs.getString("country", "United States");
                final String[][] full = LocalTrendsUtils.getArray(country);
                String[] names = new String[full.length];

                for (int i = 0; i <names.length; i++) {
                    String[] s = full[i];
                    names[i] = s[0];
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String id = full[item][1];
                        String name = full[item][0];

                        sharedPrefs.edit().putInt("woeid", Integer.parseInt(id)).apply();
                        sharedPrefs.edit().putString("location", name).apply();

                        cities.setSummary(name);

                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();

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

        SharedPreferences worldPrefs = AppSettings.getSharedPreferences(getActivity());

        // get the values and write them to our world prefs
        try {
            String s = sharedPrefs.getString(key, "");
            worldPrefs.edit().putString(key, s).apply();
        } catch (Exception e) {
            try {
                int i = sharedPrefs.getInt(key, -100);
                worldPrefs.edit().putInt(key, i).apply();
            } catch (Exception x) {
                try {
                    boolean b = sharedPrefs.getBoolean(key, false);
                    worldPrefs.edit().putBoolean(key, b).apply();
                } catch (Exception m) {

                }
            }
        }

        AppSettings.invalidate();
        ServiceUtils.rescheduleAllServices(context);

        if (key.equals("notification_options")) {
            if (sharedPrefs.getString("notification_options", "legacy").equals("push")) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.intercept_twitter_push_description)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                            } else {
                                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                            }
                        }).show();
            }
        } else if (key.equals("layout")) {
            new TrimCache(null).execute();
            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
        } else if (key.equals("alert_types")) {
            Log.v("notification_set", "alert being set");
            Set<String> set = sharedPrefs.getStringSet("alert_types", null);

            if (set == null) {
                return;
            }

            if (set.contains("1")) {
                sharedPrefs.edit().putBoolean("vibrate", true).apply();
                worldPrefs.edit().putBoolean("vibrate", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("vibrate", false).apply();
                worldPrefs.edit().putBoolean("vibrate", false).apply();
            }

            if (set.contains("2")) {
                sharedPrefs.edit().putBoolean("led", true).apply();
                worldPrefs.edit().putBoolean("led", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("led", false).apply();
                worldPrefs.edit().putBoolean("led", false).apply();
            }

            if (set.contains("3")) {
                sharedPrefs.edit().putBoolean("wake", true).apply();
                worldPrefs.edit().putBoolean("wake", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("wake", false).apply();
                worldPrefs.edit().putBoolean("wake", false).apply();
            }

            if (set.contains("4")) {
                sharedPrefs.edit().putBoolean("sound", true).apply();
                worldPrefs.edit().putBoolean("sound", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("sound", false).apply();
                worldPrefs.edit().putBoolean("sound", false).apply();
            }

            if (set.contains("5")) {
                sharedPrefs.edit().putBoolean("heads_up", true).apply();
                worldPrefs.edit().putBoolean("heads_up", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("heads_up", false).apply();
                worldPrefs.edit().putBoolean("heads_up", false).apply();
            }

        } else if (key.equals("timeline_set")) {
            Log.v("notification_set", "timeline being set");
            Set<String> set = sharedPrefs.getStringSet("timeline_set", null);

            if (set == null) {
                return;
            }

            if (set.contains("1")) {
                sharedPrefs.edit().putBoolean("timeline_notifications", true).apply();
                worldPrefs.edit().putBoolean("timeline_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("timeline_notifications", false).apply();
                worldPrefs.edit().putBoolean("timeline_notifications", false).apply();
            }

            if (set.contains("2")) {
                sharedPrefs.edit().putBoolean("direct_message_notifications", true).apply();
                worldPrefs.edit().putBoolean("direct_message_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("direct_message_notifications", false).apply();
                worldPrefs.edit().putBoolean("direct_message_notifications", false).apply();
            }

            if (set.contains("3")) {
                sharedPrefs.edit().putBoolean("activity_notifications", true).apply();
                worldPrefs.edit().putBoolean("activity_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("activity_notifications", false).apply();
                worldPrefs.edit().putBoolean("activity_notifications", false).apply();
            }
        } else if (key.equals("interactions_set")) {
            Log.v("notification_set", "interactions being set");
            Set<String> set = sharedPrefs.getStringSet("interactions_set", null);

            if (set == null) {
                return;
            }

            if (set.contains("1")) {
                sharedPrefs.edit().putBoolean("favorite_notifications", true).apply();
                worldPrefs.edit().putBoolean("favorite_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("favorite_notifications", false).apply();
                worldPrefs.edit().putBoolean("favorite_notifications", false).apply();
            }

            if (set.contains("2")) {
                sharedPrefs.edit().putBoolean("retweet_notifications", true).apply();
                worldPrefs.edit().putBoolean("retweet_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("retweet_notifications", false).apply();
                worldPrefs.edit().putBoolean("retweet_notifications", false).apply();
            }

            if (set.contains("3")) {
                sharedPrefs.edit().putBoolean("follower_notifications", true).apply();
                worldPrefs.edit().putBoolean("follower_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("follower_notifications", false).apply();
                worldPrefs.edit().putBoolean("follower_notifications", false).apply();
            }

            if (set.contains("4")) {
                sharedPrefs.edit().putBoolean("mentions_notifications", true).apply();
                worldPrefs.edit().putBoolean("mentions_notifications", true).apply();
            } else {
                sharedPrefs.edit().putBoolean("mentions_notifications", false).apply();
                worldPrefs.edit().putBoolean("mentions_notifications", false).apply();
            }
        } else if (key.equals("widget_theme") || key.equals("text_size")) {
            WidgetProvider.updateWidget(context);
        } else if (key.equals("locale")) {
            App.updateResources(context);
            getActivity().recreate();
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

            try {
                if (cache != null) {
                    cache.setSummary(getResources().getString(R.string.current_cache_size) + ": " + size / 1048576 + " MB");
                    //if (deleted) {
                    Toast.makeText(context, context.getResources().getString(R.string.trim_success), Toast.LENGTH_SHORT).show();
                /*} else {
                    Toast.makeText(context, context.getResources().getString(R.string.trim_fail), Toast.LENGTH_SHORT).show();
                }*/
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
            } catch (IllegalStateException e) {

            }


        }
    }

    class FillGaps extends AsyncTask<String, Void, Boolean> {

        private ProgressDialog pDialog;

        public List<twitter4j.Status> getList(int page, Twitter twitter) {
            try {
                return twitter.getHomeTimeline(new Paging(page, 200));
            } catch (Exception e) {
                return new ArrayList<twitter4j.Status>();
            }
        }

        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(context);
            pDialog.setMessage(getResources().getString(R.string.filling_timeline) + "...");
            pDialog.setIndeterminate(true);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected Boolean doInBackground(String... urls) {
            AppSettings settings = AppSettings.getInstance(context);

            try {
                int currentAccount = settings.currentAccount;

                Twitter twitter = Utils.getTwitter(context, settings);
                twitter.verifyCredentials();

                List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

                for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                    statuses.addAll(getList(i + 1, twitter));
                }

                for (twitter4j.Status status : statuses) {
                    try {
                        HomeDataSource.getInstance(context).createTweet(status, currentAccount, false);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

                HomeDataSource.getInstance(context).deleteDups(currentAccount);
                HomeDataSource.getInstance(context).markUnreadFilling(currentAccount);

                AppSettings.getSharedPreferences(context).edit().putBoolean("refresh_me", true).commit();

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
                return false;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean deleted) {

            try {
                if (deleted) {
                    Toast.makeText(context, context.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                    pDialog.dismiss();
                } else {
                    Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    pDialog.dismiss();
                }
            } catch (Exception e) {
                // user closed the window
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
            FollowersDataSource followers = FollowersDataSource.getInstance(context);

            followers.deleteAllUsers(sharedPrefs.getInt("current_account", 1));

            try {

                Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));

                int currentAccount = sharedPrefs.getInt("current_account", 1);
                PagableResponseList<User> friendsPaging = twitter.getFriendsList(screenName, -1, 200);

                for (User friend : friendsPaging) {
                    followers.createUser(friend, currentAccount);
                }

                long nextCursor = friendsPaging.getNextCursor();

                while (nextCursor != -1) {
                    friendsPaging = twitter.getFriendsList(screenName, nextCursor, 200);

                    for (User friend : friendsPaging) {
                        followers.createUser(friend, currentAccount);
                    }

                    nextCursor = friendsPaging.getNextCursor();
                }

            } catch (Exception e) {
                // something wrong haha
            }

            return true;
        }

        protected void onPostExecute(Boolean deleted) {

            try {
                pDialog.dismiss();
            } catch (Exception e) {
                // closed the app
            }

            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.sync_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.sync_failed), Toast.LENGTH_SHORT).show();
            }


        }
    }

    private class TrimDatabase extends AsyncTask<String, Void, Boolean> {

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
            try {
                pDialog.dismiss();
            } catch (Exception e) {
                // not attached
            }
            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.trim_success), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.trim_fail), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setUpGetHelpSettings() {
        final SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(getActivity());

        Preference tutorial = findPreference("tutorial");
        tutorial.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent tutorial = new Intent(context, MainActivity.class);
                tutorial.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                tutorial.putExtra("tutorial", true);
                sharedPrefs.edit().putBoolean("should_refresh", false).apply();
                sharedPrefs.edit().putBoolean("done_tutorial", false).apply();
                startActivity(tutorial);
                return false;
            }
        });

        Preference changelog = findPreference("whats_new");
        changelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
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

                new AlertDialog.Builder(context)
                        .setTitle(R.string.changelog)
                        .setView(list)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return false;
            }
        });

        Preference faq = findPreference("faq");
        faq.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                XmlFaqUtils.showFaqDialog(context);
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
                return false;
            }
        });

        Preference email = findPreference("email_me");
        email.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"luke@klinkerapps.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Talon (Plus)");
                emailIntent.setType("plain/text");

                startActivity(emailIntent);
                return false;
            }
        });

        Preference tweet = findPreference("tweet_me");
        tweet.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent tweet = new Intent(getActivity(), ComposeActivity.class);
                new AlertDialog.Builder(context)
                        .setItems(new CharSequence[] {"@TalonAndroid", "@lukeklinker"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) {
                                    tweet.putExtra("user", "@TalonAndroid");
                                } else {
                                    tweet.putExtra("user", "@lukeklinker");
                                }
                                startActivity(tweet);
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });

        Preference followTalon = findPreference("follow_talon");
        followTalon.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                new AlertDialog.Builder(context)
                        .setItems(new CharSequence[] {"@TalonAndroid", "@lukeklinker", "Luke's Google+"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) {
                                    ProfilePager.start(context, "Talon", "TalonAndroid", null);
                                } else if (i == 1) {
                                    ProfilePager.start(context, "Luke Klinker", "lukeklinker", null);
                                } else {
                                    // luke (google+)
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://google.com/+LukeKlinker")));
                                }

                            }
                        })
                        .create()
                        .show();

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

        Preference pulse = findPreference("pulsesms");
        pulse.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=xyz.klinker.messenger")));
                return false;
            }
        });

        Preference evolve = findPreference("evolvesms");
        evolve.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.evolve_sms")));
                return false;
            }
        });

        Preference blur = findPreference("blur_launcher");
        blur.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.launcher")));
                return false;
            }
        });

        Preference source = findPreference("source");
        source.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.reader")));
                return false;
            }
        });

        Preference spotlight = findPreference("theme_spotlight");
        spotlight.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.theme_spotlight")));
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

        findPreference("theme_spotlight").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.theme_spotlight")));
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