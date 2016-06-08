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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.klinker.android.twitter_l.R;


public class DashClockSettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupSimplePreferencesScreen() {
        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.dashclock_settings);

        findViewById(android.R.id.content).setBackgroundColor(getResources().getColor(R.color.light_background));
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return false;
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
        SharedPreferences worldPrefs = AppSettings.getSharedPreferences(this);


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
    }
}