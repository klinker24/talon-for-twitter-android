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

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.widget.Toast;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ChangelogAdapter;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.XmlChangelogUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_zoom_enter, R.anim.slide_out_right);
    }

    public static boolean useAnim = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION & WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (SettingsActivity.useAnim) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);
        }

        SettingsActivity.useAnim = true;

        AppSettings.invalidate();

        setUpTheme();

        setContentView(R.layout.settings_base);

        AppSettings settings = AppSettings.getInstance(this);

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
        ab.setIcon(null);
        ab.setBackgroundDrawable(new ColorDrawable(settings.themeColors.primaryColor));

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content, new MainPrefFrag())
                .commit();

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            getWindow().setNavigationBarColor(Color.BLACK);
        }
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
                showSettings(8, getString(R.string.get_help_settings));
                return true;
            case R.id.menu_other_apps:
                showSettings(9, getString(R.string.other_apps));
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v("talon_purchase", "coming back from activity result");
        if (requestCode == 1001) {
            Log.v("talon_purchase", "it was a purchase");

            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == Activity.RESULT_OK) {
                Log.v("talon_purchase", "result was ok");

                try {
                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    alert("Your support is greatly appreciated. Users like you are the reason I love my job :)");

                    SharedPreferences sharedPreferences = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                            Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
                    sharedPreferences.edit().putBoolean("2016_supporter", true).commit();

                    recreate();
                } catch (JSONException e) {
                    alert("Uh oh... Something went wrong with the purchase: Failed to parse purchase data.");
                    e.printStackTrace();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void alert(String alert) {
        Toast.makeText(this, alert, Toast.LENGTH_LONG)
                .show();
    }
}