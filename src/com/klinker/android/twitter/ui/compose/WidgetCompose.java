package com.klinker.android.twitter.ui.compose;

import android.os.Bundle;
import android.view.View;

public class WidgetCompose extends ComposeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reply.setText("");
    }

    @Override
    public void setUpLayout() {
        super.setUpLayout();
        attachButton.setVisibility(View.GONE);
    }
}
