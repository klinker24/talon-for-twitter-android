package com.klinker.android.twitter_l.activities.setup.material_login;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;

public class FinishedFragment extends Fragment {

    private MaterialLogin activity;

    public static FinishedFragment getInstance() {
        return new FinishedFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (MaterialLogin) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_intro_finished, container, false);

        Glide.with(this).load("https://g.twimg.com/Twitter_logo_white.png").into((ImageView) root.findViewById(R.id.image));

        return root;
    }
}