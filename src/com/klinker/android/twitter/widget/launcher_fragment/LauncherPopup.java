package com.klinker.android.twitter.widget.launcher_fragment;

import android.util.Log;
import android.view.WindowManager;

import com.klinker.android.twitter.ui.MainActivityPopup;


public class LauncherPopup extends MainActivityPopup {

    @Override
    public void setDim() {
        Log.v("talon_launcher", "setting dim");
        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window

        getWindow().setAttributes(params);
    }

    @Override
    public void setLauncherPage() {
        mViewPager.setCurrentItem(getIntent().getIntExtra("launcher_page", 0));
    }
}
