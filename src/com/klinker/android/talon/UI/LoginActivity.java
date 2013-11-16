package com.klinker.android.talon.UI;

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
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.klinker.android.talon.R;
import com.klinker.android.talon.SQLite.DMDataSource;
import com.klinker.android.talon.SQLite.HomeDataSource;
import com.klinker.android.talon.SQLite.MentionsDataSource;
import com.klinker.android.talon.Utilities.AlertDialogManager;
import com.klinker.android.talon.Utilities.AppSettings;
import com.klinker.android.talon.Utilities.ConnectionDetector;
import com.klinker.android.talon.Utilities.Utils;
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

    private Context context;
    private SharedPreferences sharedPrefs;

    private Twitter twitter;
    private static RequestToken requestToken;
    private static String verifier;

    private Button btnLoginTwitter;
    private TextSwitcher title;
    private TextSwitcher summary;
    private TextSwitcher progDescription;
    private ProgressBar progressBar;

    private AppSettings settings;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        context = this;

        settings = new AppSettings(context);

        setUpTheme();
        setContentView(R.layout.login_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(settings.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(settings.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();

        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        title = (TextSwitcher) findViewById(R.id.welcome);
        summary = (TextSwitcher) findViewById(R.id.info);
        progDescription = (TextSwitcher) findViewById(R.id.progress_desc);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);


        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right);

        title.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                TextView myText = new TextView(LoginActivity.this);
                myText.setTextSize(30);
                return myText;
            }
        });

        // set the animation type of textSwitcher
        title.setInAnimation(in);
        title.setOutAnimation(out);

        summary.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                TextView myText = new TextView(LoginActivity.this);
                myText.setTextSize(17);
                return myText;
            }
        });

        // set the animation type of textSwitcher
        summary.setInAnimation(in);
        summary.setOutAnimation(out);

        progDescription.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                TextView myText = new TextView(LoginActivity.this);
                myText.setTextSize(17);
                return myText;
            }
        });

        // set the animation type of textSwitcher
        progDescription.setInAnimation(in);
        progDescription.setOutAnimation(out);

        title.setText(getResources().getString(R.string.first_welcome));
        summary.setText(getResources().getString(R.string.first_info));

        progressBar.setProgress(100);

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

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack);
                break;
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

            }
        }
    }

    class RetreiveoAuth extends AsyncTask<String, Void, AccessToken> {

        private Exception exception;

        protected AccessToken doInBackground(String... urls) {
            try {

                return twitter.getOAuthAccessToken(requestToken, verifier);

            } catch (Exception e) {

                this.exception = e;
                e.printStackTrace();

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

                // Hide login_activity button
                btnLoginTwitter.setText(getResources().getString(R.string.initial_sync));
                title.setText(getResources().getString(R.string.second_welcome));
                summary.setText(getResources().getString(R.string.second_info));

            } catch (Exception e) {

            }
        }
    }

    class getTimeLine extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressBar.setIndeterminate(true);

            btnLoginTwitter.setEnabled(false);
            btnLoginTwitter.setText(getResources().getString(R.string.back_to_timeline));

            progDescription.setVisibility(View.VISIBLE);
            progDescription.setText(getResources().getString(R.string.syncing_timeline));

            summary.setText("");
        }

        protected String doInBackground(Void... args) {

            try {
                twitter = Utils.getTwitter(context);

                User user = twitter.verifyCredentials();
                sharedPrefs.edit().putString("twitter_users_name", user.getName()).commit();
                sharedPrefs.edit().putString("twitter_screen_name", user.getScreenName()).commit();
                sharedPrefs.edit().putString("twitter_background_url", user.getProfileBannerURL()).commit();
                sharedPrefs.edit().putString("profile_pic_url", user.getBiggerProfileImageURL()).commit();

                // syncs 200 timeline tweets with 2 pages
                Paging paging;
                paging = new Paging(2, 100);
                List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);

                HomeDataSource dataSource = new HomeDataSource(context);
                dataSource.open();

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status);
                    } catch (Exception e) {
                        break;
                    }
                }
                paging = new Paging(1, 100);
                statuses = twitter.getHomeTimeline(paging);

                sharedPrefs.edit().putLong("last_tweet_id", statuses.get(0).getId()).commit();

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status);
                    } catch (Exception e) {
                        break;
                    }
                }

                dataSource.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progDescription.setText(getResources().getString(R.string.syncing_mentions));
                    }
                });

                MentionsDataSource mentionsSource = new MentionsDataSource(context);
                mentionsSource.open();

                // syncs 100 mentions
                paging = new Paging(1, 100);
                statuses = twitter.getMentionsTimeline(paging);

                sharedPrefs.edit().putLong("last_mention_id", statuses.get(0).getId()).commit();


                for (twitter4j.Status status : statuses) {
                    Log.v("mention_found", "found mention");
                    try {
                        mentionsSource.createTweet(status);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

                mentionsSource.close();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progDescription.setText(getResources().getString(R.string.syncing_direct_messages));
                    }
                });

                // syncs 100 Direct Messages
                DMDataSource dmSource = new DMDataSource(context);
                dmSource.open();
                try {
                    paging = new Paging(1, 100);

                    List<DirectMessage> dm = twitter.getDirectMessages(paging);

                    sharedPrefs.edit().putLong("last_direct_message_id", dm.get(0).getId()).commit();

                    for (DirectMessage directMessage : dm) {
                        try {
                            dmSource.createDirectMessage(directMessage);
                        } catch (Exception e) {
                            break;
                        }
                    }

                    List<DirectMessage> sent = twitter.getSentDirectMessages();

                    for (DirectMessage directMessage : sent) {
                        try {
                            dmSource.createDirectMessage(directMessage);
                        } catch (Exception e) {
                            break;
                        }
                    }

                    dmSource.close();

                } catch (Exception e) {
                    // they have no direct messages
                }



                dataSource.close();

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            }
            return null;
        }

        protected void onPostExecute(String file_url) {

            btnLoginTwitter.setEnabled(true);

            progressBar.setIndeterminate(false);
            progressBar.setProgress(100);

            progDescription.setText(getResources().getString(R.string.done_syncing));
            title.setText(getResources().getString(R.string.third_welcome));
            summary.setText(getResources().getString(R.string.third_info));
        }

    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }
}
