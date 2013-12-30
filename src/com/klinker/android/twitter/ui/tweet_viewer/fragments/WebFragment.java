package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.ArrayList;

public class WebFragment extends Fragment {
    private Context context;
    private View layout;
    private AppSettings settings;
    private ArrayList<String> webpages;

    public WebFragment(AppSettings settings, ArrayList<String> webpages) {
        this.settings = settings;
        this.webpages = webpages;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        layout = inflater.inflate(R.layout.browser_activity, null);
        WebView webView = (WebView) layout.findViewById(R.id.webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.clearCache(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl(webpages.get(0));

        return layout;
    }
}
