package com.klinker.android.twitter_l.activities.setup.material_login;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.klinker.android.twitter_l.settings.AppSettings;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class LoginFragment extends Fragment {

    private static final String CALLBACK_URL = "http://talonfortwitter.com";

    private Twitter twitter;

    private MaterialLogin activity;
    private WebView web;

    private RequestToken requestToken;
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

        buildTwitter();
    }

    private void buildTwitter() {
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
                if (url != null && url.startsWith(CALLBACK_URL)) {
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

    private class RetrieveFeedTask extends AsyncTask<String, Void, RequestToken> {

        ProgressDialog pDialog;
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
            return loginToTwitter(CALLBACK_URL);
        }

        protected void onPostExecute(RequestToken token) {
            requestToken = token;

            try { pDialog.dismiss(); } catch (Exception e) { }

            if (token == null) {
                if (errorGettingToken) {
                    activity.restartLogin();
                }
            } else {
                callbackUrl = token.getAuthenticationURL();
                web.loadUrl(callbackUrl);
                web.requestFocus(View.FOCUS_UP | View.FOCUS_RIGHT);
            }
        }

        private RequestToken loginToTwitter(String callbackUrl) {
            try {
                if (twitter == null) {
                    buildTwitter();
                }

                return twitter.getOAuthRequestToken(callbackUrl);
            } catch (TwitterException | IllegalArgumentException ex) {
                ex.printStackTrace();
                errorGettingToken = true;
                return null;
            }
        }
    }

    private class RetreiveoAuth extends AsyncTask<String, Void, AccessToken> {

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

                SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(activity);


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

                e.apply(); // save changes

                finishedCallback.onDone();
            }
        }
    }

}
