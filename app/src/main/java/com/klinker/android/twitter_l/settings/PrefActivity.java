package com.klinker.android.twitter_l.settings;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.Utils;

public class PrefActivity extends AppCompatActivity {

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onBackPressed() {
        AppSettings.invalidate();
        super.onBackPressed();
    }

    public PrefFragment getFragment() {
        return new PrefFragment();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION & WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        setUpTheme();

        setContentView(R.layout.settings_base);

        final PrefFragment fragment = getFragment();

        Bundle args = new Bundle();
        args.putInt("position", getIntent().getIntExtra("position", 0));
        fragment.setArguments(args);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content, fragment)
                .commit();

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
        ab.setTitle(getIntent().getStringExtra("title"));
        ab.setIcon(null);
        ab.setBackgroundDrawable(new ColorDrawable(AppSettings.getInstance(this).themeColors.primaryColor));

        //setIcon(ab, getIntent().getIntExtra("position", 0));

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && AppSettings.getInstance(this).darkTheme) {
            View status = findViewById(R.id.settings_status);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) status.getLayoutParams();
            params.height = Utils.getActionBarHeight(this) + Utils.getStatusBarHeight(this);

            status.setLayoutParams(params);

            View nav = findViewById(R.id.settings_nav);
            params = (LinearLayout.LayoutParams) nav.getLayoutParams();
            params.height = Utils.hasNavBar(this) ? Utils.getNavBarHeight(this) : 0;

            nav.setLayoutParams(params);
        }
    }

    public void setUpTheme() {

        AppSettings settings = AppSettings.getInstance(this);

        Utils.setUpSettingsTheme(this, settings);

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        getWindow().getDecorView().setBackgroundResource(resource);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(Color.BLACK);
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }
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

    public void setIcon(android.support.v7.app.ActionBar ab, int position) {
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
        } else if (position == 8) {
            ab.setIcon(R.drawable.drawer_help_dark);
        } else if (position == 9) {
            ab.setIcon(R.drawable.drawer_other_apps_dark);
        } else if (position == 7) {
            ab.setIcon(R.drawable.ic_action_sd_storage_dark);
        } else if (position == 4) {
            ab.setIcon(R.drawable.ic_links_dark);
        } else if (position == 10) {
            ab.setIcon(R.drawable.ic_action_place_dark);
        } else if (position == 6) {
            ab.setIcon(R.drawable.ic_settings_pages_dark);
        }
    }
}
