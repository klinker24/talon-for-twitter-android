package com.klinker.android.twitter_l.manipulations;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;

public class BecomeSupporterPreference extends Preference {

    public BecomeSupporterPreference(Context context) {
        super(context);
    }

    public BecomeSupporterPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private boolean isSupporter = false;

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        TextView textView = (TextView) view.findViewById(android.R.id.title);

        if (sharedPreferences.getBoolean("2016_supporter", true)) {
            setTitle(R.string.are_supporter);
            setSummary(R.string.are_supporter_summary);
            isSupporter = true;
        } else {
            textView.setTextSize(19);
            setTitle(R.string.become_supporter);
        }

        return view;
    }
}

