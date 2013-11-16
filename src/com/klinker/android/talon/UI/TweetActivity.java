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
import android.webkit.WebView;
import android.widget.TextView;
import com.klinker.android.talon.R;
import com.klinker.android.talon.Utilities.AppSettings;

public class TweetActivity extends Activity {

    private AppSettings settings;
    private Context context;

    private String name;
    private String screenName;
    private String tweet;
    private String time;
    private String retweeter;
    private String webpage;
    private boolean picture;

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
        picture = from.getBooleanExtra("picture", false);
    }

    public void setUIElements() {
        TextView nametv = (TextView) findViewById(R.id.name);
        TextView screennametv = (TextView) findViewById(R.id.screen_name);
        TextView tweettv = (TextView) findViewById(R.id.tweet);
        TextView timetv = (TextView) findViewById(R.id.time);
        TextView retweetertv = (TextView) findViewById(R.id.retweeter);
        WebView website = (WebView) findViewById(R.id.webview);

        if (tweet.contains("http://")) {

        }

        if (webpage != null) {
            //new LoadWeb(webpage, website).execute();
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
        }
    }

    class LoadWeb extends AsyncTask<String, Void, Void> {

        String address;
        WebView website;

        public LoadWeb(String address, WebView website) {
            this.address = address;
            this.website = website;
        }

        protected Void doInBackground(String... urls) {
            website.loadUrl(address);
            return null;
        }
    }

    /*class GetFavoriteCount extends AsyncTask<String, Void, Status> {

        private ViewHolder holder;
        private long tweetId;

        public GetFavoriteCount(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected twitter4j.Status doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                return twitter.showStatus(tweetId);
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(twitter4j.Status status) {
            if (status != null) {
                holder.favCount.setText("- " + status.getFavoriteCount());

                if (status.isFavorited()) {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.favoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                    holder.isFavorited = true;
                } else {
                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.notFavoritedButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();

                    holder.favorite.setImageDrawable(context.getResources().getDrawable(resource));
                    holder.isFavorited = false;
                }
            }
        }
    }

    class GetRetweetCount extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public GetRetweetCount(ViewHolder holder, long tweetId) {
            this.holder = holder;
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
                holder.retweetCount.setText("- " + count);
            }
        }
    }

    class FavoriteStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public FavoriteStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                if (holder.isFavorited) {
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
            new GetFavoriteCount(holder, tweetId).execute();
        }
    }

    class RetweetStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public RetweetStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
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
            new GetRetweetCount(holder, tweetId).execute();
        }
    }

    class ReplyToStatus extends AsyncTask<String, Void, String> {

        private ViewHolder holder;
        private long tweetId;

        public ReplyToStatus(ViewHolder holder, long tweetId) {
            this.holder = holder;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                if (!isDM) {
                    twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(holder.reply.getText().toString());
                    reply.setInReplyToStatusId(tweetId);

                    twitter.updateStatus(reply);
                } else {
                    String screenName = holder.screenName;
                    String message = holder.reply.getText().toString();
                    DirectMessage dm = twitter.sendDirectMessage(screenName, message);

                    sharedPrefs.edit().putLong("last_direct_message_id", dm.getId()).commit();
                }

                return null;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            removeExpansionWithAnimation(holder);
            removeKeyboard(holder);
        }
    }   */
}
