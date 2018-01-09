package com.klinker.android.twitter_l.views.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;

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
        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(getContext());

        isSupporter = sharedPreferences.getBoolean("2018_supporter", false);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);

        TextView textView = (TextView) view.findViewById(android.R.id.title);

        if (isSupporter) {
            setTitle(R.string.are_supporter);
            setSummary(R.string.are_supporter_summary);
        } else {
            //textView.setTextSize(19);
            setTitle(R.string.become_supporter);
        }

        return view;
    }
}

