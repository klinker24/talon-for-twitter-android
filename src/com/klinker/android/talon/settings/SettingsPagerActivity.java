package com.klinker.android.talon.settings;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.droidprism.APN;
import com.droidprism.Carrier;
import com.klinker.android.messaging_donate.MainActivity;
import com.klinker.android.messaging_donate.R;
import com.klinker.android.messaging_donate.utils.IOUtil;
import com.klinker.android.messaging_donate.utils.Util;
import com.klinker.android.messaging_sliding.DeleteOldService;
import com.klinker.android.messaging_sliding.backup.BackupService;
import com.klinker.android.messaging_sliding.blacklist.BlacklistActivity;
import com.klinker.android.messaging_sliding.notifications.NotificationsSettingsActivity;
import com.klinker.android.messaging_sliding.receivers.NotificationReceiver;
import com.klinker.android.messaging_sliding.scheduled.ScheduledSms;
import com.klinker.android.messaging_sliding.security.SetPasswordActivity;
import com.klinker.android.messaging_sliding.security.SetPinActivity;
import com.klinker.android.messaging_sliding.slide_over.SlideOverService;
import com.klinker.android.messaging_sliding.templates.TemplateActivity;
import com.klinker.android.messaging_sliding.theme.PopupChooserActivity;
import com.klinker.android.messaging_sliding.theme.ThemeChooserActivity;
import com.klinker.android.messaging_sliding.views.HoloEditText;
import com.klinker.android.messaging_sliding.views.HoloTextView;
import com.klinker.android.messaging_sliding.views.NumberPickerDialog;
import com.klinker.android.send_message.Utils;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import group.pals.android.lib.ui.lockpattern.prefs.SecurityPrefs;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class SettingsPagerActivity extends FragmentActivity {

    SectionsPagerAdapter mSectionsPagerAdapter;
    SharedPreferences sharedPrefs;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private LinearLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private static final int REQ_CREATE_PATTERN = 3;

    private boolean showAll;
    private boolean userKnows;
    public static boolean settingsLinksActive = true;
    public static boolean inOtherLinks = true;

    private String[] linkItems;
    private String[] otherItems;

    private Activity activity;

    public static ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_main);

        DrawerArrayAdapter.current = 0;

        linkItems = new String[]{getResources().getString(R.string.theme_settings),
                getResources().getString(R.string.notification_settings),
                getResources().getString(R.string.popup_settings),
                getResources().getString(R.string.slideover_settings),
                getResources().getString(R.string.text_settings),
                getResources().getString(R.string.conversation_settings),
                getResources().getString(R.string.mms_settings),
                getResources().getString(R.string.google_voice_settings),
                getResources().getString(R.string.security_settings),
                getResources().getString(R.string.advanced_settings)};

        otherItems = new String[]{getResources().getString(R.string.quick_templates),
                getResources().getString(R.string.scheduled_sms),
                getResources().getString(R.string.get_help),
                getResources().getString(R.string.other_apps),
                getResources().getString(R.string.rate_it)};

        activity = this;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.links_list);
        mDrawer = (LinearLayout) findViewById(R.id.drawer);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.drawer_spinner_array, R.layout.drawer_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new SpinnerClickListener());

        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerArrayAdapter(this,
                new ArrayList<String>(Arrays.asList(linkItems))));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        );

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        try {
            if (getIntent().getBooleanExtra("mms", false)) {
                mViewPager.setCurrentItem(6, true);
            }

            int pageNumber = getIntent().getIntExtra("page_number", 0);
            mViewPager.setCurrentItem(pageNumber, true);
        } catch (Exception e) {

        }

        showAll = sharedPrefs.getBoolean("show_all_settings", false);
        userKnows = sharedPrefs.getBoolean("user_knows_navigation_drawer", false);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                DrawerArrayAdapter.current = position;
                mDrawerList.invalidateViews();
            }
        });

        if (!sharedPrefs.getBoolean("knows_show_all", false)) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    this);

            // set title
            alertDialogBuilder.setTitle("\"Show All Settings\" Tip");

            // set dialog message
            alertDialogBuilder
                    .setMessage(getResources().getString(R.string.show_all_disclaimer))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.close),new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            // don't write to the settings preference, they may want to see it again
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton(getResources().getString(R.string.never_again),new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            // Don't show this to the user again
                            sharedPrefs.edit().putBoolean("knows_show_all", true).commit();
                            dialog.cancel();
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();
        }

        if (!userKnows) {
            mDrawerLayout.openDrawer(mDrawer);
        }

        Util.checkOverride(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private class SpinnerClickListener implements Spinner.OnItemSelectedListener {
        @Override
        // sets the string repetition to whatever is choosen from the spinner
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            // An item was selected. You can retrieve the selected item using
            // parent.getItemAtPosition(pos)
            String selected = parent.getItemAtPosition(pos).toString();

            if (selected.equals(getResources().getStringArray(R.array.drawer_spinner_array)[0])) {
                mDrawerList.setAdapter(new DrawerArrayAdapter(activity,
                        new ArrayList<String>(Arrays.asList(linkItems))));
                mDrawerList.invalidate();
                settingsLinksActive = true;
            } else {
                mDrawerList.setAdapter(new DrawerArrayAdapter(activity,
                        new ArrayList<String>(Arrays.asList(otherItems))));
                mDrawerList.invalidate();
                settingsLinksActive = false;
            }


        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {

            final Context context = getApplicationContext();
            Intent intent;
            final int mPos = position;

            if (settingsLinksActive) {
                mViewPager.setCurrentItem(mPos, true);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDrawerLayout.closeDrawer(mDrawer);
                    }
                }, 200);
            } else {
                mDrawerLayout.closeDrawer(mDrawer);

                switch (position) {
                    case 0:
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent mIntent = new Intent(context, TemplateActivity.class);
                                mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                                overridePendingTransition(0, 0);
                            }
                        }, 200);
                        break;

                    case 1:

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent mIntent = new Intent(context, ScheduledSms.class);
                                mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                                overridePendingTransition(0, 0);
                            }
                        }, 200);
                        break;

                    case 2:
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent mIntent = new Intent(context, GetHelpSettingsActivity.class);
                                mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                                overridePendingTransition(0, 0);
                            }
                        }, 200);

                        break;

                    case 3:
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent mIntent = new Intent(context, OtherAppsSettingsActivity.class);
                                mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                                overridePendingTransition(0, 0);
                            }
                        }, 200);

                        break;

                    case 4:
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

                                try {
                                    startActivity(goToMarket);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(context, "Couldn't launch the market", Toast.LENGTH_SHORT).show();
                                }
                                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                            }
                        }, 200);

                        break;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                /*if (mViewPager.getCurrentItem() == 0)
                {
                   onBackPressed();
                } else
                {
                    mViewPager.setCurrentItem(0, true);
                }*/

                // Pass the event to ActionBarDrawerToggle, if it returns
                // true, then it has handled the app icon touch event
                if (mDrawerToggle.onOptionsItemSelected(item)) {
                    if (!userKnows) {
                        userKnows = true;

                        sharedPrefs.edit().putBoolean("user_knows_navigation_drawer", true).commit();
                    }
                    return true;
                }

                // Handle your other action bar items...

                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

    }

    @Override
    public void onBackPressed() {
//        Intent i = new Intent(this, MainActivity.class);
//        startActivity(i);
        finish();
        setResult(Activity.RESULT_OK);
        overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public static final int NUM_PAGES = 10;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            if (position != 7) {
                PreferenceFragment fragment = new PrefFragment();
                Bundle args = new Bundle();
                args.putInt("position", position);
                fragment.setArguments(args);
                return fragment;
            } else {
                GoogleVoiceFragment fragment = new GoogleVoiceFragment();
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                //case 0:
                //return getResources().getString(R.string.menu_settings);
                case 0:
                    return getResources().getString(R.string.theme_settings);
                case 1:
                    return getResources().getString(R.string.sliding_notification_settings);
                case 2:
                    return getResources().getString(R.string.popup_settings);
                case 3:
                    return getResources().getString(R.string.slideover_settings);
                case 4:
                    return getResources().getString(R.string.text_settings);
                case 5:
                    return getResources().getString(R.string.conversation_settings);
                case 6:
                    return getResources().getString(R.string.mms_settings);
                case 7:
                    return getResources().getString(R.string.google_voice_settings);
                case 8:
                    return getResources().getString(R.string.security_settings);
                case 9:
                    return getResources().getString(R.string.advanced_settings);
            }
            return null;
        }
    }

    public class PrefFragment extends PreferenceFragment {

        public int position;

        public PrefFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            position = args.getInt("position");

            DrawerArrayAdapter.current = position - 1;
            mDrawerList.setAdapter(new DrawerArrayAdapter(activity,
                    new ArrayList<String>(Arrays.asList(linkItems))));

            switch (position) {
                case 0:
                    addPreferencesFromResource(R.xml.sliding_theme_settings);
                    setUpThemeSettings();
                    break;
                case 1:
                    addPreferencesFromResource(R.xml.sliding_notification_settings);
                    setUpNotificationSettings();
                    break;
                case 2:
                    addPreferencesFromResource(R.xml.popup_settings);
                    setUpPopupSettings();
                    break;
                case 3:
                    addPreferencesFromResource(R.xml.slideover_settings);
                    setUpSlideOverSettings();
                    break;
                case 4:
                    addPreferencesFromResource(R.xml.sliding_message_settings);
                    setUpMessageSettings();
                    break;
                case 5:
                    addPreferencesFromResource(R.xml.sliding_conversation_settings);
                    setUpConversationSettings();
                    break;
                case 6:
                    addPreferencesFromResource(R.xml.mms_settings);
                    setUpMmsSettings();
                    break;
                case 8:
                    addPreferencesFromResource(R.xml.sliding_security_settings);
                    setUpSecuritySettings();
                    break;
                case 9:
                    addPreferencesFromResource(R.xml.sliding_advanced_settings);
                    setUpAdvancedSettings();
                    break;
            }
        }

        public void setUpThemeSettings() {
            final Context context = getActivity();
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            Preference titleSettings = findPreference("title_prefs");
            titleSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(context, TitleBarSettingsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                    return true;
                }
            });

            Preference customThemeSettings = findPreference("custom_theme_prefs");
            customThemeSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(context, ThemeChooserActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);

                    return true;
                }
            });

            Preference customBackground = findPreference("custom_background");
            customBackground.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (sharedPrefs.getBoolean("custom_background", false)) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Menu Background Picture"), 1);
                    }

                    return true;
                }

            });

            Preference customBackground2 = findPreference("custom_background2");
            customBackground2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (sharedPrefs.getBoolean("custom_background2", false)) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Message Background Picture"), 2);
                    }

                    return true;
                }

            });

            Preference deviceFont = findPreference("device_font");
            deviceFont.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    HoloTextView.typeface = null;
                    HoloEditText.typeface = null;
                    return false;
                }
            });

            if (!showAll) {
                try {
                    ((PreferenceGroup) findPreference("smilies_category")).removePreference(findPreference("smiliesType"));
                    ((PreferenceGroup) findPreference("smilies_category")).removePreference(findPreference("use_system_emojis"));
                    ((PreferenceGroup) findPreference("emoji_category")).removePreference(findPreference("emoji_keyboard"));
                    ((PreferenceGroup) findPreference("emoji_category")).removePreference(findPreference("emoji_type"));
                    ((PreferenceGroup) findPreference("emoji_category")).removePreference(findPreference("emoji_keyboard_color"));
                    ((PreferenceGroup) findPreference("look_style_category")).removePreference(findPreference("device_font"));
                    ((PreferenceGroup) findPreference("look_style_category")).removePreference(findPreference("custom_background"));
                    ((PreferenceGroup) findPreference("look_style_category")).removePreference(findPreference("custom_background2"));
                    ((PreferenceGroup) findPreference("look_style_category")).removePreference(findPreference("page_or_menu2"));
                } catch (Exception e) {

                }
            }
        }

        public void setUpNotificationSettings() {
            final Context context = getActivity();

            Preference indiv = (Preference) findPreference("individual_notification_settings");
            indiv.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(context, NotificationsSettingsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                    return false;
                }

            });

            Preference blacklistSettings = (Preference) findPreference("blacklist_settings");
            blacklistSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(context, BlacklistActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                    return true;
                }
            });

            Preference testNotification = findPreference("test_notification");
            testNotification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    giveTestNotification();
                    return true;
                }
            });

            TelephonyManager manager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();

            if (!(carrierName.equalsIgnoreCase("giffgaff") || carrierName.replace(" ", "").equalsIgnoreCase("o2-uk")) && !sharedPrefs.getBoolean("giffgaff_delivery", false)) {
                ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("giffgaff_delivery"));
            }

            if (!showAll) {
                ((PreferenceGroup) findPreference("general_notification_category")).removePreference(findPreference("in_app_notifications"));
                ((PreferenceGroup) findPreference("general_notification_category")).removePreference(findPreference("quick_text_slideover"));
                ((PreferenceGroup) findPreference("general_notification_category")).removePreference(findPreference("quick_text"));
                ((PreferenceGroup) findPreference("notification_look_category")).removePreference(findPreference("breath"));
                ((PreferenceGroup) findPreference("notification_look_category")).removePreference(findPreference("repeating_notification"));
                ((PreferenceGroup) findPreference("notification_look_category")).removePreference(findPreference("stack_notifications"));
                ((PreferenceGroup) findPreference("notification_vibrate_category")).removePreference(findPreference("custom_vibrate_pattern"));
                ((PreferenceGroup) findPreference("notification_vibrate_category")).removePreference(findPreference("set_custom_vibrate_pattern"));
                ((PreferenceGroup) findPreference("notification_led_category")).removePreference(findPreference("led_off_time"));
                ((PreferenceGroup) findPreference("notification_led_category")).removePreference(findPreference("led_on_time"));
                ((PreferenceGroup) findPreference("notification_look_category")).removePreference(findPreference("button_options"));
                ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("secure_notification"));
                ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("blacklist_settings"));
                try { ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("giffgaff_delivery")); } catch (Exception e) { }
                ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("delivery_reports_type"));
                ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("swipe_read"));
                ((PreferenceGroup) findPreference("notification_other_category")).removePreference(findPreference("auto_clear_fn"));
                ((PreferenceGroup) findPreference("wake_notification_category")).removePreference(findPreference("screen_timeout"));
            }
        }

        public void setUpPopupSettings() {
            final Context context = getActivity();
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Preference customPopup = (Preference) findPreference("popup_theme");

            if (sharedPrefs.getBoolean("full_app_popup", true)) {
                customPopup.setEnabled(false);
                customPopup.setSelectable(false);
                customPopup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        return true;
                    }
                });

                try {
                    findPreference("halo_popup").setEnabled(false);
                    findPreference("enable_view_conversation").setEnabled(false);
                    findPreference("text_alignment2").setEnabled(false);
                    findPreference("use_old_popup").setEnabled(false);
                    findPreference("dark_theme_quick_reply").setEnabled(false);
                } catch (Exception e) {

                }
            } else {
                if (!sharedPrefs.getBoolean("use_old_popup", false)) {
                    customPopup.setEnabled(true);
                    customPopup.setSelectable(true);
                    customPopup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            Intent intent = new Intent(context, PopupChooserActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                            return true;
                        }
                    });
                } else {
                    customPopup.setEnabled(false);
                    customPopup.setSelectable(false);
                    customPopup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        public boolean onPreferenceClick(Preference preference) {
                            return true;
                        }
                    });
                }

                try {
                    findPreference("halo_popup").setEnabled(true);
                    findPreference("enable_view_conversation").setEnabled(true);
                    findPreference("text_alignment2").setEnabled(true);
                    findPreference("use_old_popup").setEnabled(true);
                    findPreference("dark_theme_quick_reply").setEnabled(true);
                } catch (Exception e) {

                }
            }

            try {
                Preference oldPopup = findPreference("use_old_popup");
                oldPopup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        setUpPopupSettings();
                        return true;
                    }
                });
            } catch (Exception e) {

            }

            Preference slideOver = findPreference("full_app_popup");
            slideOver.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    setUpPopupSettings();
                    return true;
                }
            });

            if (!showAll) {
                try {
                    ((PreferenceGroup) findPreference("slideover_popup_category")).removePreference(findPreference("slideover_popup_lockscreen_only"));
                    ((PreferenceGroup) findPreference("card_popup_category")).removePreference(findPreference("enable_view_conversation"));
                    getPreferenceScreen().removePreference(findPreference("show_keyboard_popup"));
                    getPreferenceScreen().removePreference(findPreference("text_alignment2"));
                    ((PreferenceGroup) findPreference("old_popup_category")).removePreference(findPreference("use_old_popup"));
                    ((PreferenceGroup) findPreference("old_popup_category")).removePreference(findPreference("dark_theme_quick_reply"));
                    getPreferenceScreen().removePreference(findPreference("enable_view_conversation"));
                    ((PreferenceGroup) findPreference("experimental_popup_category")).removeAll();
                    getPreferenceScreen().removePreference(findPreference("experimental_popup_category"));
                    getPreferenceScreen().removePreference(findPreference("old_popup_category"));
                } catch (Exception e) {

                }
            }
        }

        public void setUpSlideOverSettings() {

            final Context context = getActivity();

            Preference googlePlus = findPreference("slideover_help");
            googlePlus.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/117432358268488452276/posts/S1YMm5K69bQ")));
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                    return false;
                }

            });

            Preference onlyQuickPeek = findPreference("only_quickpeek");
            onlyQuickPeek.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference enableQuickPeek = findPreference("enable_quick_peek");
            enableQuickPeek.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference foreground = findPreference("foreground_service");
            foreground.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    SlideOverService.restartHalo(context);
                    return false;
                }

            });

            Preference voiceSend = findPreference("quick_peek_send_voice");
            voiceSend.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference slideOver = findPreference("slideover_enabled");
            slideOver.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SlideOverService.restartHalo(context);
                    return false;
                }
            });

            Preference textMarkers = findPreference("quick_peek_text_markers");
            textMarkers.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SlideOverService.restartHalo(context);
                    return false;
                }
            });

            Preference size = findPreference("scaled_size");
            size.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    SlideOverService.restartHalo(context);
                    return false;
                }

            });

            /*Preference side = findPreference("slideover_side");
            side.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    restartHalo();
                    return true;
                }
            });*/

            Preference sliver = findPreference("slideover_sliver");
            sliver.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference sliverNew = findPreference("slideover_new_sliver");
            sliverNew.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference unreadOnly = findPreference("slideover_only_unread");
            unreadOnly.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference disableDrag = findPreference("slideover_disable_drag");
            disableDrag.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference disableSliverDrag = findPreference("slideover_disable_sliver_drag");
            disableSliverDrag.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference quickPeekTransparency = findPreference("quick_peek_transparency");
            quickPeekTransparency.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference smsToStore = (Preference) findPreference("quick_peek_contact_num");
            smsToStore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {

                    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
                            new NumberPickerDialog.OnNumberSetListener() {
                                public void onNumberSet(int limit) {
                                    SharedPreferences.Editor editor = sharedPrefs.edit();

                                    editor.putInt("quick_peek_contact_num", limit);
                                    editor.commit();
                                }
                            };

                    new NumberPickerDialog(context, mSmsLimitListener, sharedPrefs.getInt("quick_peek_contact_num", 5), 1, 5, R.string.quick_peek_contact_num).show();

                    SlideOverService.restartHalo(context);

                    return false;
                }

            });

            Preference quickPeekMessages = (Preference) findPreference("quick_peek_messages");
            quickPeekMessages.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {

                    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
                            new NumberPickerDialog.OnNumberSetListener() {
                                public void onNumberSet(int limit) {
                                    SharedPreferences.Editor editor = sharedPrefs.edit();

                                    editor.putInt("quick_peek_messages", limit);
                                    editor.commit();
                                }
                            };

                    new NumberPickerDialog(context, mSmsLimitListener, sharedPrefs.getInt("quick_peek_messages", 3), 1, 5, R.string.quick_peek_messages).show();

                    SlideOverService.restartHalo(context);

                    return false;
                }

            });
            /*Preference alignment = findPreference("slideover_vertical");
            alignment.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    restartHalo();
                    return true;
                }
            });*/

            Preference activation = findPreference("slideover_activation");
            activation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference breakPoint = findPreference("slideover_break_point");
            breakPoint.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            Preference haptic = findPreference("slideover_haptic_feedback");
            haptic.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            ColorPickerPreference haloColor = (ColorPickerPreference) findPreference("slideover_color");
            haloColor.setAlphaSliderEnabled(true);
            haloColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            ColorPickerPreference haloUnreadColor = (ColorPickerPreference) findPreference("slideover_unread_color");
            haloUnreadColor.setAlphaSliderEnabled(true);
            haloUnreadColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SlideOverService.restartHalo(context);
                    return true;
                }
            });

            if (!showAll) {
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("slideover_break_point"));
                ((PreferenceGroup) findPreference("slideover_general_category")).removePreference(findPreference("slideover_secondary_action"));
                ((PreferenceGroup) findPreference("slideover_general_category")).removePreference(findPreference("foreground_service"));
                ((PreferenceGroup) findPreference("slideover_general_category")).removePreference(findPreference("slideover_haptic_feedback"));
                ((PreferenceGroup) findPreference("slideover_general_category")).removePreference(findPreference("slideover_hide_notifications"));
                ((PreferenceGroup) findPreference("slideover_general_category")).removePreference(findPreference("contact_picture_slideover"));
                ((PreferenceGroup) findPreference("slideover_quick_peek")).removePreference(findPreference("quick_peek_send_voice"));
                ((PreferenceGroup) findPreference("slideover_quick_peek")).removePreference(findPreference("only_quickpeek"));
                ((PreferenceGroup) findPreference("slideover_quick_peek")).removePreference(findPreference("quick_peek_messages"));
                ((PreferenceGroup) findPreference("slideover_quick_peek")).removePreference(findPreference("quick_peek_text_markers"));
                ((PreferenceGroup) findPreference("slideover_quick_peek")).removePreference(findPreference("quick_peek_transparency"));
                ((PreferenceGroup) findPreference("slideover_quick_peek")).removePreference(findPreference("close_quick_peek_on_send"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("slideover_disable_drag"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("slideover_disable_sliver_drag"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("slideover_new_sliver"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("ping_on_unlock"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("animate_text_on_ping"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("slideover_activation"));
                ((PreferenceGroup) findPreference("slideover_positioning_category")).removePreference(findPreference("slideover_return_timeout_length"));
                ((PreferenceGroup) findPreference("slideover_settings_popup_category")).removePreference(findPreference("disable_backgrounds"));
                ((PreferenceGroup) findPreference("slideover_settings_popup_category")).removePreference(findPreference("slideover_height"));
                ((PreferenceGroup) findPreference("slideover_settings_popup_category")).removePreference(findPreference("slideover_width"));
                ((PreferenceGroup) findPreference("slideover_themeing")).removePreference(findPreference("slideover_animation_speed"));
                ((PreferenceGroup) findPreference("slideover_themeing")).removePreference(findPreference("slideover_unread_color"));
            }
        }

        public void setUpMessageSettings() {
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            if (sharedPrefs.getString("run_as", "sliding").equals("sliding") || sharedPrefs.getString("run_as", "sliding").equals("hangout") || sharedPrefs.getString("run_as", "sliding").equals("card2") || sharedPrefs.getString("run_as", "sliding").equals("card+")) {

            } else {
                getPreferenceScreen().removePreference(findPreference("text_alignment"));
                getPreferenceScreen().removePreference(findPreference("contact_pictures"));
                getPreferenceScreen().removePreference(findPreference("auto_insert_draft"));
            }

            if (!showAll) {
                ((PreferenceGroup) findPreference("message_theme_category")).removePreference(findPreference("tiny_date"));
                ((PreferenceGroup) findPreference("general_messages_category")).removePreference(findPreference("limit_messages"));
                ((PreferenceGroup) findPreference("message_theme_category")).removePreference(findPreference("show_original_timestamp"));
                ((PreferenceGroup) findPreference("message_theme_category")).removePreference(findPreference("text_alignment"));
                ((PreferenceGroup) findPreference("messages_animation_category")).removePreference(findPreference("animation_speed"));
            }
        }

        public void setUpConversationSettings() {
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            if (sharedPrefs.getString("run_as", "sliding").equals("sliding") || sharedPrefs.getString("run_as", "sliding").equals("hangout") || sharedPrefs.getString("run_as", "sliding").equals("card2") || sharedPrefs.getString("run_as", "sliding").equals("card+")) {
                ((PreferenceGroup) findPreference("conversation_theme_category")).removePreference(findPreference("hide_contact_number"));
                ((PreferenceGroup) findPreference("conversation_theme_category")).removePreference(findPreference("open_to_first"));
            } else {
                getPreferenceScreen().removePreference(findPreference("pin_conversation_list"));
                getPreferenceScreen().removePreference(findPreference("contact_pictures2"));
                getPreferenceScreen().removePreference(findPreference("open_contact_menu"));
                getPreferenceScreen().removePreference(findPreference("slide_messages"));
            }

            if (!showAll) {
                ((PreferenceGroup) findPreference("conversation_theme_category")).removePreference(findPreference("hide_message_counter"));
                ((PreferenceGroup) findPreference("conversation_theme_category")).removePreference(findPreference("pin_conversation_list"));
                ((PreferenceGroup) findPreference("conversation_theme_category")).removePreference(findPreference("conversation_list_images"));
                ((PreferenceGroup) findPreference("conversation_theme_category")).removePreference(findPreference("hide_date_conversations"));
                ((PreferenceGroup) findPreference("general_conversation_category")).removePreference(findPreference("limit_conversations_start"));
                ((PreferenceGroup) findPreference("general_conversation_category")).removePreference(findPreference("slide_messages"));
            }
        }

        public void setUpMmsSettings() {
            boolean isTablet;

            Preference mmsc, proxy, port;
            final Context context = getActivity();
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            try {
                if ((Utils.getMyPhoneNumber(context) == null || Utils.getMyPhoneNumber(context).equals("")) && tabletSize(context) > 6.5 && xLargeScreen(context)) {
                    isTablet = true;
                } else {
                    isTablet = false;
                }
            } catch (Exception e) {
                isTablet = true;
            }

            if (isTablet) {
                findPreference("group_message").setEnabled(false);
                findPreference("auto_download_mms").setEnabled(false);
                findPreference("wifi_mms_fix").setEnabled(false);
                findPreference("send_as_mms").setEnabled(false);
                findPreference("mms_after").setEnabled(false);
                findPreference("send_with_stock").setEnabled(false);
                findPreference("receive_with_stock").setEnabled(false);
                findPreference("auto_select_apn").setEnabled(false);
                findPreference("preset_apns").setEnabled(false);
                findPreference("mmsc_url").setEnabled(false);
                findPreference("mms_proxy").setEnabled(false);
                findPreference("mms_port").setEnabled(false);
                findPreference("mms_disclaimer").setEnabled(false);
                findPreference("get_apn_help").setEnabled(false);
            }
            Preference smsToStore = (Preference) findPreference("mms_after");
            smsToStore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (sharedPrefs.getBoolean("send_as_mms", false)) {
                        NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
                                new NumberPickerDialog.OnNumberSetListener() {
                                    public void onNumberSet(int limit) {
                                        SharedPreferences.Editor editor = sharedPrefs.edit();

                                        editor.putInt("mms_after", limit);
                                        editor.commit();
                                    }
                                };

                        new NumberPickerDialog(context, mSmsLimitListener, sharedPrefs.getInt("mms_after", 4), 1, 1000, R.string.mms_after).show();
                    }

                    return false;
                }

            });

            mmsc = (Preference) findPreference("mmsc_url");
            mmsc.setSummary(sharedPrefs.getString("mmsc_url", ""));

            proxy = (Preference) findPreference("mms_proxy");
            proxy.setSummary(sharedPrefs.getString("mms_proxy", ""));

            port = (Preference) findPreference("mms_port");
            port.setSummary(sharedPrefs.getString("mms_port", ""));

            Preference autoSelect = findPreference("auto_select_apn");
            autoSelect.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    final String networkOperator = manager.getNetworkOperator();

                    if (networkOperator != null) {
                        final ProgressDialog dialog = new ProgressDialog(context);
                        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        dialog.setMessage(context.getString(R.string.finding_apns));
                        dialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    int mcc = Integer.parseInt(networkOperator.substring(0, 3));
                                    String s = networkOperator.substring(3);
                                    int mnc = Integer.parseInt(s.replaceFirst("^0{1,2}", ""));
                                    Carrier c = Carrier.getCarrier(mcc, mnc);
                                    APN a = c.getAPN();

                                    sharedPrefs.edit().putString("mmsc_url", a.mmsc).putString("mms_proxy", a.proxy).putString("mms_port", a.port + "").commit();
                                } catch (Throwable e) {
                                    ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context, "Error, couldn't get system APNs.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setUpMmsSettings();
                                        dialog.dismiss();
                                    }
                                });
                            }
                        }).start();
                    } else {
                        Toast.makeText(context, "Error, no network operator.", Toast.LENGTH_SHORT).show();
                    }

                    return false;
                }
            });

            Preference presets = (Preference) findPreference("preset_apns");
            presets.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), com.klinker.android.messaging_sliding.mms.APNSettingsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                    return false;
                }

            });

            Preference getHelp = (Preference) findPreference("get_apn_help");
            getHelp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://www.google.com"));
                    startActivity(intent);
                    return false;
                }

            });
            
            if (getResources().getBoolean(R.bool.hasKitKat)) {
                try {
                    getPreferenceScreen().removePreference(findPreference("mmsThroughStock"));
                } catch (Exception e) {
                    
                }
            }

            if (!showAll) {
                try {
                    ((PreferenceGroup) findPreference("general_mms_category")).removePreference(findPreference("wifi_mms_fix"));
                } catch (Exception e) {

                }
            }
        }

        // used for the list preference to determine when it changes and when to call the intents
        SharedPreferences.OnSharedPreferenceChangeListener myPrefListner;

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(myPrefListner);
            inOtherLinks = false;
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(myPrefListner);

        }

        public void setUpSecuritySettings() {
            final Context context = getActivity();
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            // sets up the preferences dynamically depending on what security type you have
            if (sharedPrefs.getString("security_option", "none").equals("none")) {
                getPreferenceScreen().findPreference("set_password").setEnabled(false);
                getPreferenceScreen().findPreference("auto_unlock").setEnabled(false);
                getPreferenceScreen().findPreference("timeout_settings").setEnabled(false);
            } else if (sharedPrefs.getString("security_option", "none").equals("password")) {
                getPreferenceScreen().findPreference("set_password").setEnabled(true);
                getPreferenceScreen().findPreference("auto_unlock").setEnabled(false);
                getPreferenceScreen().findPreference("timeout_settings").setEnabled(true);
            } else if (sharedPrefs.getString("security_option", "none").equals("pin")) {
                getPreferenceScreen().findPreference("set_password").setEnabled(true);
                getPreferenceScreen().findPreference("auto_unlock").setEnabled(true);
                getPreferenceScreen().findPreference("timeout_settings").setEnabled(true);
            } else if (sharedPrefs.getString("security_option", "none").equals("pattern")) {
                getPreferenceScreen().findPreference("set_password").setEnabled(true);
                getPreferenceScreen().findPreference("auto_unlock").setEnabled(false);
                getPreferenceScreen().findPreference("timeout_settings").setEnabled(true);
            }

            // listner for list preference change to call intents and change preferences
            myPrefListner = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.equals("security_option")) {
                        //Get the value from the list_preference with default: "Nothing"
                        String value = sharedPrefs.getString(key, "none");

                        if (value.equals("none")) {
                            getPreferenceScreen().findPreference("set_password").setEnabled(false);
                            getPreferenceScreen().findPreference("auto_unlock").setEnabled(false);
                            getPreferenceScreen().findPreference("timeout_settings").setEnabled(false);
                        } else if (value.equals("pin")) {
                            getPreferenceScreen().findPreference("auto_unlock").setEnabled(true);
                            getPreferenceScreen().findPreference("set_password").setEnabled(true);
                            getPreferenceScreen().findPreference("timeout_settings").setEnabled(true);

                            Intent intent = new Intent(getActivity(), SetPinActivity.class);
                            startActivity(intent);
                        } else if (value.equals("password")) {
                            getPreferenceScreen().findPreference("auto_unlock").setEnabled(false);
                            getPreferenceScreen().findPreference("set_password").setEnabled(true);
                            getPreferenceScreen().findPreference("timeout_settings").setEnabled(true);

                            Intent intent = new Intent(getActivity(), SetPasswordActivity.class);
                            startActivity(intent);
                        } else if (value.equals("pattern")) // could be implemented for pattern, but it wasn't working before
                        {
                            getPreferenceScreen().findPreference("set_password").setEnabled(true);
                            getPreferenceScreen().findPreference("auto_unlock").setEnabled(false);
                            getPreferenceScreen().findPreference("timeout_settings").setEnabled(true);

                            SecurityPrefs.setAutoSavePattern(getActivity(), true);
                            Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                                    getActivity(), LockPatternActivity.class);
                            startActivityForResult(intent, REQ_CREATE_PATTERN);
                        }
                    }
                }
            };

            // calls the correct intent for clicking set password preference
            Preference setPassword = (Preference) findPreference("set_password");
            setPassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (sharedPrefs.getString("security_option", "none").equals("pin")) {
                        Intent intent = new Intent(getActivity(), SetPinActivity.class);
                        startActivity(intent);
                    } else if (sharedPrefs.getString("security_option", "none").equals("password")) {
                        Intent intent = new Intent(getActivity(), SetPasswordActivity.class);
                        startActivity(intent);
                    } else if (sharedPrefs.getString("security_option", "none").equals("pattern")) {
                        SecurityPrefs.setAutoSavePattern(context, true);
                        Intent intent = new Intent(LockPatternActivity.ACTION_CREATE_PATTERN, null,
                                getActivity(), LockPatternActivity.class);
                        startActivityForResult(intent, REQ_CREATE_PATTERN);
                    }
                    return false;
                }

            });
        }

        public void setUpSpeedSettings() {
            Preference scheduleBackup = (Preference) findPreference("schedule_backup");
            scheduleBackup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), com.klinker.android.messaging_sliding.backup.ScheduleBackup.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                    return false;
                }

            });

            Preference sdcard = (Preference) findPreference("sd_backup");
            sdcard.setEnabled(false);

            Preference runNow = (Preference) findPreference("run_backup");
            runNow.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), BackupService.class);
                    startService(intent);
                    return false;
                }

            });
        }

        public void setUpAdvancedSettings() {
            final Context context = getActivity();
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            Preference deleteOld = (Preference) findPreference("delete_old");
            deleteOld.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (sharedPrefs.getBoolean("delete_old", false)) {
                        Intent deleteIntent = new Intent(context, DeleteOldService.class);
                        PendingIntent pintent = PendingIntent.getService(context, 0, deleteIntent, 0);
                        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        alarm.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), 6 * 60 * 60 * 1000, pintent);
                    }

                    return false;
                }

            });

            Preference backup = (Preference) findPreference("backup");
            backup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.backup_settings_title))
                            .setMessage(context.getResources().getString(R.string.backup_settings_summary))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    File des = new File(Environment.getExternalStorageDirectory() + "/SlidingMessaging/backup.prefs");
                                    IOUtil.saveSharedPreferencesToFile(des, context);

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

            Preference restore = (Preference) findPreference("restore");
            restore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    File des = new File(Environment.getExternalStorageDirectory() + "/SlidingMessaging/backup.prefs");
                    IOUtil.loadSharedPreferencesFromFile(des, context);

                    Toast.makeText(context, context.getResources().getString(R.string.restore_success), Toast.LENGTH_LONG).show();

                    recreate();

                    return false;
                }

            });

            Preference smsToStore = (Preference) findPreference("sms_limit");
            smsToStore.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    if (sharedPrefs.getBoolean("delete_old", false)) {
                        NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
                                new NumberPickerDialog.OnNumberSetListener() {
                                    public void onNumberSet(int limit) {
                                        SharedPreferences.Editor editor = sharedPrefs.edit();

                                        editor.putInt("sms_limit", limit);
                                        editor.commit();
                                    }
                                };

                        new NumberPickerDialog(context, mSmsLimitListener, sharedPrefs.getInt("sms_limit", 500), 100, 1000, R.string.sms_limit).show();
                    }

                    return false;
                }

            });

            Preference deleteAll = findPreference("delete_all");
            deleteAll.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(context);
                    builder2.setMessage(context.getResources().getString(R.string.delete_all));
                    builder2.setPositiveButton(context.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @SuppressLint("SimpleDateFormat")
                        public void onClick(DialogInterface dialog, int id) {

                            final ProgressDialog progDialog = new ProgressDialog(context);
                            progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            progDialog.setMessage(context.getResources().getString(R.string.deleting));
                            progDialog.show();

                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    deleteSMS(context);
                                    ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content).post(new Runnable() {

                                        @Override
                                        public void run() {
                                            progDialog.dismiss();
                                        }

                                    });
                                }

                            }).start();

                        }
                    });
                    builder2.setNegativeButton(context.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog2 = builder2.create();

                    dialog2.show();

                    return true;
                }

            });

//            Preference numCacheConversations = findPreference("num_cache_conversations");
//            numCacheConversations.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
//
//                @Override
//                public boolean onPreferenceClick(Preference arg0) {
//                    if (sharedPrefs.getBoolean("cache_conversations", false))
//                    {
//                        NumberPickerDialog.OnNumberSetListener numCacheListener =
//                                new NumberPickerDialog.OnNumberSetListener() {
//                                    public void onNumberSet(int limit) {
//                                        SharedPreferences.Editor editor = sharedPrefs.edit();
//
//                                        editor.putInt("num_cache_conversations", limit);
//                                        editor.commit();
//
//                                        if (sharedPrefs.getBoolean("cache_conversations", false)) {
//                                            Intent cacheService = new Intent(context, CacheService.class);
//                                            context.startService(cacheService);
//                                        }
//                                    }
//                                };
//
//                        new NumberPickerDialog(context, numCacheListener, sharedPrefs.getInt("num_cache_conversations", 5), 0, 15, R.string.num_cache_conversations).show();
//                    }
//
//                    return false;
//                }
//
//            });

            if (getResources().getBoolean(R.bool.hasKitKat)) {
                ((PreferenceGroup) findPreference("advanced_other_category")).removePreference(findPreference("override"));
            }

            if (!showAll) {
                ((PreferenceGroup) findPreference("advanced_theme_category")).removePreference(findPreference("strip_unicode"));
                ((PreferenceGroup) findPreference("advanced_theme_category")).removePreference(findPreference("mobile_only"));
                ((PreferenceGroup) findPreference("advanced_theme_category")).removePreference(findPreference("hide_keyboard"));
                ((PreferenceGroup) findPreference("advanced_theme_category")).removePreference(findPreference("override_lang"));
                ((PreferenceGroup) findPreference("advanced_theme_category")).removePreference(findPreference("keyboard_type"));
                ((PreferenceGroup) findPreference("advanced_theme_category")).removePreference(findPreference("send_with_return"));
                ((PreferenceGroup) findPreference("advanced_other_category")).removePreference(findPreference("save_to_external"));

//                getPreferenceScreen().removePreference(findPreference("cache_conversations"));
//                getPreferenceScreen().removePreference(findPreference("num_cache_conversations"));
            }

            findPreference("show_all_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    recreate();
                    return true;
                }
            });
        }

        public void giveTestNotification() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.test_notification);
            builder.setMessage(R.string.test_notification_summary);
            final AlertDialog dialog = builder.create();
            dialog.show();

            BroadcastReceiver screenOff = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, Intent intent) {
                    final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getActivity())
                                    .setSmallIcon(R.drawable.stat_notify_sms)
                                    .setContentTitle("Test")
                                    .setContentText("Test Notification")
                                    .setTicker("Test: Test Notification");

                    setIcon(mBuilder);

                    if (sharedPrefs.getBoolean("vibrate", true)) {
                        if (!sharedPrefs.getBoolean("custom_vibrate_pattern", false)) {
                            String vibPat = sharedPrefs.getString("vibrate_pattern", "2short");

                            if (vibPat.equals("short")) {
                                long[] pattern = {0L, 400L};
                                mBuilder.setVibrate(pattern);
                            } else if (vibPat.equals("long")) {
                                long[] pattern = {0L, 800L};
                                mBuilder.setVibrate(pattern);
                            } else if (vibPat.equals("2short")) {
                                long[] pattern = {0L, 400L, 100L, 400L};
                                mBuilder.setVibrate(pattern);
                            } else if (vibPat.equals("2long")) {
                                long[] pattern = {0L, 800L, 200L, 800L};
                                mBuilder.setVibrate(pattern);
                            } else if (vibPat.equals("3short")) {
                                long[] pattern = {0L, 400L, 100L, 400L, 100L, 400L};
                                mBuilder.setVibrate(pattern);
                            } else if (vibPat.equals("3long")) {
                                long[] pattern = {0L, 800L, 200L, 800L, 200L, 800L};
                                mBuilder.setVibrate(pattern);
                            }
                        } else {
                            try {
                                String[] vibPat = sharedPrefs.getString("set_custom_vibrate_pattern", "0, 400, 100, 400").replace("L", "").split(", ");
                                long[] pattern = new long[vibPat.length];

                                for (int i = 0; i < vibPat.length; i++) {
                                    pattern[i] = Long.parseLong(vibPat[i]);
                                }

                                mBuilder.setVibrate(pattern);
                            } catch (Exception e) {

                            }
                        }
                    }

                    if (sharedPrefs.getBoolean("led", true)) {
                        String ledColor = sharedPrefs.getString("led_color", "white");
                        int ledOn = sharedPrefs.getInt("led_on_time", 1000);
                        int ledOff = sharedPrefs.getInt("led_off_time", 2000);

                        if (ledColor.equalsIgnoreCase("white")) {
                            mBuilder.setLights(0xFFFFFFFF, ledOn, ledOff);
                        } else if (ledColor.equalsIgnoreCase("blue")) {
                            mBuilder.setLights(0xFF0099CC, ledOn, ledOff);
                        } else if (ledColor.equalsIgnoreCase("green")) {
                            mBuilder.setLights(0xFF00FF00, ledOn, ledOff);
                        } else if (ledColor.equalsIgnoreCase("orange")) {
                            mBuilder.setLights(0xFFFF8800, ledOn, ledOff);
                        } else if (ledColor.equalsIgnoreCase("red")) {
                            mBuilder.setLights(0xFFCC0000, ledOn, ledOff);
                        } else if (ledColor.equalsIgnoreCase("purple")) {
                            mBuilder.setLights(0xFFAA66CC, ledOn, ledOff);
                        } else {
                            mBuilder.setLights(0xFFFFFFFF, ledOn, ledOff);
                        }
                    }

                    try {
                        mBuilder.setSound(Uri.parse(sharedPrefs.getString("ringtone", "null")));
                    } catch (Exception e) {
                        mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    }

                    NotificationManager mNotificationManager =
                            (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

                    Notification notification = new NotificationCompat.BigTextStyle(mBuilder).bigText("Test Notification").build();
                    Intent deleteIntent = new Intent(getActivity(), NotificationReceiver.class);
                    notification.deleteIntent = PendingIntent.getBroadcast(getActivity(), 0, deleteIntent, 0);
                    mNotificationManager.notify(1, notification);

                    context.unregisterReceiver(this);
                    dialog.dismiss();
                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);

            getActivity().registerReceiver(screenOff, filter);
        }

        public void setIcon(NotificationCompat.Builder mBuilder) {
            final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            if (!sharedPrefs.getBoolean("breath", false)) {
                String notIcon = sharedPrefs.getString("notification_icon", "white");
                int notImage = Integer.parseInt(sharedPrefs.getString("notification_image", "1"));

                switch (notImage) {
                    case 1:
                        if (notIcon.equals("white")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms);
                        } else if (notIcon.equals("blue")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_blue);
                        } else if (notIcon.equals("green")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_green);
                        } else if (notIcon.equals("orange")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_orange);
                        } else if (notIcon.equals("purple")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_purple);
                        } else if (notIcon.equals("red")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_red);
                        } else if (notIcon.equals("icon")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_icon);
                        }

                        break;
                    case 2:
                        if (notIcon.equals("white")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_bubble);
                        } else if (notIcon.equals("blue")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_bubble_blue);
                        } else if (notIcon.equals("green")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_bubble_green);
                        } else if (notIcon.equals("orange")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_bubble_orange);
                        } else if (notIcon.equals("purple")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_bubble_purple);
                        } else if (notIcon.equals("red")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_bubble_red);
                        } else if (notIcon.equals("icon")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_icon);
                        }

                        break;
                    case 3:
                        if (notIcon.equals("white")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_point);
                        } else if (notIcon.equals("blue")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_point_blue);
                        } else if (notIcon.equals("green")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_point_green);
                        } else if (notIcon.equals("orange")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_point_orange);
                        } else if (notIcon.equals("purple")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_point_purple);
                        } else if (notIcon.equals("red")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_point_red);
                        } else if (notIcon.equals("icon")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_icon);
                        }

                        break;
                    case 4:
                        if (notIcon.equals("white")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_airplane);
                        } else if (notIcon.equals("blue")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_airplane_blue);
                        } else if (notIcon.equals("green")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_airplane_green);
                        } else if (notIcon.equals("orange")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_airplane_orange);
                        } else if (notIcon.equals("purple")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_airplane_purple);
                        } else if (notIcon.equals("red")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_airplane_red);
                        } else if (notIcon.equals("icon")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_icon);
                        }

                        break;
                    case 5:
                        if (notIcon.equals("white")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_cloud);
                        } else if (notIcon.equals("blue")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_cloud_blue);
                        } else if (notIcon.equals("green")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_cloud_green);
                        } else if (notIcon.equals("orange")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_cloud_orange);
                        } else if (notIcon.equals("purple")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_cloud_purple);
                        } else if (notIcon.equals("red")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_cloud_red);
                        } else if (notIcon.equals("icon")) {
                            mBuilder.setSmallIcon(R.drawable.stat_notify_sms_icon);
                        }
                        break;
                }
            } else {
                mBuilder.setSmallIcon(R.drawable.stat_notify_sms_breath);
            }
        }

        public void deleteSMS(Context context) {
            ArrayList<String> threadIds = new ArrayList<String>();
            String[] projection = new String[]{"_id"};
            Uri uri = Uri.parse("content://mms-sms/conversations/?simple=true");
            Cursor query = context.getContentResolver().query(uri, projection, null, null, null);

            if (query.moveToFirst()) {
                do {
                    threadIds.add(query.getString(query.getColumnIndex("_id")));
                } while (query.moveToNext());
            }

            try {
                for (int i = 0; i < threadIds.size(); i++) {
                    deleteThread(context, threadIds.get(i));
                }
            } catch (Exception e) {
            }
        }

        public Boolean deleteLocked = null;
        public boolean showingDialog = false;

        public void deleteThread(final Context context, final String id) {
            if (checkLocked(context, id)) {
                while (showingDialog) {
                    try {
                        Thread.sleep(250);
                    } catch (Exception e) {

                    }
                }

                if (deleteLocked == null) {
                    showingDialog = true;
                    ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content).post(new Runnable() {

                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.locked_messages)
                                    .setMessage(R.string.locked_messages_summary)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    deleteLocked = true;
                                                    showingDialog = false;
                                                    deleteLocked(context, id);
                                                }
                                            }).start();
                                        }
                                    })
                                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    deleteLocked = false;
                                                    showingDialog = false;
                                                    dontDeleteLocked(context, id);
                                                }
                                            }).start();
                                        }
                                    })
                                    .create()
                                    .show();
                        }

                    });
                } else {
                    if (deleteLocked) {
                        deleteLocked(context, id);
                    } else {
                        dontDeleteLocked(context, id);
                    }
                }
            } else {
                deleteLocked(context, id);
            }
        }

        public boolean checkLocked(Context context, String id) {
            try {
                return context.getContentResolver().query(Uri.parse("content://mms-sms/locked/" + id + "/"), new String[]{"_id"}, null, null, null).moveToFirst();
            } catch (Exception e) {
                return false;
            }
        }

        public void deleteLocked(Context context, String id) {
            context.getContentResolver().delete(Uri.parse("content://mms-sms/conversations/" + id + "/"), null, null);
            context.getContentResolver().delete(Uri.parse("content://mms-sms/conversations/"), "_id=?", new String[]{id});
        }

        public void dontDeleteLocked(Context context, String id) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            ops.add(ContentProviderOperation.newDelete(Uri.parse("content://mms-sms/conversations/" + id + "/"))
                    .withSelection("locked=?", new String[]{"0"})
                    .build());
            try {
                context.getContentResolver().applyBatch("mms-sms", ops);
            } catch (RemoteException e) {
            } catch (OperationApplicationException e) {
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
            super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

            if (requestCode == 1) {
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = sharedPrefs.edit();

                    editor.putString("custom_background_location", filePath);
                    editor.commit();

                }
            } else if (requestCode == 2) {
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();

                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = sharedPrefs.edit();

                    editor.putString("custom_background2_location", filePath);
                    editor.commit();

                }
            } else if (requestCode == REQ_CREATE_PATTERN) {
                if (resultCode == RESULT_OK) {

                } else {
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString("security_option", "none");
                    editor.commit();
                }
            }
        }

    }

    public static double tabletSize(Context context) {
        double size = 0;

        try {
            DisplayMetrics dm = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
            double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
            double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
            double screenInches = Math.sqrt(x + y);
            return screenInches;
        } catch (Throwable t) {

        }

        return size;
    }

    public static boolean xLargeScreen(Context context) {
        Configuration config = context.getResources().getConfiguration();
        if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return true;
        }

        return false;
    }
}