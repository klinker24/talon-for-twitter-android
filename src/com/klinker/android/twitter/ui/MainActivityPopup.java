package com.klinker.android.twitter.ui;

import android.graphics.Point;
import android.view.Display;
import android.view.Window;

import com.klinker.android.twitter.utils.Utils;

public class MainActivityPopup extends MainActivity {

    @Override
    public void setUpWindow() {
        try {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {
            recreate();
        }

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

        MainActivity.isPopup = true;
    }

    @Override
    public void setUpTheme() {

        translucent = false;

        Utils.setUpNotifTheme(context, settings);
    }

    @Override
    public void onPause() {
        sharedPrefs.edit().putBoolean("refresh_me", true).commit();
        super.onPause();
    }

    @Override
    public void onStop() {
        sharedPrefs.edit().putBoolean("remake_me", true).commit();

        super.onStop();
    }


}
