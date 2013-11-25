package com.klinker.android.talon.ui;

import android.content.Intent;
import android.graphics.Point;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;

public class MainActivityPopup extends MainActivity {

    @Override
    public void setUpWindow() {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);

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

        isPopup = true;
    }

    @Override
    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight_Popup);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark_Popup);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack_Popup);
                break;
        }
    }
}
