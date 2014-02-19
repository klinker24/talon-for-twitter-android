package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.VideoView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.ArrayList;

public class WebFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private View layout;
    private ArrayList<String> webpages;
    private String[] pages;

    private WebView webView;
    private ProgressBar progressBar;

    public Context context;

    public WebFragment(AppSettings settings, ArrayList<String> webpages) {
        this.webpages = webpages;
    }

    public WebFragment() {
        this.webpages = new ArrayList<String>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        context = getActivity();

        layout = inflater.inflate(R.layout.web_fragment, null);
        webView = (WebView) layout.findViewById(R.id.webview);
        progressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);
        progressBar.setProgress(0);

        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.clearCache(true);
        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportZoom(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                super.onShowCustomView(view, callback);
                if (view instanceof FrameLayout){
                    FrameLayout frame = (FrameLayout) view;
                    if (frame.getFocusedChild() instanceof VideoView){
                        VideoView video = (VideoView) frame.getFocusedChild();
                        frame.removeView(video);
                        getActivity().setContentView(video);
                        video.start();
                    }
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress)
            {
                if(progress < 100 && progressBar.getVisibility() == ProgressBar.GONE){
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
                progressBar.setProgress(progress);
                if(progress == 100) {
                    progressBar.setVisibility(ProgressBar.GONE);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= 17) {
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }

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
        if (pages[i].contains("play.google.com")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(pages[i])));
        } else {
            webView.loadUrl(pages[i]);
        }
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
