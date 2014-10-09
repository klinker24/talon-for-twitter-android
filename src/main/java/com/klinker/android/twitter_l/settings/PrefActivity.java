package com.klinker.android.twitter_l.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.utils.Utils;

public class PrefActivity extends PreferenceActivity {

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }

    @Override
    public void onBackPressed() {
        AppSettings.invalidate();
        Intent main = new Intent(this, SettingsActivity.class);
        SettingsActivity.useAnim = false;
        startActivity(main);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpTheme();

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);

        final PrefFragment fragment = new PrefFragment();
        Bundle args = new Bundle();
        args.putInt("position", getIntent().getIntExtra("position", 0));
        fragment.setArguments(args);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
        ab.setTitle(getIntent().getStringExtra("title"));
        ab.setIcon(null);
        ab.setBackgroundDrawable(new ColorDrawable(AppSettings.getInstance(this).themeColors.primaryColor));

        setIcon(ab, getIntent().getIntExtra("position", 0));
    }

    public void setUpTheme() {

        AppSettings settings = AppSettings.getInstance(this);

        Utils.setUpSettingsTheme(this, settings);

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        getWindow().getDecorView().setBackgroundResource(resource);

        getWindow().setNavigationBarColor(settings.themeColors.primaryColorDark);
        getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setIcon(ActionBar ab, int position) {
        if (position == 0) {
            ab.setIcon(R.drawable.drawer_theme_dark);
        } else if (position == 1) {
            ab.setIcon(R.drawable.drawer_user_dark);
        } else if (position == 2) {
            ab.setIcon(R.drawable.drawer_sync_dark);
        } else if (position == 3) {
            ab.setIcon(R.drawable.drawer_notifications_dark);
        } else if (position == 5) {
            ab.setIcon(R.drawable.advanced_settings_dark);
        } else if (position == 7) {
            ab.setIcon(R.drawable.drawer_help_dark);
        } else if (position == 8) {
            ab.setIcon(R.drawable.drawer_other_apps_dark);
        } else if (position == 6) {
            ab.setIcon(R.drawable.ic_action_sd_storage_dark);
        } else if (position == 4) {
            ab.setIcon(R.drawable.ic_links_dark);
        } else if (position == 9) {
            ab.setIcon(R.drawable.ic_action_place_dark);
        }
    }
}
