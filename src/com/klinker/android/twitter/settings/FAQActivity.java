package com.klinker.android.twitter.settings;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.FAQArrayAdapter;
import com.klinker.android.twitter.utils.Utils;

import java.util.ArrayList;

public class FAQActivity extends Activity {

    private ArrayList<String[]> faq = new ArrayList<String[]>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpFAQ();

        AppSettings settings = new AppSettings(this);

        Utils.setUpPopupTheme(this, settings);
        setUpWindow();

        setContentView(R.layout.faq_activity);

        ListView faqs = (ListView) findViewById(R.id.listView);
        faqs.setAdapter(new FAQArrayAdapter(this, faq));
    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window

        getWindow().setAttributes(params);

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

    }

    public void setUpFAQ() {
        faq.add(new String[] {
                "Push Notifications",
                "https://plus.google.com/117432358268488452276/posts/31oSKEmMFnq"
        });
        faq.add(new String[] {
                "Translucency",
                "https://plus.google.com/117432358268488452276/posts/Kc2sB8uBYwa"
        });
        faq.add(new String[] {
                "Theming Limits",
                "https://plus.google.com/117432358268488452276/posts/dHDRSc4J3yV"
        });
        faq.add(new String[] {
                "More Info on Status's",
                "https://plus.google.com/117432358268488452276/posts/hY7Aa3eSVvC"
        });
        faq.add(new String[] {
                "Clearing Cache",
                "https://plus.google.com/117432358268488452276/posts/ZgAHJxKycfv"
        });

    }
}
