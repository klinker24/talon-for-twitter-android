package com.klinker.android.twitter.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.klinker.android.twitter.R;

public class BrowserActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.browser_activity);

        String url = getIntent().getStringExtra("url");

        WebView browser = (WebView) findViewById(R.id.webview);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setBuiltInZoomControls(true);
        browser.clearCache(true);
        browser.getSettings().setAppCacheEnabled(false);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.getSettings().setUseWideViewPort(true);

        final Activity activity = this;
        browser.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                // Activities and WebViews measure progress with different scales.
                // The progress meter will automatically disappear when we reach 100%
                activity.setProgress(progress * 100);
            }
        });

        browser.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(activity, getResources().getString(R.string.error_loading_page), Toast.LENGTH_SHORT).show();
            }
        });

        browser.loadUrl(url);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /*Uri weburi = Uri.parse(touched);
    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
    startActivity(launchBrowser);*/
}
