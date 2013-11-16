package com.klinker.android.talon.UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.klinker.android.talon.R;
import com.klinker.android.talon.Utilities.AppSettings;
import com.klinker.android.talon.Utilities.CircleTransform;
import com.klinker.android.talon.Utilities.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import twitter4j.Status;
import twitter4j.Twitter;

public class TweetActivity extends Activity {

    private AppSettings settings;
    private Context context;

    private String name;
    private String screenName;
    private String tweet;
    private String time;
    private String retweeter;
    private String webpage;
    private String proPic;
    private boolean picture;
    private long tweetId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = new AppSettings(context);

        setUpTheme();
        //setUpWindow();
        getFromIntent();

        Log.v("tweet_page", "done with setup");

        setContentView(R.layout.tweet_activity);

        setUIElements();

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


        getWindow().requestFeature(Window.FEATURE_PROGRESS);
    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .9f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

    }

    public void getFromIntent() {
        Intent from = getIntent();

        name = from.getStringExtra("name");
        screenName = from.getStringExtra("screenname");
        tweet = from.getStringExtra("tweet");
        time = from.getStringExtra("time");
        retweeter = from.getStringExtra("retweeter");
        webpage = from.getStringExtra("webpage");
        tweetId = from.getLongExtra("tweetid", 0);
        picture = from.getBooleanExtra("picture", false);
        proPic = from.getStringExtra("proPic");
    }

    public void setUIElements() {
        TextView nametv = (TextView) findViewById(R.id.name);
        TextView screennametv = (TextView) findViewById(R.id.screen_name);
        TextView tweettv = (TextView) findViewById(R.id.tweet);
        TextView timetv = (TextView) findViewById(R.id.time);
        TextView retweetertv = (TextView) findViewById(R.id.retweeter);
        WebView website = (WebView) findViewById(R.id.webview);

        ImageView profilePic = (ImageView) findViewById(R.id.profile_pic);

        final ImageButton favoriteButton = (ImageButton) findViewById(R.id.favorite);
        final ImageButton retweetButton = (ImageButton) findViewById(R.id.retweet);
        final TextView favoriteCount = (TextView) findViewById(R.id.fav_count);
        final TextView retweetCount = (TextView) findViewById(R.id.retweet_count);
        final EditText reply = (EditText) findViewById(R.id.reply);
        final ImageButton replyButton = (ImageButton) findViewById(R.id.reply_button);

        Picasso.with(context)
                .load(proPic)
                .transform(new CircleTransform())
                .into(profilePic);

        if (tweet.contains("http://")) {
            String[] split = tweet.split(" ");

            for (String s : split) {
                if (s.contains("http://")) {
                    s.replaceAll("!", "");
                    s.replaceAll("\"", "");

                    if(webpage == null) {
                        webpage = s;
                    }

                    break;
                }
            }
        }

        if (webpage != null) {
            website.getSettings().setJavaScriptEnabled(true);
            website.getSettings().setBuiltInZoomControls(true);

            final Activity activity = this;
            website.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int progress) {
                    // Activities and WebViews measure progress with different scales.
                    // The progress meter will automatically disappear when we reach 100%
                    activity.setProgress(progress * 100);
                }
            });

            website.setWebViewClient(new WebViewClient() {
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Toast.makeText(activity, "Couldn't load the web page. " + description, Toast.LENGTH_SHORT).show();
                }
            });

            website.loadUrl(webpage);
        } else {
            website.setVisibility(View.GONE);
        }

        nametv.setText(name);
        screennametv.setText("@" + screenName);
        tweettv.setText(tweet);
        timetv.setText(time);

        tweettv.setLinksClickable(true);

        if (retweeter.length() > 0 ) {
            retweetertv.setText("Retweeted by @" + retweeter);
            retweetertv.setVisibility(View.VISIBLE);
            isRetweet = true;
        }

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FavoriteStatus(favoriteCount, favoriteButton, tweetId).execute();
            }
        });

        retweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new RetweetStatus(retweetCount, tweetId).execute();
            }
        });

        new GetFavoriteCount(favoriteCount, favoriteButton, tweetId).execute();
        new GetRetweetCount(retweetCount, tweetId).execute();

        String text = tweet;
        String extraNames = "";

        if (text.contains("@")) {
            String[] split = text.split(" ");

            for (String s : split) {
                if (s.endsWith(":")) {
                    s = s.substring(0, s.length() - 1);
                }

                if (s.contains("@") && !s.contains(settings.myScreenName) && !s.contains(screenName) && s.length() > 1) {
                    extraNames += s.substring(s.indexOf("@")) + " ";
                }
            }
        }

        reply.setText("@" + screenName + " " + extraNames);

        reply.setSelection(reply.getText().length());

        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ReplyToStatus(reply, tweetId).execute();
            }
        });

    }

    private boolean isFavorited = false;
    private boolean isRetweet = false;

    class GetFavoriteCount extends AsyncTask<String, Void, Status> {

        private long tweetId;
        private TextView favs;
        private ImageButton favButton;

        public GetFavoriteCount(TextView favs, ImageButton favButton, long tweetId) {
            this.tweetId = tweetId;
            this.favButton = favButton;
            this.favs = favs;
        }

        protected twitter4j.Status doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                if (isRetweet) {
                    twitter4j.Status retweeted = twitter.showStatus(tweetId).getRetweetedStatus();
                    return retweeted;
                }
                return twitter.showStatus(tweetId);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.Status status) {
            if (status != null) {
                favs.setText("- " + status.getFavoriteCount());

                if (status.isFavorited()) {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    favButton.setImageDrawable(context.getResources().getDrawable(resource));
                    isFavorited = true;
                } else {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    favButton.setImageDrawable(context.getResources().getDrawable(resource));
                    isFavorited = false;
                }
            }
        }
    }

    class GetRetweetCount extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView retweetCount;

        public GetRetweetCount(TextView retweetCount, long tweetId) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                twitter4j.Status status = twitter.showStatus(tweetId);
                return "" + status.getRetweetCount();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            if (count != null) {
                retweetCount.setText("- " + count);
            }
        }
    }

    class FavoriteStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView favs;
        private ImageButton favButton;

        public FavoriteStatus(TextView favs, ImageButton favButton, long tweetId) {
            this.tweetId = tweetId;
            this.favButton = favButton;
            this.favs = favs;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                if (isFavorited) {
                    twitter.destroyFavorite(tweetId);
                } else {
                    twitter.createFavorite(tweetId);
                }
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            new GetFavoriteCount(favs, favButton, tweetId).execute();
        }
    }

    class RetweetStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private TextView retweetCount;

        public RetweetStatus(TextView retweetCount, long tweetId) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                twitter.retweetStatus(tweetId);
                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            new GetRetweetCount(retweetCount, tweetId).execute();
        }
    }

    class ReplyToStatus extends AsyncTask<String, Void, String> {

        private long tweetId;
        private EditText message;

        public ReplyToStatus(EditText message, long tweetId) {
            this.message = message;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(message.getText().toString());
                reply.setInReplyToStatusId(tweetId);

                twitter.updateStatus(reply);


                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            finish();
        }
    }
}
