package com.klinker.android.twitter.utils.redirects;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.klinker.android.twitter.ui.MainActivityPopup;
import com.klinker.android.twitter.widget.launcher_fragment.LauncherPopup;

/**
 * Created by luke on 4/24/14.
 */
public class RedirectToLauncherPopup extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent popup = new Intent(this, LauncherPopup.class);
        popup.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        popup.putExtra("launcher_page", getIntent().getIntExtra("launcher_page", 0));
        finish();

        startActivity(popup);
    }
}
