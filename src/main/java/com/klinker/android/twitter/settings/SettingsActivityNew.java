package com.klinker.android.twitter.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.launcher.api.BaseLauncherPage;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.manipulations.widgets.HoloTextView;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
}