package com.klinker.android.twitter_l.ui.setup.material_login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class LoginFragment extends Fragment {

    private Twitter twitter;

    private MaterialLogin activity;
    private WebView web;

    private RequestToken requestToken;
    private String requestUrl;
    private String callbackUrl;
    private String oauthVerifier;

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

        ConfigurationBuilder builder = new ConfigurationBuilder();
        APIKeys keys = new APIKeys(activity);
        builder.setOAuthConsumerKey(keys.consumerKey);
        builder.setOAuthConsumerSecret(keys.consumerSecret);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        web = new WebView(getActivity());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        web.setLayoutParams(params);

        web.setBackgroundResource(android.R.color.white);

        try { web.getSettings().setJavaScriptEnabled(true); } catch (Exception e) { }
        web.getSettings().setAppCacheEnabled(false);
        web.getSettings().setSavePassword(false);
        web.getSettings().setSaveFormData(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                if (requestUrl != null && url != null && url.startsWith(requestUrl)) {
                    handleRequest(url);
                    webView.loadUrl("");
                } else if (url.equals("https://twitter.com/")) {
                    webView.loadUrl(callbackUrl);
                } else {
                    webView.loadUrl(url);
                }

                return false;
            }
        });

        return web;
    }

    private MaterialLogin.Callback finishedCallback = null;
    public void start(final MaterialLogin.Callback callback) {
        finishedCallback = callback;
        new RetrieveFeedTask().execute();
    }

    public void handleRequest(String url) {
        oauthVerifier = Uri.parse(url).getQueryParameter("oauth_verifier");
        new RetreiveoAuth().execute();

    }

    class RetrieveFeedTask extends AsyncTask<String, Void, RequestToken> {

        ProgressDialog pDialog;
        boolean licenseTimeout = false;
        boolean errorGettingToken = false;

        @Override
        public void onPreExecute() {
            super.onPreExecute();

            try {
                pDialog = new ProgressDialog(getActivity());
                pDialog.setMessage(getResources().getString(R.string.preparing_signin) + "...");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
            } catch (Exception e) {

            }
        }

        @Override
        protected RequestToken doInBackground(String... urls) {

            int counter = 0;
            while ((activity == null || !activity.isCheckComplete) && counter < 10) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { }
                counter++;
            }

            if (counter == 10 && !activity.isCheckComplete) {
                // timeout on the license check
                licenseTimeout = true;
                return null;
            }

            requestUrl = activity.getUserUrl();

            if (!activity.licenced || requestUrl == null) {
                return null;
            }

            return loginToTwitter(requestUrl);
        }

        protected void onPostExecute(RequestToken token) {

            requestToken = token;

            try { pDialog.dismiss(); } catch (Exception e) { }

            if (token == null) {
                if (licenseTimeout) {
                    activity.licenseTimeout();
                } else {
                    if (errorGettingToken) {
                        activity.restartLogin();
                    } else {
                        activity.notLicenced();
                    }
                }
            } else if (activity.licenced) {
                callbackUrl = token.getAuthenticationURL();
                web.loadUrl(callbackUrl);
                web.requestFocus(View.FOCUS_UP | View.FOCUS_RIGHT);
            } else {
                activity.restartLogin();
            }
        }

        private RequestToken loginToTwitter(String requestUrl) {
            try {
                return twitter.getOAuthRequestToken(requestUrl);
            } catch (TwitterException ex) {
                ex.printStackTrace();
                errorGettingToken = true;
                return null;
            }
        }
    }

    class RetreiveoAuth extends AsyncTask<String, Void, AccessToken> {

        ProgressDialog pDialog;

        @Override
        public void onPreExecute() {
            super.onPreExecute();

            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage(getResources().getString(R.string.verifying_login) + "...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected AccessToken doInBackground(String... urls) {
            try {
                return twitter.getOAuthAccessToken(requestToken, oauthVerifier);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(AccessToken accessToken) {

            pDialog.dismiss();

            if (accessToken == null) {
                activity.restartLogin();
            } else {

                SharedPreferences sharedPrefs = activity.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

                SharedPreferences.Editor e = sharedPrefs.edit();

                if (sharedPrefs.getInt("current_account", 1) == 1) {
                    e.putString("authentication_token_1", accessToken.getToken());
                    e.putString("authentication_token_secret_1", accessToken.getTokenSecret());
                    e.putBoolean("is_logged_in_1", true);
                } else {
                    e.putString("authentication_token_2", accessToken.getToken());
                    e.putString("authentication_token_secret_2", accessToken.getTokenSecret());
                    e.putBoolean("is_logged_in_2", true);
                }

                e.commit(); // save changes

                finishedCallback.onDone();
            }
        }
    }

}
