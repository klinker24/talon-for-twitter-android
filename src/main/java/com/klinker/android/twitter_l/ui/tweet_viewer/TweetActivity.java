package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.*;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.manipulations.widgets.*;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.utils.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import com.klinker.android.twitter_l.utils.text.TextUtils;

import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.hdodenhof.circleimageview.CircleImageView;
import twitter4j.*;

public class TweetActivity extends AppCompatActivity {

    public Context context;
    public AppSettings settings;
    public SharedPreferences sharedPrefs;


    public String name;
    public String screenName;
    public String tweet;
    public long time;
    public String retweeter;
    public String webpage;
    public String proPic;
    public boolean picture;
    public long tweetId;
    public String[] users;
    public String[] hashtags;
    public String[] otherLinks;
    public String linkString;
    public boolean isMyTweet = false;
    public boolean isMyRetweet = true;
    public boolean secondAcc = false;
    public String gifVideo;

    protected boolean fromLauncher = false;

    public TweetYouTubeFragment youTubeFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = AppSettings.getInstance(this);
        Utils.setSharedContentTransition(this);
        getFromIntent();

        ArrayList<String> webpages = new ArrayList<String>();

        if (otherLinks == null) {
            otherLinks = new String[0];
        }

        if (gifVideo == null) {
            gifVideo = "no gif video";
        }
        boolean hasWebpage;
        boolean youtube = false;
        if (otherLinks.length > 0 && !otherLinks[0].equals("")) {
            for (String s : otherLinks) {
                if (s.contains("youtu")) {
                    youtubeVideo = s;
                    youtube = true;
                    break;
                } else {
                    if (!s.contains("pic.twitt")) {
                        webpages.add(s);
                    }
                }
            }

            if (webpages.size() >= 1) {
                hasWebpage = true;
            } else {
                hasWebpage = false;
            }

        } else {
            hasWebpage = false;
        }

        if (hasWebpage && webpages.size() == 1) {
            if (webpages.get(0).contains(tweetId + "/photo/1")) {
                hasWebpage = false;
                gifVideo = webpages.get(0);
            } else if (webpages.get(0).contains("vine.co/v/")) {
                hasWebpage = false;
                gifVideo = webpages.get(0);
            }
        }

        if (getResources().getBoolean(R.bool.isTablet)) {
            setUpWindow((youtubeVideo != null && !android.text.TextUtils.isEmpty(youtubeVideo)) ||
                    (null != gifVideo &&
                            !android.text.TextUtils.isEmpty(gifVideo) &&
                            (gifVideo.contains(".mp4") || gifVideo.contains("/photo/1") || gifVideo.contains("vine.co/v/"))));

            getSupportActionBar().setHomeAsUpIndicator(R.drawable.tablet_close);

            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        Utils.setUpTweetTheme(context, settings);

        setContentView(R.layout.tweet_activity_new);

        setUpTheme();

        setUIElements(getWindow().getDecorView().findViewById(android.R.id.content));

        String page = webpages.size() > 0 ? webpages.get(0) : "";

        if (hasWebpage && page.contains("/status/")) {
            final CardView view = (CardView) findViewById(R.id.embedded_tweet_card);
            final long embeddedId = TweetLinkUtils.getTweetIdFromLink(page);

            if (embeddedId != 0l) {
                view.setVisibility(View.INVISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Twitter twitter = getTwitter();

                        try {
                            final Status s = twitter.showStatus(embeddedId);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TweetView v = new TweetView(context, s);
                                    v.setCurrentUser(settings.myScreenName);
                                    v.hideImage(true);

                                    view.addView(v.getView());
                                    view.setVisibility(View.VISIBLE);
                                }
                            });
                        } catch (Exception e) { }
                    }
                }).start();
            }
        }

        findViewById(R.id.extra_content).setVisibility(View.VISIBLE);

        if (youtube) {
            View v = findViewById(R.id.youtube_view);
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                v.setPadding(150,150,150,0);
            }

            YouTubePlayerSupportFragment fragment = new YouTubePlayerSupportFragment();
            fragment.initialize(APIKeys.YOUTUBE_API_KEY,
                    new YouTubePlayer.OnInitializedListener() {

                        @Override
                        public void onInitializationSuccess(YouTubePlayer.Provider arg0,
                                                            YouTubePlayer arg1, boolean arg2) {
                            String url = youtubeVideo;
                            String video = "";
                            try {
                                if (url.contains("youtube")) { // normal youtube link
                                    // first get the youtube video code
                                    int start = url.indexOf("v=") + 2;
                                    int end;
                                    if (url.substring(start).contains("&")) {
                                        end = url.indexOf("&");
                                        video = url.substring(start, end);
                                    } else if (url.substring(start).contains("?")) {
                                        end = url.indexOf("?");
                                        video = url.substring(start, end);
                                    } else {
                                        video = url.substring(start);
                                    }
                                } else { // shortened youtube link
                                    // first get the youtube video code
                                    int start = url.indexOf(".be/") + 4;
                                    int end;
                                    if (url.substring(start).contains("&")) {
                                        end = url.indexOf("&");
                                        video = url.substring(start, end);
                                    } else if (url.substring(start).contains("?")) {
                                        end = url.indexOf("?");
                                        video = url.substring(start, end);
                                    } else {
                                        video = url.substring(start);
                                    }
                                }
                            } catch (Exception e) {
                                video = "";
                            }

                            arg1.loadVideo(video);
                        }

                        @Override
                        public void onInitializationFailure(YouTubePlayer.Provider arg0,
                                                            YouTubeInitializationResult arg1) {
                        }

                    }
            );
            android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.youtube_view, fragment);
            ft.commit();
        }

        VideoView gif = (VideoView) findViewById(R.id.gif);
        Log.v("talon_gif", "gif video: " + gifVideo);
        if (null != gifVideo && !android.text.TextUtils.isEmpty(gifVideo) && (gifVideo.contains(".mp4") || gifVideo.contains("/photo/1") || gifVideo.contains("vine.co/v/"))) {
            getVideo(gif);
        } else {
            findViewById(R.id.spinner).setVisibility(View.GONE);
            gif.setVisibility(View.GONE);
            findViewById(R.id.gif_holder).setVisibility(View.GONE);
        }

        // delay displaying the extra content just a little bit to get rid of some weird animations
        final View extra = findViewById(R.id.extra_content);
        final View name = findViewById(R.id.person_info);
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (extra.getVisibility() != View.VISIBLE) {
                    extra.setVisibility(View.VISIBLE);
                }
                if (name.getVisibility() != View.VISIBLE) {
                    name.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        anim.setStartOffset(400);
        anim.setDuration(300);

        if (settings.fastTransitions || fromLauncher) {
            name.setVisibility(View.VISIBLE);
        } else {
            name.startAnimation(anim);
        }
        /*if (!fromLauncher) {
            extra.startAnimation(anim);
            name.startAnimation(anim);
        } else {
            name.setVisibility(View.VISIBLE);
        }*/


        View nav = findViewById(R.id.landscape_nav_bar);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) nav.getLayoutParams();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && !getResources().getBoolean(R.bool.isTablet)) {
            params.width = (int) (Utils.getNavBarHeight(context) * .9);
        } else {
            params.width = 0;
        }

        nav.setLayoutParams(params);
        nav.setVisibility(View.VISIBLE);
    }

    public void getVideo(final VideoView video) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (gifVideo.contains("vine.co")) {
                    // have to get the html from the page and parse the video from there.

                    gifVideo = getVineLink();
                } else if (gifVideo.contains("/photo/1") && gifVideo.contains("twitter.com/")) {
                    // this is before it was added to the api.
                    // finds the video from the HTML on twitters website.

                    gifVideo = getGifLink();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (gifVideo != null) {

                                final Uri videoUri = Uri.parse(gifVideo);

                                video.setVideoURI(videoUri);
                                video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {
                                        findViewById(R.id.spinner).setVisibility(View.GONE);
                                        video.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                                    }
                                });

                                video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.seekTo(0);
                                        mp.start();
                                    }
                                });

                                video.start();

                                if (gifVideo.contains("video.twimg")) {
                                    MediaController mediaController = new MediaController(context);
                                    mediaController.setAnchorView(video);
                                    video.setMediaController(mediaController);
                                }
                            } else {
                                Toast.makeText(TweetActivity.this, R.string.error_gif, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            // not attached to activity
                        }
                    }
                });

            }
        }).start();
    }

    public Document getDoc() {
        try {
            org.apache.http.client.HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet((gifVideo.contains("http") ? "" : "https://") + gifVideo);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                sb.append(line + "\n");

            String docHtml = sb.toString();

            is.close();

            return Jsoup.parse(docHtml);
        } catch (Exception e) {
            return null;
        }
    }

    public String getGifLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("class", "animated-gif");

                for (Element e : elements) {
                    for (Element x : e.getAllElements()) {
                        if (x.nodeName().contains("source")) {
                            return x.attr("video-src");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getVineLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("property", "twitter:player:stream");

                for (Element e : elements) {
                    return e.attr("content");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    String youtubeVideo = "";

    @Override
    public void onBackPressed() {
        if (youTubeFrag == null || youTubeFrag.onBack()) {
            if (!hidePopups()) {
                hideExtraContent();
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        hidePopups();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        //super.onConfigurationChanged(newConfig);
    }

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        sharedPrefs.edit().putBoolean("from_activity", true).commit();

        if (expansionHelper != null) {
            expansionHelper.stop();
        }

        super.finish();
    }

    public boolean hidePopups() {
        if (picsPopup != null && picsPopup.isShowing()) {
            picsPopup.hide();
            return true;
        } else if (expansionHelper != null && expansionHelper.hidePopups()) {
            return true;
        }

        return false;
    }

    public View insetsBackground;

    public void setUpTheme() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        }

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(new ColorDrawable(android.R.color.transparent));
        actionBar.setTitle("");
        actionBar.setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
        actionBar.setHomeAsUpIndicator(new ColorDrawable(android.R.color.transparent));

        final int abHeight = Utils.getActionBarHeight(context);
        final int sbHeight = Utils.getStatusBarHeight(context);

        if (getResources().getBoolean(R.bool.isTablet)) {
            findViewById(R.id.content).setPadding(0, Utils.toDP(10, context), 0, 0);
        } else {
            findViewById(R.id.content).setPadding(0, sbHeight + Utils.toDP(10, context), 0, 0);
        }

        View status = findViewById(R.id.status_bar);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) status.getLayoutParams();
        params.height = sbHeight;
        status.setLayoutParams(params);

        if (getResources().getBoolean(R.bool.isTablet)) {
            status.setVisibility(View.GONE);
        }

        final NotifyScrollView scroll = (NotifyScrollView) findViewById(R.id.notify_scroll_view);
        scroll.setOnScrollChangedListener(new NotifyScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                /*final int headerHeight = header.getHeight() - abHeight;
                final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
                insetsBackground.setAlpha(ratio);*/
            }
        });
        scroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (view instanceof ImageButton) {
                    return false;
                }
                if (picsPopup != null && picsPopup.isShowing()) {
                    picsPopup.hide();
                    return true;
                } else {
                    return false;
                }
            }
        });

        View navBarSeperator = findViewById(R.id.nav_bar_seperator);
        RelativeLayout.LayoutParams navBarParams = (RelativeLayout.LayoutParams) findViewById(R.id.navigation_bar).getLayoutParams();
        navBarParams.height = Utils.getNavBarHeight(context);
        findViewById(R.id.navigation_bar).setLayoutParams(navBarParams);

        LinearLayout.LayoutParams navBar = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));

        if (Utils.hasNavBar(context) && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            navBar.height = Utils.getNavBarHeight(context);
        } else {
            navBar.height = 0;
        }
        navBarSeperator.setLayoutParams(navBar);

        if (getResources().getBoolean(R.bool.isTablet) ||
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ||
                !Utils.hasNavBar(context)) {
            navBarSeperator.setVisibility(View.GONE);
            findViewById(R.id.navigation_bar).setVisibility(View.GONE);
        }

        if (settings.transpartSystemBars) {
            navBarSeperator.setVisibility(View.INVISIBLE);
            status.setVisibility(View.INVISIBLE);
            findViewById(R.id.navigation_bar).setVisibility(View.INVISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !getResources().getBoolean(R.bool.isTablet)) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.transparent_system_bar));
            }
        }
    }

    public void setUpWindow(boolean youtube) {

        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR | Window.FEATURE_PROGRESS);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        if(!youtube) {
            params.dimAmount = .4f;  // set it higher if you want to dim behind the window
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
            getWindow().setLayout((int) (width * .85), (int) (height * .68));
        } else {
            getWindow().setLayout((int) (width * .6), (int) (height * .8));
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
        secondAcc = from.getBooleanExtra("second_account", false);
        gifVideo = from.getStringExtra("animated_gif");

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
            linkString = from.getStringExtra("other_links");
            otherLinks = linkString.split("  ");
        } catch (Exception e) {
            otherLinks = null;
        }

        if (screenName != null) {
            if (screenName.equals(settings.myScreenName)) {
                isMyTweet = true;
            } else if (screenName.equals(retweeter)) {
                isMyRetweet = true;
            }
        }

        if (hashtags != null) {
            // we will add them to the auto complete
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<String> tags = new ArrayList<String>();
                    if (hashtags != null) {
                        for (String s : hashtags) {
                            if (!s.equals("")) {
                                tags.add("#" + s);
                            }
                        }
                    }


                    HashtagDataSource source = HashtagDataSource.getInstance(context);

                    for (String s : tags) {
                        Log.v("talon_hashtag", "trend: " + s);
                        if (s.contains("#")) {
                            // we want to add it to the auto complete
                            Log.v("talon_hashtag", "adding: " + s);

                            source.deleteTag(s);
                            source.createTag(s);
                        }
                    }
                }
            }).start();
        }
    }

    public Twitter getTwitter() {
        if (secondAcc) {
            return Utils.getSecondTwitter(this);
        } else {
            return Utils.getTwitter(this, settings);
        }
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected void onPreExecute() {
            finish();
        }

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {

                HomeDataSource.getInstance(context).deleteTweet(tweetId);
                MentionsDataSource.getInstance(context).deleteTweet(tweetId);

                try {
                    twitter.destroyStatus(tweetId);
                } catch (Exception x) {

                }

                return true;
            } catch (Exception e) {
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

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).commit();
        }
    }

    class MarkSpam extends AsyncTask<String, Void, Boolean> {

        protected void onPreExecute() {
            finish();
        }

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {
                HomeDataSource.getInstance(context).deleteTweet(tweetId);
                MentionsDataSource.getInstance(context).deleteTweet(tweetId);

                try {
                    twitter.reportSpam(screenName.replace(" ", "").replace("@", ""));
                } catch (Throwable t) {
                    // for somme reason this causes a big "naitive crash" on some devices
                    // with a ton of random letters on play store reports... :/ hmm
                }

                try {
                    twitter.destroyStatus(tweetId);
                } catch (Exception x) {

                }

                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).commit();

                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public CircleImageView profilePic;
    public NetworkedCacheableImageView image;
    public TextView retweetertv;
    public TextView timetv;

    public void setUIElements(final View layout) {
        TextView nametv;
        TextView screennametv;
        TextView tweettv;

        nametv = (TextView) layout.findViewById(R.id.name);
        screennametv = (TextView) layout.findViewById(R.id.screen_name);
        tweettv = (TextView) layout.findViewById(R.id.tweet);
        retweetertv = (TextView) layout.findViewById(R.id.retweeter);
        profilePic = (CircleImageView) layout.findViewById(R.id.profile_pic);
        image = (NetworkedCacheableImageView) layout.findViewById(R.id.image);
        timetv = (TextView) layout.findViewById(R.id.time);

        View.OnClickListener viewPro = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    Intent viewProfile = new Intent(context, ProfilePager.class);
                    viewProfile.putExtra("name", name);
                    viewProfile.putExtra("screenname", screenName);
                    viewProfile.putExtra("proPic", proPic);
                    viewProfile.putExtra("tweetid", tweetId);
                    viewProfile.putExtra("retweet", retweetertv.getVisibility() == View.VISIBLE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getWindow().setExitTransition(null);
                    }

                    context.startActivity(viewProfile);
                }
            }
        };

        ImageUtils.loadImage(context, profilePic, proPic, App.getInstance(context).getBitmapCache());
        profilePic.setOnClickListener(viewPro);

        findViewById(R.id.person_info).setOnClickListener(viewPro);
        nametv.setOnClickListener(viewPro);
        screennametv.setOnClickListener(viewPro);

        if (picture) { // if there is a picture already loaded

            image.loadImage(webpage, false, null);
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!hidePopups()) {
                        if (webpage.contains(" ")) {
                            picsPopup = new MultiplePicsPopup(context, context.getResources().getBoolean(R.bool.isTablet), webpage);
                            picsPopup.setFullScreen();
                            picsPopup.setExpansionPointForAnim(view);
                            picsPopup.show();
                        } else {
                            Intent photo = new Intent(context, PhotoViewerActivity.class).putExtra("url", webpage);
                            photo.putExtra("shared_trans", true);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ActivityOptions options = ActivityOptions
                                        .makeSceneTransitionAnimation(((Activity) context), image, "image");

                                context.startActivity(photo, options.toBundle());
                            } else {
                                context.startActivity(photo);
                            }
                        }
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                image.setClipToOutline(true);
            }

        } else {
            // remove the picture
            image.setVisibility(View.GONE);
        }

        nametv.setText(name);
        screennametv.setText("@" + screenName);
        tweettv.setText(tweet);
        tweettv.setTextIsSelectable(true);

        if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            if (EmojiUtils.emojiPattern.matcher(tweet).find()) {
                final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweet.replaceAll("\n", "<br/>")));
                tweettv.setText(span);
            }
        }

        //Date tweetDate = new Date(time);
        String timeDisplay;

        if (!settings.militaryTime) {
            timeDisplay = android.text.format.DateFormat.getDateFormat(context).format(time) + " " +
                    android.text.format.DateFormat.getDateFormat(context).format(time);
        } else {
            timeDisplay = new SimpleDateFormat("kk:mm").format(time).replace("24:", "00:") + " " +
                    android.text.format.DateFormat.getDateFormat(context).format(time);
        }

        timetv.setText(timeDisplay);

        if (retweeter != null && retweeter.length() > 0) {
            retweetertv.setText(getResources().getString(R.string.retweeter) + retweeter);
            retweetertv.setVisibility(View.VISIBLE);
        }

        String text = tweet;
        String extraNames = "";

        String screenNameToUse;

        if (secondAcc) {
            screenNameToUse = settings.secondScreenName;
        } else {
            screenNameToUse = settings.myScreenName;
        }

        if (text.contains("@")) {
            for (String s : users) {
                if (!s.equals(screenNameToUse) && !extraNames.contains(s)  && !s.equals(screenName)) {
                    extraNames += "@" + s + " ";
                }
            }
        }

        if (retweeter != null && !retweeter.equals("") && !retweeter.equals(screenNameToUse) && !extraNames.contains(retweeter)) {
            extraNames += "@" + retweeter + " ";
        }

        // lets get a little scale in animation on that fab
        final LinearLayout content = (LinearLayout) findViewById(R.id.content);
        content.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                content.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fade_in));
                content.setVisibility(View.VISIBLE);
            }
        }, 200);

        // last bool is whether it should open in the external browser or not
        TextUtils.linkifyText(context, retweetertv, null, true, linkString, true);
        TextUtils.linkifyText(context, tweettv, null, true, linkString, true);

        String replyStuff = "";
        if (!screenName.equals(screenNameToUse)) {
            replyStuff = "@" + screenName + " " + extraNames;
        } else {
            replyStuff = extraNames;
        }

        expansionHelper = new ExpansionViewHelper(context, tweetId, getResources().getBoolean(R.bool.isTablet));
        expansionHelper.setSecondAcc(secondAcc);
        expansionHelper.setBackground(findViewById(R.id.notify_scroll_view));
        expansionHelper.setWebLink(otherLinks);
        expansionHelper.setReplyDetails("@" + screenName + ": " + text, replyStuff);
        expansionHelper.setUser(screenName);
        expansionHelper.setText(text);
        expansionHelper.setUpOverflow();

        LinearLayout ex = (LinearLayout) findViewById(R.id.expansion_area);
        ex.addView(expansionHelper.getExpansion());
        expansionHelper.startFlowAnimation();
    }

    private ExpansionViewHelper expansionHelper;

    public void hideExtraContent() {
        final LinearLayout extra = (LinearLayout) findViewById(R.id.extra_content);
        final View name = findViewById(R.id.person_info);
        if (extra.getVisibility() == View.VISIBLE) {
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    extra.setVisibility(View.INVISIBLE);
                    //back.setVisibility(View.INVISIBLE);
                    name.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            anim.setDuration(250);
            if (!fromLauncher) {
                name.startAnimation(anim);
                extra.startAnimation(anim);
            }
        }
    }

    private Status status = null;
    private MultiplePicsPopup picsPopup;

    public Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
