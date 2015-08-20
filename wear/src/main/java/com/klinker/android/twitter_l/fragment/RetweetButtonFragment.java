package com.klinker.android.twitter_l.fragment;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.view.CircledImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activity.SettingsActivity;
import com.klinker.android.twitter_l.activity.WearActivity;
import com.klinker.android.twitter_l.transaction.KeyProperties;

public class RetweetButtonFragment extends Fragment {

    private static final String ARG_TWEET_ID = "tweet_id";

    public static RetweetButtonFragment create(long id) {
        Bundle args = new Bundle();
        args.putLong(ARG_TWEET_ID, id);

        RetweetButtonFragment frag = new RetweetButtonFragment();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_retweet_button, parent, false);
        CircledImageView button = (CircledImageView) view.findViewById(R.id.retweet_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((WearActivity)getActivity()).sendRetweetRequest(getArguments().getLong(ARG_TWEET_ID));
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int accentColor = sharedPreferences.getInt(KeyProperties.KEY_ACCENT_COLOR, getResources().getColor(R.color.orange_accent_color));
        button.setCircleColor(accentColor);
        return view;
    }

}
