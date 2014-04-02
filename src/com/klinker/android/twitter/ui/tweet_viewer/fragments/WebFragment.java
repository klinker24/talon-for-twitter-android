package com.klinker.android.twitter.ui.tweet_viewer.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
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
import com.klinker.android.twitter.manipulations.widgets.HTML5WebView;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.ArrayList;

public class WebFragment extends Fragment {
    private View layout;
    private ArrayList<String> webpages;
    private String[] pages;

    private HTML5WebView webView;
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

        webView = new HTML5WebView(context);
        webView.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        try {
            webView.loadUrl(webpages.get(0));
        } catch (Exception e) {

        }

        return webView.getLayout();
    }

    @Override
    public void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (webpages.get(0).contains("vine")) {
                ((AudioManager)context.getSystemService(
                        Context.AUDIO_SERVICE)).requestAudioFocus(
                        new AudioManager.OnAudioFocusChangeListener() {
                            @Override
                            public void onAudioFocusChange(int focusChange) {}
                        }, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        } catch (Exception e) {

        }
    }
}
