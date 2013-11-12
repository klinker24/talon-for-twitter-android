package com.klinker.android.roar.UI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.klinker.android.roar.R;
import com.klinker.android.roar.SQLite.HomeDataSource;
import com.klinker.android.roar.Utilities.AlertDialogManager;
import com.klinker.android.roar.Utilities.AppSettings;
import com.klinker.android.roar.Utilities.ConnectionDetector;
import com.klinker.android.roar.Utilities.Utils;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: luke
 * Date: 11/9/13
 * Time: 1:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoginActivity extends Activity {

    private ConnectionDetector cd;
    private ProgressDialog pDialog;
    AlertDialogManager alert = new AlertDialogManager();
    private Context context;
    private SharedPreferences sharedPrefs;

    private Twitter twitter;
    private static RequestToken requestToken;
    private static String verifier;

    private Button btnLoginTwitter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        context = this;

        AppSettings settings = new AppSettings(context);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(settings.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(settings.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();

        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            // Internet Connection is not present
            alert.showAlertDialog(LoginActivity.this,
                    "Internet Connection Error",
                    "Please connect to working Internet connection", false);
            // stop executing code by return
            return;
        }

        // All UI elements
        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);

        /**
         * Twitter login_activity button click event will call loginToTwitter() function
         * */
        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call login_activity twitter function
                if (btnLoginTwitter.getText().equals(getResources().getString(R.string.login_to_twitter))) {
                    new RetreiveFeedTask().execute();
                } else if (btnLoginTwitter.getText().equals(getResources().getString(R.string.initial_sync))) {
                    new getTimeLine().execute();
                } else {
                    Intent timeline = new Intent(context, MainActivity.class);
                    startActivity(timeline);
                }

            }
        });

        if (!isTwitterLoggedInAlready()) {
            Log.v("twitter_login_activity", "after web");
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith("oauth://roartotweet")) {
                Log.v("twitter_login_activity", "oauth");

                // oAuth verifier
                verifier = uri.getQueryParameter("oauth_verifier");

                try {
                    // Get the access token
                    //AccessToken accessToken = twitter.getOAuthAccessToken(
                    //requestToken, verifier);
                    new RetreiveoAuth().execute();
                    Log.v("twitter_login_activity", "retreiving");

                } catch (Exception e) {
                    // Check log for login_activity errors
                    e.printStackTrace();
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }
        }
    }

    private boolean isTwitterLoggedInAlready() {
        // return twitter login_activity status from Shared Preferences
        return sharedPrefs.getBoolean("is_logged_in", false);
    }

    class RetreiveFeedTask extends AsyncTask<String, Void, Void> {

        private Exception exception;

        @Override
        protected Void doInBackground(String... urls) {
            try {
                loginToTwitter();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Function to login_activity twitter
         */
        private void loginToTwitter() {
            // Check if already logged in
            if (!isTwitterLoggedInAlready()) {
                try {
                    requestToken = twitter.getOAuthRequestToken("oauth://roartotweet");
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
                } catch (TwitterException e) {
                    e.printStackTrace();
                }

            } else {
                // user already logged into twitter
                Toast.makeText(getApplicationContext(),
                        "Already Logged into twitter", Toast.LENGTH_LONG).show();
            }
        }
    }

    class RetreiveoAuth extends AsyncTask<String, Void, AccessToken> {

        private Exception exception;

        protected AccessToken doInBackground(String... urls) {
            try {
                Log.v("twitter_login_activity", "request token: " + requestToken);
                Log.v("twitter_login_activity", "verifier: " + verifier);
                return twitter.getOAuthAccessToken(requestToken, verifier);
            } catch (Exception e) {
                this.exception = e;
                e.printStackTrace();
                Log.v("twitter_login_activity", "caught executing");
                return null;
            }
        }

        protected void onPostExecute(AccessToken accessToken) {

            try {
                // Shared Preferences
                SharedPreferences.Editor e = sharedPrefs.edit();

                // After getting access token, access token secret
                // store them in application preferences
                e.putString("authentication_token", accessToken.getToken());
                e.putString("authentication_token_secret", accessToken.getTokenSecret());

                // Store login_activity status - true
                e.putBoolean("is_logged_in", true);
                e.commit(); // save changes

                Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

                // Hide login_activity button
                btnLoginTwitter.setText(getResources().getString(R.string.initial_sync));

            } catch (Exception e) {

            }
        }
    }

    /**
     * Function to get timeline
     */
    class getTimeLine extends AsyncTask<Void, Void, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Getting Timeline from Twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Places JSON
         */
        protected String doInBackground(Void... args) {

            try {
                twitter = Utils.getTwitter(context);

                User user = twitter.verifyCredentials();
                Paging paging;
                paging = new Paging(1, 500);
                List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);

                sharedPrefs.edit().putLong("last_tweet_id", statuses.get(0).getId()).commit();

                HomeDataSource dataSource = new HomeDataSource(context);
                dataSource.open();

                Log.v("timeline_update", "Showing @" + user.getScreenName() + "'s home timeline.");
                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status);
                    } catch (Exception e) {
                        break;
                    }
                }

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();

            btnLoginTwitter.setText(getResources().getString(R.string.back_to_timeline));
        }

    }
}
