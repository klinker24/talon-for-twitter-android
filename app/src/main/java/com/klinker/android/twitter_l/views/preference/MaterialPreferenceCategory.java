package com.klinker.android.twitter_l.views.preference;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;

import com.klinker.android.twitter_l.R;

public class MaterialPreferenceCategory extends PreferenceCategory {

    public MaterialPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr);
    }

    public MaterialPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MaterialPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaterialPreferenceCategory(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_category_card);
    }
}
