package com.klinker.android.twitter.settings;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.Spanned;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.widget.Toast;
import com.klinker.android.launcher.api.BaseLauncherPage;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ChangelogAdapter;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter.utils.XmlChangelogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivityNew extends PreferenceActivity {

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_zoom_enter, R.anim.slide_out_right);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);

        AppSettings.invalidate();

        setUpTheme();

        addPreferencesFromResource(R.xml.main_settings);

        setClicks();

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        ListView list = (ListView) findViewById(android.R.id.list);
        list.setDivider(new ColorDrawable(getResources().getColor(android.R.color.transparent))); // or some other color int
        list.setDividerHeight(0);


        /*HoloTextView createdBy = (HoloTextView) findViewById(R.id.created_by);
        HoloTextView versionNumber = (HoloTextView) findViewById(R.id.version_number);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;

            String text = getResources().getString(R.string.created_by) + " Luke Klinker";
            String text2 = getResources().getString(R.string.version) + " " + versionName;
            createdBy.setText(text);
            versionNumber.setText(text2);
        } catch (Exception e) {
            String text = getResources().getString(R.string.created_by) + " Luke Klinker";
            String text2 = getResources().getString(R.string.version) + " 0.00";
            createdBy.setText(text);
            versionNumber.setText(text2);
        }

        LinearLayout description = (LinearLayout) findViewById(R.id.created_by_layout);
        description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Klinker+Apps")));
            }
        });*/
    }

    public void setUpTheme() {

        AppSettings settings = AppSettings.getInstance(this);

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack);
                break;
        }

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        getWindow().getDecorView().setBackgroundResource(resource);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_settings, menu);

        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_whats_new:
                final Context context = this;
                final ListView list = new ListView(this);
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

                new AlertDialog.Builder(this)
                        .setTitle(R.string.changelog)
                        .setView(list)
                        .setPositiveButton(R.string.ok, null)
                        .show();
                return true;
            case R.id.menu_rate_it:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Couldn't launch the market", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_get_help:
                showSettings(7, getString(R.string.get_help_settings));
                return true;
            case R.id.menu_other_apps:
                showSettings(8, getString(R.string.other_apps));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        AppSettings.invalidate();
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        finish();
    }

    public void setClicks() {

        findPreference("ui_settings").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
        });
    }

    private void showSettings(int position, String title) {
        startActivity(new Intent(this, PrefActivity.class)
                .putExtra("position", position)
                .putExtra("title", title));
    }
}