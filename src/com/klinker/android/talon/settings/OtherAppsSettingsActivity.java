package com.klinker.android.talon.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.klinker.android.messaging_donate.MainActivity;
import com.klinker.android.messaging_donate.R;
import com.klinker.android.messaging_sliding.scheduled.ScheduledSms;
import com.klinker.android.messaging_sliding.templates.TemplateActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class OtherAppsSettingsActivity extends PreferenceActivity {

    public static Context context;

    private String[] linkItems;
    private String[] otherItems;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private LinearLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    private Activity activity;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.other_apps);
        setContentView(R.layout.preference_drawers_layout);
        setTitle(R.string.other_apps);

        context = this;

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (sharedPrefs.getBoolean("override_lang", false)) {
            String languageToLoad = "en";
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        } else {
            String languageToLoad = Resources.getSystem().getConfiguration().locale.getLanguage();
            Locale locale = new Locale(languageToLoad);
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }

        Preference emojiKeyboard = findPreference("emoji_keyboard");
        emojiKeyboard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard_trial&hl=en")));
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                return false;
            }

        });

        Preference emojiUnlock = findPreference("emoji_unlock");
        emojiUnlock.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.emoji_keyboard&hl=en")));
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                return false;
            }

        });

        Preference haloPop = findPreference("halo_pop");
        haloPop.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.halopop&hl=en")));
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                return false;
            }

        });

        Preference themeEngine = findPreference("theme_engine");
        themeEngine.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.klinker.android.messaging_theme&hl=en")));
                overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                return false;
            }

        });

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

        DrawerArrayAdapter.current = 3;
        SettingsPagerActivity.settingsLinksActive = false;
        SettingsPagerActivity.inOtherLinks = true;

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

        spinner.setSelection(1);

        spinner.setOnItemSelectedListener(new SpinnerClickListener());

        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerArrayAdapter(this,
                new ArrayList<String>(Arrays.asList(otherItems))));
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

        activity = this;
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
                SettingsPagerActivity.settingsLinksActive = true;
            } else {
                mDrawerList.setAdapter(new DrawerArrayAdapter(activity,
                        new ArrayList<String>(Arrays.asList(otherItems))));
                mDrawerList.invalidate();
                SettingsPagerActivity.settingsLinksActive = false;
            }


        }

        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {

            // TODO: Make this smoother
            // TODO: Add the other settings options for not switching viewpager
            final Context context = getApplicationContext();
            Intent intent;

            final int mPos = position;

            if (SettingsPagerActivity.settingsLinksActive) {
                mDrawerLayout.closeDrawer(mDrawer);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //onBackPressed();

                        Intent mIntent = new Intent(context, SettingsPagerActivity.class);
                        mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        mIntent.putExtra("page_number", mPos);
                        startActivity(mIntent);
                        overridePendingTransition(0, 0);
                    }
                }, 200);
            } else {
                mDrawerLayout.closeDrawer(mDrawer);

                switch (position) {
                    case 0:
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                onBackPressed();

                                Intent mIntent = new Intent(context, TemplateActivity.class);
                                mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                                overridePendingTransition(0, 0);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
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
                                overridePendingTransition(0, 0);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
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
                                overridePendingTransition(0, 0);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                            }
                        }, 200);

                        break;

                    case 3:
                        /*new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent mIntent = new Intent(context, OtherAppsSettingsActivity.class);
                                mIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(mIntent);
                                overridePendingTransition(0,0);
                                //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                            }
                        }, 100);*/

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

                        //overridePendingTransition(R.anim.activity_slide_in_right, R.anim.activity_slide_out_left);
                        //mDrawerLayout.closeDrawer(mDrawer);
                        break;
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Pass the event to ActionBarDrawerToggle, if it returns
                // true, then it has handled the app icon touch event
                if (mDrawerToggle.onOptionsItemSelected(item)) {

                    return true;
                }

                // Handle your other action bar items...

                return super.onOptionsItemSelected(item);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
        overridePendingTransition(R.anim.activity_slide_in_left, R.anim.activity_slide_out_right);
    }
}
