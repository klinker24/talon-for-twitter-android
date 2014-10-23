<<<<<<< HEAD:src/main/java/com/klinker/android/twitter_l/ui/setup/LoginActivity.java
package com.klinker.android.twitter_l.ui.setup;
=======
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

package com.klinker.android.twitter.ui.setup;
>>>>>>> master:src/main/java/com/klinker/android/twitter/ui/setup/LoginActivity.java

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.SearchRecentSuggestions;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import com.jakewharton.disklrucache.Util;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.FollowMePopup;
import com.klinker.android.twitter_l.manipulations.widgets.NotificationDrawerLayout;
import com.klinker.android.twitter_l.services.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.TimelineRefreshService;
import com.klinker.android.twitter_l.services.TrimDataService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.DMFragment;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.HomeFragment;
import com.klinker.android.twitter_l.ui.main_fragments.other_fragments.MentionsFragment;
import com.klinker.android.twitter_l.utils.MySuggestionsProvider;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.text.TextUtils;

import java.io.File;
import java.util.Date;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import twitter4j.DirectMessage;
import twitter4j.PagableResponseList;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class LoginActivity extends Activity {

    private Context context;
    private SharedPreferences sharedPrefs;

    private Twitter twitter;
    private static RequestToken requestToken;
    private static String verifier;

    private Button btnLoginTwitter;
    private Button noThanks;
    private TextSwitcher title;
    private TextSwitcher summary;
    private TextSwitcher progDescription;
    private SmoothProgressBar progressBar;
    private WebView mWebView;
    private LinearLayout main;

    private AppSettings settings;

    private FollowMePopup popup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        context = this;
        settings = AppSettings.getInstance(context);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        //context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH"));

        Utils.setUpTheme(context, settings);
        setContentView(R.layout.login_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.relLayout);

        layout.setPadding(0, Utils.getActionBarHeight(context) + Utils.getStatusBarHeight(context), 0, Utils.getNavBarHeight(context));

        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(settings.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(settings.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();

        TwitterFactory factory = new TwitterFactory(configuration);
        twitter = factory.getInstance();

        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
        noThanks = (Button) findViewById(R.id.dont_follow);
        title = (TextSwitcher) findViewById(R.id.welcome);
        summary = (TextSwitcher) findViewById(R.id.info);
        progDescription = (TextSwitcher) findViewById(R.id.progress_desc);
        progressBar = (SmoothProgressBar) findViewById(R.id.progress_bar);
        main = (LinearLayout) findViewById(R.id.mainLayout);

        progressBar.setSmoothProgressDrawableColors(new int[] {settings.themeColors.accentColorLight, settings.themeColors.accentColor});
        progressBar.setVisibility(View.GONE);

        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        Animation out = AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right);

        title.setFactory(new ViewSwitcher.ViewFactory() {

            public View makeView() {
                TextView myText = new TextView(LoginActivity.this);
                myText.setTextSize(30);
                if (settings.darkTheme) {
                    myText.setTextColor(getResources().getColor(R.color.dark_text));
                } else {
                    myText.setTextColor(getResources().getColor(R.color.light_text));
                }
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
                if (settings.darkTheme) {
                    myText.setTextColor(getResources().getColor(R.color.dark_text));
                } else {
                    myText.setTextColor(getResources().getColor(R.color.light_text));
                }
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
                if (settings.darkTheme) {
                    myText.setTextColor(getResources().getColor(R.color.dark_text));
                } else {
                    myText.setTextColor(getResources().getColor(R.color.light_text));
                }
                return myText;
            }
        });

        // set the animation type of textSwitcher
        progDescription.setInAnimation(in);
        progDescription.setOutAnimation(out);

        title.setText(getResources().getString(R.string.first_welcome));
        summary.setText(getResources().getString(R.string.first_info));

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        mWebView = (WebView)findViewById(R.id.loginWebView);
        try {
            mWebView.getSettings().setJavaScriptEnabled(true);
        } catch (Exception e) {
            
        }
        mWebView.getSettings().setAppCacheEnabled(false);
        mWebView.getSettings().setSavePassword(false);
        mWebView.getSettings().setSaveFormData(false);
        mWebView.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url)
            {
                if (url != null && url.startsWith("oauth:///talonforandroid")) {
                    handleTwitterCallback(url);
                } else {
                    webView.loadUrl(url);
                }
                return true;
            }
        });

        popup = new FollowMePopup(context);
        int threeHundred = Utils.toDP(300, context);
        popup.setWidth(threeHundred);
        popup.setHeight(threeHundred + Utils.toDP(80, context));

        noThanks.setText(getResources().getString(R.string.follow_progress));

        noThanks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                popup.setExpansionPointForAnim(view);
                popup.setOnTopOfView(view);

                popup.show();

            }
        });

        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // Call login_activity twitter function
                if (btnLoginTwitter.getText().toString().contains(getResources().getString(R.string.login_to_twitter))) {
                    if (Utils.hasInternetConnection(context)) {
                        btnLoginTwitter.setEnabled(false);

                        new RetreiveFeedTask().execute();
                    } else {
                        Toast.makeText(context, getResources().getString(R.string.no_network) + "!", Toast.LENGTH_SHORT).show();
                    }
                } else if (btnLoginTwitter.getText().toString().contains(getResources().getString(R.string.initial_sync))) {
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

                        PendingIntent pendingIntent3 = PendingIntent.getService(context, DMFragment.DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);
                        am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.dmRefresh, pendingIntent3);
                    }

                    // set up the autotrim
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    long now = new Date().getTime();
                    long alarm = now + AlarmManager.INTERVAL_DAY;
                    Log.v("alarm_date", "auto trim " + new Date(alarm).toString());
                    PendingIntent pendingIntent = PendingIntent.getService(context, 161, new Intent(context, TrimDataService.class), 0);
                    am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);

                    finish();

                    Intent timeline = new Intent(context, MainActivity.class);
                    timeline.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    timeline.putExtra("tutorial", true);
                    sharedPrefs.edit().putBoolean("should_refresh", false).commit();
                    sharedPrefs.edit().putBoolean("refresh_me", true).commit();
                    sharedPrefs.edit().putBoolean("refresh_me_mentions", true).commit();
                    sharedPrefs.edit().putBoolean("refresh_me_dm", true).commit();
                    sharedPrefs.edit().putBoolean("need_new_dm", false).commit();
                    sharedPrefs.edit().putBoolean("need_clean_databases_version_1_3_0", false).commit();
                    sharedPrefs.edit().putBoolean("setup_v_two", true).commit();
                    sharedPrefs.edit().putBoolean("version_2_2_7_1", false).commit();
                    AppSettings.invalidate();
                    startActivity(timeline);
                }

            }
        });
    }

    public void handleTwitterCallback(String url) {
        Log.v("twitter_login_activity", "oauth");

        // oAuth verifier
        verifier = Uri.parse(url).getQueryParameter("oauth_verifier");

        try {
            new RetreiveoAuth().execute();
        } catch (Exception e) {
            Looper.prepare();
            restartLogin();
        }

    }

    class FollowMe extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... urls) {
            Twitter twit = Utils.getTwitter(context, settings);

            try {
                twit.createFriendship("TalonAndroid");
            } catch (Exception x) {

            }

            return null;
        }

    }

    class RetreiveFeedTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... urls) {
            try {
                loginToTwitter();
                return null;
            } catch (Exception e) {
                try {
                    Looper.prepare();
                } catch (Exception x) {
                    // looper exists already
                }
                restartLogin();
                return null;
            }
        }

        protected void onPostExecute(Void none) {
            showWebView();

            if (requestToken != null) {
                mWebView.loadUrl(requestToken.getAuthenticationURL());
                mWebView.requestFocus(View.FOCUS_UP|View.FOCUS_RIGHT);
            } else {
                restartLogin();
            }
        }

        private void loginToTwitter() {
            try {
                requestToken = twitter.getOAuthRequestToken("oauth:///talonforandroid");
            } catch (TwitterException ex) {
                ex.printStackTrace();
                try {
                    Looper.prepare();
                } catch (Exception e) {
                    // looper exists already
                }
                restartLogin();
            }

        }

    }

    class RetreiveoAuth extends AsyncTask<String, Void, AccessToken> {

        protected AccessToken doInBackground(String... urls) {
            try {
                return twitter.getOAuthAccessToken(requestToken, verifier);
            } catch (Exception e) {
                try {
                    Looper.prepare();
                } catch (Exception x) {
                    // looper exists already
                }
                restartLogin();
                return null;
            }
        }

        protected void onPostExecute(AccessToken accessToken) {

            try {
                // Shared Preferences
                SharedPreferences.Editor e = sharedPrefs.edit();

                Log.v("logging_in", "this is what the token should be: " + accessToken.getToken());

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
                btnLoginTwitter.setEnabled(true);
                title.setText(getResources().getString(R.string.second_welcome));
                summary.setText(getResources().getString(R.string.second_info));

                hideHideWebView();

            } catch (Exception e) {
                restartLogin();
            }
        }
    }

    class getTimeLine extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressBar.setIndeterminate(true);
            progressBar.progressiveStart();
            progressBar.setVisibility(View.VISIBLE);

            btnLoginTwitter.setEnabled(false);

            progDescription.setVisibility(View.VISIBLE);
            progDescription.setText(getResources().getString(R.string.syncing_timeline));

            summary.setText("");
        }

        protected String doInBackground(Void... args) {

            try {
                settings = new AppSettings(context);

                twitter = Utils.getTwitter(context, settings);

                User user = twitter.verifyCredentials();
                if (sharedPrefs.getInt("current_account", 1) == 1) {
                    sharedPrefs.edit().putString("twitter_users_name_1", user.getName()).commit();
                    sharedPrefs.edit().putString("twitter_screen_name_1", user.getScreenName()).commit();
                    sharedPrefs.edit().putString("twitter_background_url_1", user.getProfileBannerURL()).commit();
                    sharedPrefs.edit().putString("profile_pic_url_1", user.getOriginalProfileImageURL()).commit();
                    sharedPrefs.edit().putLong("twitter_id_1", user.getId()).commit();
                } else {
                    sharedPrefs.edit().putString("twitter_users_name_2", user.getName()).commit();
                    sharedPrefs.edit().putString("twitter_screen_name_2", user.getScreenName()).commit();
                    sharedPrefs.edit().putString("twitter_background_url_2", user.getProfileBannerURL()).commit();
                    sharedPrefs.edit().putString("profile_pic_url_2", user.getOriginalProfileImageURL()).commit();
                    sharedPrefs.edit().putLong("twitter_id_2", user.getId()).commit();
                }

                // syncs 200 timeline tweets with 2 pages
                Paging paging;
                paging = new Paging(2, 100);
                List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);

                HomeDataSource dataSource = HomeDataSource.getInstance(context);

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    } catch (Exception e) {
                        dataSource = HomeDataSource.getInstance(context);
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    }
                }
                paging = new Paging(1, 100);
                statuses = twitter.getHomeTimeline(paging);

                if (statuses.size() > 0) {
                    sharedPrefs.edit().putLong("last_tweet_id_" + sharedPrefs.getInt("current_account", 1), statuses.get(0).getId()).commit();
                }

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    } catch (Exception e) {
                        dataSource = HomeDataSource.getInstance(context);
                        dataSource.createTweet(status, sharedPrefs.getInt("current_account", 1), true);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progDescription.setText(getResources().getString(R.string.syncing_mentions));
                    }
                });

                MentionsDataSource mentionsSource = MentionsDataSource.getInstance(context);

                // syncs 100 mentions
                paging = new Paging(1, 100);
                statuses = twitter.getMentionsTimeline(paging);

                for (twitter4j.Status status : statuses) {
                    try {
                        mentionsSource.createTweet(status, sharedPrefs.getInt("current_account", 1), false);
                    } catch (Exception e) {
                        mentionsSource = MentionsDataSource.getInstance(context);
                        mentionsSource.createTweet(status, sharedPrefs.getInt("current_account", 1), false);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progDescription.setText(getResources().getString(R.string.syncing_direct_messages));
                    }
                });

                // syncs 100 Direct Messages
                DMDataSource dmSource = DMDataSource.getInstance(context);

                try {
                    paging = new Paging(1, 100);

                    List<DirectMessage> dm = twitter.getDirectMessages(paging);

                    sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), dm.get(0).getId()).commit();

                    for (DirectMessage directMessage : dm) {
                        try {
                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        } catch (Exception e) {
                            dmSource = DMDataSource.getInstance(context);
                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        }
                    }

                    List<DirectMessage> sent = twitter.getSentDirectMessages();

                    for (DirectMessage directMessage : sent) {
                        try {
                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        } catch (Exception e) {
                            dmSource = DMDataSource.getInstance(context);
                            dmSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        }
                    }

                } catch (Exception e) {
                    // they have no direct messages
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progDescription.setText(getResources().getString(R.string.syncing_user));
                    }
                });

                FollowersDataSource followers = FollowersDataSource.getInstance(context);

                try {
                    int currentAccount = sharedPrefs.getInt("current_account", 1);
                    PagableResponseList<User> friendsPaging = twitter.getFriendsList(user.getId(), -1);

                    for (User friend : friendsPaging) {
                        followers.createUser(friend, currentAccount);
                    }

                    long nextCursor = friendsPaging.getNextCursor();

                    final SearchRecentSuggestions suggestions = new SearchRecentSuggestions(context,
                            MySuggestionsProvider.AUTHORITY, MySuggestionsProvider.MODE);

                    while (nextCursor != -1) {
                        friendsPaging = twitter.getFriendsList(user.getId(), nextCursor);

                        for (User friend : friendsPaging) {
                            followers.createUser(friend, currentAccount);

                            // insert them into the suggestion search provider
                            suggestions.saveRecentQuery(
                                    "@" + friend.getScreenName(),
                                    null);
                        }

                        nextCursor = friendsPaging.getNextCursor();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
                e.printStackTrace();

            }
            return null;
        }

        protected void onPostExecute(String file_url) {

            String text = getResources().getString(R.string.third_info);

            btnLoginTwitter.setEnabled(true);
            btnLoginTwitter.setText(getResources().getString(R.string.done_label));
            noThanks.setVisibility(View.VISIBLE);

            progressBar.progressiveStop();

            progDescription.setText(getResources().getString(R.string.done_syncing));
            title.setText(getResources().getString(R.string.third_welcome));
            summary.setText(text);

            TextUtils.linkifyText(context, (TextView) summary.getChildAt(0), null, false, "", true);
        }

    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    private void showWebView() {
        mWebView.setVisibility(View.VISIBLE);
        Animation in = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
        in.setDuration(400);

        Animation out = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                main.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        out.setDuration(400);
        main.startAnimation(out);
        mWebView.startAnimation(in);
    }

    private void hideHideWebView() {
        main.setVisibility(View.VISIBLE);
        Animation in = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        in.setDuration(400);

        Animation out = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mWebView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        out.setDuration(400);
        mWebView.startAnimation(out);
        main.startAnimation(in);
    }

    public void restartLogin() {
        new AlertDialog.Builder(context)
                .setMessage(context.getResources().getString(R.string.login_error))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent restart = new Intent(context, LoginActivity.class);
                        finish();
                        AppSettings.invalidate();
                        startActivity(restart);
                    }
                })
                .create()
                .show();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.getVisibility() == View.VISIBLE) {
            if (mWebView.canGoBack()) {
                mWebView.goBack();
                return;
            } else {
                hideHideWebView();
                btnLoginTwitter.setEnabled(true);
                return;
            }
        } else if (popup != null && popup.isShowing()) {
            popup.hide();
        }
    }
}
