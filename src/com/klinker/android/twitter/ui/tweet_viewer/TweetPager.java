package com.klinker.android.twitter.ui.tweet_viewer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.TweetPagerAdapter;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.ComposeActivity;
import com.klinker.android.twitter.ui.tweet_viewer.fragments.TweetYouTubeFragment;
import com.klinker.android.twitter.utils.HtmlUtils;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.Utils;

import java.net.URL;
import java.util.Random;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TweetPager extends YouTubeBaseActivity {

    private TweetPagerAdapter mSectionsPagerAdapter;
    private ViewPager pager;
    private Context context;
    private AppSettings settings;

    private String name;
    private String screenName;
    private String tweet;
    private long time;
    private String retweeter;
    private String webpage;
    private String proPic;
    private boolean picture;
    private long tweetId;
    private String[] users;
    private String[] hashtags;
    private String[] otherLinks;
    private boolean isMyTweet = false;
    private boolean isMyRetweet = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        context = this;
        settings = new AppSettings(this);

        getFromIntent();
        Utils.setUpPopupTheme(context, settings);

        // methods for advancing windowed
        boolean settingsVal = settings.advanceWindowed;
        boolean fromWidget = getIntent().getBooleanExtra("from_widget", false);
        boolean youtube = webpage.contains("youtu");

        // cases: (youtube will ALWAYS be full screen...)
        // from widget
        // the user set the preference to advance windowed
        // has a webview and want to advance windowed
        if (fromWidget || settingsVal) {
            setUpWindow(youtube);
        }

        setContentView(R.layout.tweet_pager);
        pager = (ViewPager) findViewById(R.id.pager);
        mSectionsPagerAdapter = new TweetPagerAdapter(getFragmentManager(), context,
                name, screenName, tweet, time, retweeter, webpage, proPic, tweetId, picture, users, hashtags, otherLinks, isMyTweet, isMyRetweet);
        pager.setAdapter(mSectionsPagerAdapter);
        pager.setOffscreenPageLimit(3);

        final int numberOfPages = mSectionsPagerAdapter.getCount();

        switch (numberOfPages) {
            case 2:
                pager.setCurrentItem(0);
                break;
            case 3:
                pager.setCurrentItem(1);
                break;
            case 4:
                pager.setCurrentItem(2);
                break;
        }

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if (mSectionsPagerAdapter.getHasYoutube()) {
                    switch (numberOfPages) {
                        case 3:
                            if (i != 0) {
                                TweetYouTubeFragment.pause();
                            } else {
                                TweetYouTubeFragment.resume();
                            }
                            break;
                        case 4:
                            if (i != 1) {
                                TweetYouTubeFragment.pause();
                            } else {
                                TweetYouTubeFragment.resume();
                            }
                            break;
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
    }

    public void setUpWindow(boolean youtube) {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        if(!youtube) {
            params.dimAmount = .75f;  // set it higher if you want to dim behind the window
        } else {
            params.dimAmount = 0f;
        }
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
        time = from.getLongExtra("time", 0);
        retweeter = from.getStringExtra("retweeter");
        webpage = from.getStringExtra("webpage");
        tweetId = from.getLongExtra("tweetid", 0);
        picture = from.getBooleanExtra("picture", false);
        proPic = from.getStringExtra("proPic");

        try {
            users = from.getStringExtra("users").split("  ");
        } catch (Exception e) {
            users = null;
        }

        try {
            hashtags = from.getStringExtra("hashtags").split("  ");
        } catch (Exception e) {
            hashtags = null;
        }

        try {
            otherLinks = from.getStringExtra("other_links").split("  ");
        } catch (Exception e) {
            otherLinks = null;
        }

        if (screenName.equals(settings.myScreenName)) {
            isMyTweet = true;
        } else if (screenName.equals(retweeter)) {
            isMyRetweet = true;
        }
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context);

            try {
                twitter.destroyStatus(tweetId);

                HomeDataSource source = new HomeDataSource(context);
                source.open();
                source.deleteTweet(tweetId);
                source.close();

                return true;
            } catch (TwitterException e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {
            if (deleted) {
                Toast.makeText(context, getResources().getString(R.string.deleted_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
            }

            finish();
        }
    }

    private ShareActionProvider mShareActionProvider;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.tweet_activity, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        mShareActionProvider.setShareIntent(getShareIntent());

        return super.onCreateOptionsMenu(menu);
    }

    private Intent getShareIntent() {
        String text1 = "\"@" + screenName + ": " + tweet + "\" ";
        text1 = HtmlUtils.removeColorHtml(text1);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text1);
        return intent;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        final int MENU_SHARE = 0;
        final int MENU_DELETE_TWEET = 1;
        final int MENU_QUOTE = 2;
        final int MENU_COPY_TEXT = 3;
        final int MENU_OPEN_WEB = 4;
        final int MENU_SAVE_IMAGE = 5;

        if (!isMyTweet) {
            menu.getItem(MENU_DELETE_TWEET).setVisible(false);
        } else {
            menu.getItem(MENU_QUOTE).setVisible(false);
        }

        if (mSectionsPagerAdapter.getHasWebpage()) {
            menu.getItem(MENU_OPEN_WEB).setVisible(false);
        }

        if (!picture) {
            menu.getItem(MENU_SAVE_IMAGE).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_delete_tweet:
                new DeleteTweet().execute();
                return true;

            case R.id.menu_share:
                String text1 = "\"@" + screenName + ": " + tweet + "\" ";
                text1 = HtmlUtils.removeColorHtml(text1);
                Log.v("my_text_on_share", text1);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, text1);

                startActivity(share);
                return true;

            case R.id.menu_copy_text:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("tweet_text", HtmlUtils.removeColorHtml(tweet));
                clipboard.setPrimaryClip(clip);
                return true;

            case R.id.menu_open_web:
                Uri weburi = Uri.parse(webpage);
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                startActivity(launchBrowser);

                return true;

            case R.id.menu_save_image:

                Toast.makeText(context, getResources().getString(R.string.saving_picture), Toast.LENGTH_SHORT).show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            URL mUrl = new URL(webpage);

                            Bitmap bitmap = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());

                            Random generator = new Random();
                            int n = 1000000;
                            n = generator.nextInt(n);
                            String fname = "Image-" + n;

                            IOUtils.saveImage(bitmap, fname, context);

                            Toast.makeText(context, getResources().getString(R.string.saved_picture), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        }
                    }
                }).start();

                return true;

            case R.id.menu_quote:
                String text = "\"@" + screenName + ": " + tweet + "\" ";

                text = HtmlUtils.removeColorHtml(text);

                Intent quote = new Intent(context, ComposeActivity.class);
                quote.putExtra("user", text);
                startActivity(quote);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
