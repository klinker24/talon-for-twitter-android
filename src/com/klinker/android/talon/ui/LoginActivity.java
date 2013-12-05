package com.klinker.android.talon.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.klinker.android.talon.R;
import com.klinker.android.talon.services.DirectMessageRefreshService;
import com.klinker.android.talon.services.MentionsRefreshService;
import com.klinker.android.talon.services.TimelineRefreshService;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.fragments.DMFragment;
import com.klinker.android.talon.ui.fragments.HomeFragment;
import com.klinker.android.talon.ui.fragments.MentionsFragment;
import com.klinker.android.talon.utils.Utils;

import java.util.Date;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

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
                    btnLoginTwitter.setEnabled(false);
                    new RetreiveFeedTask().execute();
                } else if (btnLoginTwitter.getText().equals(getResources().getString(R.string.initial_sync))) {
                    new getTimeLine().execute();
                } else {

                    if (settings.timelineRefresh != 0) { // user only wants manual
                        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                        long now = new Date().getTime();
                        long alarm = now + settings.timelineRefresh;

                        PendingIntent pendingIntent = PendingIntent.getService(context, HomeFragment.HOME_REFRESH_ID, new Intent(context, TimelineRefreshService.class), 0);

                        am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.timelineRefresh, pendingIntent);

                        now = new Date().getTime();
                        alarm = now + settings.mentionsRefresh;

                        PendingIntent pendingIntent2 = PendingIntent.getService(context, MentionsFragment.MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);

                        am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.mentionsRefresh, pendingIntent2);

                        alarm = now + settings.dmRefresh;

                        Log.v("alarm_date", "dircet message " + new Date(alarm).toString());

                        PendingIntent pendingIntent3 = PendingIntent.getService(context, DMFragment.DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);

                        am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.dmRefresh, pendingIntent3);
                    }

                    finish();

                    Intent timeline = new Intent(context, MainActivity.class);
                    timeline.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
        return false;//settings.isTwitterLoggedIn;
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

        protected void onPostExecute(Void none) {
            btnLoginTwitter.setEnabled(true);
        }

        /**
         * Function to login to twitter
         */
        private void loginToTwitter() {
            // Check if already logged in

            try {
                requestToken = twitter.getOAuthRequestToken("oauth://roartotweet");
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
            } catch (TwitterException e) {
                e.printStackTrace();
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
                if (sharedPrefs.getInt("current_account", 1) == 1) {
                    sharedPrefs.edit().putString("twitter_users_name_1", user.getName()).commit();
                    sharedPrefs.edit().putString("twitter_screen_name_1", user.getScreenName()).commit();
                    sharedPrefs.edit().putString("twitter_background_url_1", user.getProfileBannerURL()).commit();
                    sharedPrefs.edit().putString("profile_pic_url_1", user.getBiggerProfileImageURL()).commit();
                } else {
                    sharedPrefs.edit().putString("twitter_users_name_2", user.getName()).commit();
                    sharedPrefs.edit().putString("twitter_screen_name_2", user.getScreenName()).commit();
                    sharedPrefs.edit().putString("twitter_background_url_2", user.getProfileBannerURL()).commit();
                    sharedPrefs.edit().putString("profile_pic_url_2", user.getBiggerProfileImageURL()).commit();
                }

                // syncs 200 timeline tweets with 2 pages
                Paging paging;
                paging = new Paging(2, 100);
                List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);

                HomeDataSource dataSource = new HomeDataSource(context);
                dataSource.open();

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    } catch (Exception e) {
                        break;
                    }
                }
                paging = new Paging(1, 100);
                statuses = twitter.getHomeTimeline(paging);

                sharedPrefs.edit().putLong("last_tweet_id_" + sharedPrefs.getInt("current_account", 1), statuses.get(0).getId()).commit();

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
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

                sharedPrefs.edit().putLong("last_mention_id_" + sharedPrefs.getInt("current_account", 1), statuses.get(0).getId()).commit();


                for (twitter4j.Status status : statuses) {
                    Log.v("mention_found", "found mention");
                    try {
                        mentionsSource.createTweet(status, sharedPrefs.getInt("current_account", 1), false);
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

                    sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), dm.get(0).getId()).commit();

                    for (DirectMessage directMessage : dm) {
                        try {
                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        } catch (Exception e) {
                            break;
                        }
                    }

                    List<DirectMessage> sent = twitter.getSentDirectMessages();

                    for (DirectMessage directMessage : sent) {
                        try {
                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
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
