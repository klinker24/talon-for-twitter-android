package com.klinker.android.twitter_l.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.klinker.android.peekview.PeekViewActivity;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;

/**
 * A helper class to facilitate the usage of very light colored Toolbars, where the text will need to be
 * changed to dark.
 */
@SuppressLint("Registered")
public class WhiteToolbarActivity extends PeekViewActivity {

    protected boolean neverUseLightStatusBar = false;
    protected int lightStatusBarIconColor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.lightStatusBarIconColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                getResources().getColor(R.color.light_status_bar_color) : Color.BLACK;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AppSettings.getInstance(this).blackTheme) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar == null) {
            toolbar = (Toolbar) findViewById(R.id.dragdismiss_toolbar);
        }

        if (AppSettings.isWhiteToolbar(this)) {
            activateLightStatusBar(true);
        } else {
            lightStatusBarIconColor = Color.WHITE;
        }

        if (toolbar != null) {
            toolbar.setTitleTextColor(lightStatusBarIconColor);

            if (toolbar.getOverflowIcon() != null) {
                toolbar.getOverflowIcon().setColorFilter(lightStatusBarIconColor, PorterDuff.Mode.MULTIPLY);
            }

            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setColorFilter(lightStatusBarIconColor, PorterDuff.Mode.MULTIPLY);
            }
        }

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getIcon() != null) {
                menu.getItem(i).getIcon().setColorFilter(lightStatusBarIconColor, PorterDuff.Mode.MULTIPLY);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Sets the status bar to be light or not. Light status bar means dark icons.
     * @param lightStatusBar if true, make sure the status bar is light
     */
    public void activateLightStatusBar(boolean lightStatusBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !AppSettings.isWhiteToolbar(this) || neverUseLightStatusBar) {
            return;
        }

        int oldSystemUiFlags = getWindow().getDecorView().getSystemUiVisibility();
        int newSystemUiFlags = oldSystemUiFlags;
        if (lightStatusBar) {
            newSystemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            newSystemUiFlags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (newSystemUiFlags != oldSystemUiFlags) {
            getWindow().getDecorView().setSystemUiVisibility(newSystemUiFlags);
        }
    }
}
