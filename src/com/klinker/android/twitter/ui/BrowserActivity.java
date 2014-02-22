package com.klinker.android.twitter.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

public class BrowserActivity extends Activity {

    private AppSettings settings;
    private String url;
    private WebView browser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = AppSettings.getInstance(this);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        Utils.setUpTheme(this, settings);
        setContentView(R.layout.browser_activity);

        url = getIntent().getStringExtra("url");

        browser = (WebView) findViewById(R.id.webview);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setBuiltInZoomControls(true);
        browser.clearCache(true);
        browser.getSettings().setAppCacheEnabled(false);
        browser.getSettings().setLoadWithOverviewMode(true);
        browser.getSettings().setUseWideViewPort(true);
        browser.getSettings().setDisplayZoomControls(false);
        browser.getSettings().setSupportZoom(true);

        if (Build.VERSION.SDK_INT >= 17) {
            browser.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }

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

    @Override
    public void onStop() {
        browser.loadUrl("");
        super.onStop();
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
                onBackPressed();
                return true;

            case R.id.menu_open_web:
                Uri weburi = Uri.parse(url);
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                startActivity(launchBrowser);
                return true;

            default:
                return true;
        }
    }
}
