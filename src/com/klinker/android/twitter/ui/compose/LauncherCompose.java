package com.klinker.android.twitter.ui.compose;

import android.view.View;

public class LauncherCompose extends ComposeActivity {

    @Override
    public void setUpLayout() {
        super.setUpLayout();
        attachButton.setVisibility(View.GONE);
    }
}
