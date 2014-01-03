package com.klinker.android.twitter.ui.compose;

import android.os.Bundle;

/**
 * Created by luke on 1/2/14.
 */
public class WidgetCompose extends ComposeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reply.setText("");
    }
}
