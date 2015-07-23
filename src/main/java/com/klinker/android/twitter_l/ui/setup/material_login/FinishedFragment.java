package com.klinker.android.twitter_l.ui.setup.material_login;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.utils.ImageUtils;

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

        ImageUtils.loadImage(activity, (ImageView) root.findViewById(R.id.image), "https://g.twimg.com/Twitter_logo_white.png",  App.getInstance(activity).getBitmapCache());

        return root;
    }
}