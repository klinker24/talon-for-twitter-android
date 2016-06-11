package com.klinker.android.twitter_l.manipulations.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.Utils;

public class BecomeSupporterPreference extends IconPreference {

    public BecomeSupporterPreference(Context context) {
        super(context);
        init();
    }

    public BecomeSupporterPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private boolean isSupporter = false;

    public void init() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        isSupporter = sharedPreferences.getBoolean("2016_supporter", false);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        TextView textView = (TextView) view.findViewById(android.R.id.title);

        if (isSupporter) {
            setTitle(R.string.are_supporter);
            setSummary(R.string.are_supporter_summary);
        } else {
            textView.setTextSize(19);
            setTitle(R.string.become_supporter);
        }

        return view;
    }
}

