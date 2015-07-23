package com.klinker.android.twitter_l.ui.setup.material_login;

import android.os.Bundle;
import android.os.Handler;
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

public class LoginFragment extends Fragment {

    private MaterialLogin activity;
    private WebView web;

    public static LoginFragment getInstance() {
        return new LoginFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (MaterialLogin) getActivity();

        CookieSyncManager.createInstance(getActivity());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        web = new WebView(getActivity());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        web.setLayoutParams(params);

        try { web.getSettings().setJavaScriptEnabled(true); } catch (Exception e) { }
        web.getSettings().setAppCacheEnabled(false);
        web.getSettings().setSavePassword(false);
        web.getSettings().setSaveFormData(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                /*if (requestUrl != null && url != null && url.startsWith(requestUrl)) {
                    handleRequest(url);
                } else if (url.equals("https://twitter.com/")) {
                    webView.loadUrl(callbackUrl);
                } else {
                    webView.loadUrl(url);
                }*/
                return false;
            }
        });

        return web;
    }

    public void start(final MaterialLogin.Callback callback) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onDone();
            }
        }, 2000);
    }
}
