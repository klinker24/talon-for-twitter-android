package com.klinker.android.twitter_l.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class ThemeColor {

    public int primaryColor;
    public int primaryColorDark;
    public int primaryColorLight;
    public int accentColor;
    public int accentColorLight;

    public ThemeColor(String prefix, Context context, boolean realAccent) {
        ResourceHelper helper = new ResourceHelper(context, Utils.PACKAGE_NAME);
        primaryColor = helper.getColor(prefix + "_primary_color");
        primaryColorDark = helper.getColor(prefix + "_primary_color_dark");
        primaryColorLight = helper.getColor(prefix + "_primary_color_light");
        accentColor = helper.getColor(prefix + "_accent_color");
        accentColorLight = helper.getColor(prefix + "_accent_color_light");

        if (!realAccent) {
            SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


            int currentAccount = sharedPrefs.getInt("current_account", 1);
            int accent = sharedPrefs.getInt("material_accent_" + currentAccount, -1);
            int accentLight = sharedPrefs.getInt("material_accent_light_" + currentAccount, -1);
            if (accent != -1) {
                accentColor = accent;
                accentColorLight = accentLight;
            }
        }
    }

    public ThemeColor(String prefix, Context context) {
        ResourceHelper helper = new ResourceHelper(context, Utils.PACKAGE_NAME);
        primaryColor = helper.getColor(prefix + "_primary_color");
        primaryColorDark = helper.getColor(prefix + "_primary_color_dark");
        primaryColorLight = helper.getColor(prefix + "_primary_color_light");
        accentColor = helper.getColor(prefix + "_accent_color");
        accentColorLight = helper.getColor(prefix + "_accent_color_light");

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


        int currentAccount = sharedPrefs.getInt("current_account", 1);
        int accent = sharedPrefs.getInt("material_accent_" + currentAccount, -1);
        int accentLight = sharedPrefs.getInt("material_accent_light_" + currentAccount, -1);
        if (accent != -1) {
            accentColor = accent;
            accentColorLight = accentLight;
        }
    }
}
