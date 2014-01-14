package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.ArrayList;

public class WebFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private View layout;
    private ArrayList<String> webpages;
    private String[] pages;

    private WebView webView;

    public WebFragment(AppSettings settings, ArrayList<String> webpages) {
        this.webpages = webpages;
    }

    public WebFragment() {
        this.webpages = new ArrayList<String>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        layout = inflater.inflate(R.layout.web_fragment, null);
        webView = (WebView) layout.findViewById(R.id.webview);

        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.clearCache(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient());

        if (webpages.size() > 0) {
            webView.loadUrl(webpages.get(0));
        }

        pages = new String[webpages.size()];

        for (int i = 0; i < pages.length; i++) {
            pages[i] = webpages.get(i);
        }

        Spinner spinner = (Spinner) layout.findViewById(R.id.spinner);
        spinner.setAdapter(new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, pages));
        spinner.setOnItemSelectedListener(this);

        if (pages.length <= 1) {
            spinner.setVisibility(View.GONE);
        }

        return layout;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        webView.loadUrl(pages[i]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onDestroy() {
        webView.loadUrl("");
        super.onDestroy();
    }
}
