package com.klinker.android.twitter_l.activities;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.*;

import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.views.widgets.HTML5WebView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class BrowserActivity extends WhiteToolbarActivity {

    public AppSettings settings;
    public String url;
    private HTML5WebView browser;

    public Context context;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setTaskDescription(this);

        /*try {
            getWindow().requestFeature(Window.FEATURE_PROGRESS);
        } catch (Exception e) {
            // oops, something went wrong... don't quite know what though, or why
            startActivity(new Intent(this, BrowserActivity.class).putExtra("url", url));
            overridePendingTransition(0,0);
            finish();
            return;
        }*/

        context = this;

        overridePendingTransition(R.anim.slide_in_left, R.anim.activity_zoom_exit);

        settings = AppSettings.getInstance(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        url = getIntent().getStringExtra("url").replace("http://", "https://");

        Utils.setUpTweetTheme(this, settings);
        Utils.setActionBar(this);

        setUpLayout();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setUpLayout() {

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(getResources().getColor(android.R.color.white));

        ViewGroup.LayoutParams frameParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frame.setLayoutParams(frameParams);

        browser = new HTML5WebView(this);
        FrameLayout layout = browser.getLayout();

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            FrameLayout.LayoutParams browserParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            browserParams.topMargin  = Utils.getStatusBarHeight(this) + Utils.getActionBarHeight(this);
            layout.setLayoutParams(browserParams);

            View status = new View(context);
            status.setBackgroundColor(getResources().getColor(android.R.color.black));

            FrameLayout.LayoutParams statParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            statParams.height  = Utils.getStatusBarHeight(this);
            status.setLayoutParams(statParams);

            frame.addView(status);
        }

        frame.addView(layout);
        setContentView(frame);

        browser.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        if (url.contains("youtu") || url.contains("play.google.com")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } else {
            browser.loadUrl(url);
        }

        browser.setWebViewClient(new WebClient());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browser_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_open_web:
                try {
                    Uri weburi;

                    if (browser != null) {
                        weburi = Uri.parse(browser.getUrl());
                    } else { // on plain text
                        weburi = Uri.parse(url);
                    }

                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                    startActivity(launchBrowser);
                } catch (Exception e) {
                    e.printStackTrace();
                    // it is a picture link that they clicked from the timeline i think...
                }
                return true;

            default:
                return true;
        }
    }

    @Override
    public void onDestroy() {
        try {
            browser.destroy();
        } catch (Exception e) {
            // plain text browser
        }
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (url.contains("vine")) {
                ((AudioManager)getSystemService(
                        Context.AUDIO_SERVICE)).requestAudioFocus(
                        focusChange -> {}, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onBackPressed() {
        if (browser != null && browser.canGoBack() && !browser.getUrl().equals(url)) {
            browser.goBack();
        } else {
            super.onBackPressed();
        }
    }

    class WebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            if (url.contains("twitter://")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(intent);
                } catch (Exception e) { }

                return false;
            } else {
                webView.loadUrl(url);
                getIntent().putExtra("url", url);

                return true;
            }
        }
    }
}
