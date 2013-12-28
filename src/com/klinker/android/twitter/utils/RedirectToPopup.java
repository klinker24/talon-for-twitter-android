package com.klinker.android.twitter.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.klinker.android.twitter.ui.MainActivityPopup;

public class RedirectToPopup extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent popup = new Intent(this, MainActivityPopup.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();

        startActivity(popup);
    }
}
