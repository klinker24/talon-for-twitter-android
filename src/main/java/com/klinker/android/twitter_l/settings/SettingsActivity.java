package com.klinker.android.twitter_l.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Spanned;
import android.view.*;
import android.widget.ListView;

import android.widget.Toast;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ChangelogAdapter;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.XmlChangelogUtils;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_zoom_enter, R.anim.slide_out_right);
    }

    public static boolean useAnim = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SettingsActivity.useAnim) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);
        }

        SettingsActivity.useAnim = true;

        AppSettings.invalidate();

        setUpTheme();

        AppSettings settings = AppSettings.getInstance(this);

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
        ab.setIcon(null);
        ab.setBackgroundDrawable(new ColorDrawable(settings.themeColors.primaryColor));

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MainPrefFrag())
                .commit();

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

    public boolean refresh = false;
    @Override
    public void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (refresh) {
                    refresh = false;
                    recreate();
                }
            }
        }, 300);
    }

    public void setUpTheme() {

        AppSettings settings = AppSettings.getInstance(this);

        Utils.setUpSettingsTheme(this, settings);

        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        getWindow().getDecorView().setBackgroundResource(resource);

        getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        getWindow().setNavigationBarColor(settings.themeColors.primaryColorDark);
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

    private void showSettings(int position, String title) {
        startActivity(new Intent(this, PrefActivity.class)
                .putExtra("position", position)
                .putExtra("title", title));
    }
}