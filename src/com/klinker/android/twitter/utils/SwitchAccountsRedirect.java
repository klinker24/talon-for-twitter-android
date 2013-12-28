package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.ui.MainActivity;

public class SwitchAccountsRedirect extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        int currentAccount = sharedPrefs.getInt("current_account", 1);

        if (currentAccount == 1) {
            sharedPrefs.edit().putInt("current_account", 2).commit();
        } else {
            sharedPrefs.edit().putInt("current_account", 1).commit();
        }

        Intent main = new Intent(this, MainActivity.class);
        main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        main.putExtra("switch_account", true);
        overridePendingTransition(0, 0);
        finish();
        startActivity(main);
    }
}
