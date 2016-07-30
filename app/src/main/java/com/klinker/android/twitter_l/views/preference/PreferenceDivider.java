package com.klinker.android.twitter_l.views.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.klinker.android.twitter_l.R;

/**
 * Created by luke on 6/10/16.
 */

public class PreferenceDivider extends Preference {

    public PreferenceDivider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PreferenceDivider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_divider);
    }
}
