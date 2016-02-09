package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.animation.ValueAnimator;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.*;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.*;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.*;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.klinker.android.sliding.MultiShrinkScroller;
import com.klinker.android.sliding.SlidingActivity;
import com.klinker.android.twitter_l.APIKeys;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.*;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.manipulations.photo_viewer.VideoViewerActivity;
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

public class TweetActivity extends SlidingActivity {

    private static final long NETWORK_ACTION_DELAY = 200;

    public static final String USE_EXPANSION = "use_expansion";
    public static final String EXPANSION_DIMEN_LEFT_OFFSET = "left_offset";
    public static final String EXPANSION_DIMEN_TOP_OFFSET = "top_offset";
    public static final String EXPANSION_DIMEN_WIDTH = "view_width";
    public static final String EXPANSION_DIMEN_HEIGHT = "view_height";

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

    private boolean sharedTransition = false;

    @Override
    public void init(Bundle savedInstanceState) {

        context = this;
        settings = AppSettings.getInstance(this);
        sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

        disableHeader();
        setPrimaryColors(settings.themeColors.primaryColor, settings.themeColors.primaryColorDark);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenHeight = size.y;
        int screenWidth = size.x;

        Intent intent = getIntent();

        if (getIntent().getBooleanExtra(USE_EXPANSION, false)) {
            enableFullscreen();
        }

        expandFromPoints(
                intent.getIntExtra(EXPANSION_DIMEN_LEFT_OFFSET, 0),
                intent.getIntExtra(EXPANSION_DIMEN_TOP_OFFSET, screenHeight),
                intent.getIntExtra(EXPANSION_DIMEN_WIDTH, screenWidth),
                intent.getIntExtra(EXPANSION_DIMEN_HEIGHT, 0)
        );

        if (!sharedPrefs.getBoolean("knows_about_tweet_swipedown", false)) {
            sharedPrefs.edit().putBoolean("knows_about_tweet_swipedown", true).commit();
            Snackbar.make(findViewById(android.R.id.content), R.string.tell_about_swipe_down, Snackbar.LENGTH_LONG).show();
        }


        if (getIntent().getBooleanExtra("share_trans", false)) {
            sharedTransition = false;
        }

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
                if (s.contains("youtu") &&
                        !(s.contains("channel") || s.contains("user") || s.contains("playlist"))) {
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
            String web = webpages.get(0);
            if (web.contains(tweetId + "/photo/1") ||
                    web.contains("vine.co/v/") ||
                    web.contains("amp.twimg.com/v/") ||
                    web.contains("snpy.tv")) {
                hasWebpage = false;
                gifVideo = webpages.get(0);
            }
        }

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

        setContent(R.layout.tweet_activity_new);

        if (settings.darkTheme) {
            findViewById(R.id.content_scroller).setBackgroundColor(getResources().getColor(R.color.dark_background));
        } else if (settings.blackTheme){
            findViewById(R.id.content_scroller).setBackgroundColor(getResources().getColor(R.color.black_background));
        } else {
            findViewById(R.id.content_scroller).setBackgroundColor(getResources().getColor(R.color.light_background));
        }

        if (youtube ||
                (null != gifVideo && !android.text.TextUtils.isEmpty(gifVideo) &&
                        (gifVideo.contains(".mp4") ||
                                gifVideo.contains("/photo/1") ||
                                gifVideo.contains("vine.co/v/") ||
                                gifVideo.contains("amp.twimg.com/v/") ||
                                gifVideo.contains("snpy.tv")))) {
            displayPlayButton = true;
        }

        setUpTheme();
        setUIElements(getWindow().getDecorView().findViewById(android.R.id.content));


        String page = webpages.size() > 0 ? webpages.get(0) : "";
        String embedded = page;

        for (int i = 0; i < webpages.size(); i++) {
            if (webpages.get(i).contains("/status/")) {
                embedded = webpages.get(i);
            }
        }

        if (hasWebpage && embedded.contains("/status/")) {
            final CardView view = (CardView) findViewById(R.id.embedded_tweet_card);
            final long embeddedId = TweetLinkUtils.getTweetIdFromLink(embedded);

            if (embeddedId != 0l) {
                view.setVisibility(View.INVISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Twitter twitter = getTwitter();

                        try {
                            Thread.sleep(NETWORK_ACTION_DELAY);
                        } catch (Exception e) { }

                        try {
                            final Status s = twitter.showStatus(embeddedId);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TweetView v = new TweetView(context, s);
                                    v.setCurrentUser(settings.myScreenName);
                                    v.setSmallImage(true);

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

        final VideoView gif = (VideoView) findViewById(R.id.gif);
        Log.v("talon_gif", "gif video: " + gifVideo);
        findViewById(R.id.spinner).setVisibility(View.INVISIBLE);
        gif.setVisibility(View.GONE);
        findViewById(R.id.gif_holder).setVisibility(View.GONE);
    }

    boolean displayPlayButton = false;
    public VideoView video;
    public boolean videoError = false;

    String youtubeVideo = "";

    @Override
    public void onBackPressed() {
        if (!hidePopups()) {
            super.onBackPressed();
        }
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
        actionBar.setIcon(new ColorDrawable(Color.TRANSPARENT));
        actionBar.setTitle("");
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        actionBar.setHomeAsUpIndicator(new ColorDrawable(Color.TRANSPARENT));

        final View content = findViewById(R.id.content);
        content.setOnTouchListener(new View.OnTouchListener() {
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

                    try {
                        Thread.sleep(NETWORK_ACTION_DELAY);
                    } catch (Exception e) {

                    }

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
                        if (s.contains("#") && source != null) {
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

    private void setTransitionNames() {
        profilePic.setTransitionName("pro_pic");
        nametv.setTransitionName("name");
        screennametv.setTransitionName("screen_name");
        tweettv.setTransitionName("tweet");
        image.setTransitionName("image");
    }

    public CircleImageView profilePic;
    public NetworkedCacheableImageView image;
    public HoloTextView retweetertv;
    public HoloTextView timetv;
    public HoloTextView nametv;
    public HoloTextView screennametv;
    public HoloTextView tweettv;

    public void setUIElements(final View layout) {

        //findViewById(R.id.content_container).setPadding(0,0,0,0);

        nametv = (HoloTextView) layout.findViewById(R.id.name);
        screennametv = (HoloTextView) layout.findViewById(R.id.screen_name);
        tweettv = (HoloTextView) layout.findViewById(R.id.tweet);
        retweetertv = (HoloTextView) layout.findViewById(R.id.retweeter);
        profilePic = (CircleImageView) layout.findViewById(R.id.profile_pic);
        image = (NetworkedCacheableImageView) layout.findViewById(R.id.image);
        timetv = (HoloTextView) layout.findViewById(R.id.time);

        tweettv.setTextSize(settings.textSize);
        screennametv.setTextSize(settings.textSize - 2);
        nametv.setTextSize(settings.textSize + 4);
        timetv.setTextSize(settings.textSize - 3);

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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ImageUtils.loadImage(context, profilePic, proPic, App.getInstance(context).getBitmapCache());
            }
        }, NETWORK_ACTION_DELAY);
        profilePic.setOnClickListener(viewPro);

        findViewById(R.id.person_info).setOnClickListener(viewPro);
        nametv.setOnClickListener(viewPro);
        screennametv.setOnClickListener(viewPro);

        if (picture || displayPlayButton) { // if there is a picture already loaded (or we have a vine/twimg video)

            if (displayPlayButton &&
                    (gifVideo.contains("vine") ||
                            gifVideo.contains("amp.twimg.com/v/") ||
                            gifVideo.contains("snpy.tv"))) {
                image.setBackgroundResource(android.R.color.black);
            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        image.loadImage(webpage, false, null);
                    }
                }, NETWORK_ACTION_DELAY);
            }

            if (displayPlayButton) {
                findViewById(R.id.play_button).setVisibility(View.VISIBLE);
            }

            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!hidePopups()) {
                        if (displayPlayButton) {
                            if (gifVideo != null &&
                                    (gifVideo.contains("amp.twimg.com") ||
                                     gifVideo.contains("snpy.tv"))) {
                                // I cant figure out how to play these for the life of me...
                                new WebIntentBuilder(context)
                                        .setUrl(gifVideo)
                                        .build().start();
                            } else {
                                String links = "";
                                for (String s : otherLinks) {
                                    links += s + "  ";
                                }

                                VideoViewerActivity.startActivity(context, tweetId, gifVideo, links);
                            }
                        } else if (webpage.contains(" ")) {
                            /*picsPopup = new MultiplePicsPopup(context, webpage);
                            picsPopup.setFullScreen();
                            picsPopup.setExpansionPointForAnim(view);
                            picsPopup.show();*/

                            PhotoPagerActivity.startActivity(context, tweetId, webpage, 0);

                        } else {
                            PhotoViewerActivity.startActivity(context, tweetId, webpage, image);
                        }
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                image.setClipToOutline(true);
            }

            ViewGroup.LayoutParams layoutParams = image.getLayoutParams();
            layoutParams.height = (int) getResources().getDimension(R.dimen.header_condensed_height);
            image.setLayoutParams(layoutParams);

        } else {
            // remove the picture
            image.setVisibility(View.GONE);
        }

        nametv.setText(name);
        screennametv.setText("@" + screenName);

        boolean replace = false;
        boolean embeddedTweetFound = TweetView.embeddedTweetPattern.matcher(tweet).find();

        if (settings.inlinePics && (tweet.contains("pic.twitter.com/") || embeddedTweetFound)) {
            if (tweet.lastIndexOf(".") == tweet.length() - 1) {
                replace = true;
            }
        }

        try {
            tweettv.setText(replace ?
                    tweet.substring(0, tweet.length() - (embeddedTweetFound ? 33 : 25)) :
                    tweet);
        } catch (Exception e) {
            tweettv.setText(tweet);
        }
        tweettv.setTextIsSelectable(true);

        if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            if (EmojiUtils.emojiPattern.matcher(tweet).find()) {
                final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweet.replaceAll("\n", "<br/>")));
                tweettv.setText(span);
            }
        }

        //Date tweetDate = new Date(time);
        setTime(time);

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTransitionNames();
        }

        // last bool is whether it should open in the external browser or not
        TextUtils.linkifyText(context, retweetertv, null, true, linkString, false);
        TextUtils.linkifyText(context, tweettv, null, true, linkString, false);

        String replyStuff = "";
        if (!screenName.equals(screenNameToUse)) {
            replyStuff = "@" + screenName + " " + extraNames;
        } else {
            replyStuff = extraNames;
        }

        expansionHelper = new ExpansionViewHelper(context, tweetId, getResources().getBoolean(R.bool.isTablet));
        expansionHelper.setSecondAcc(secondAcc);
        expansionHelper.setBackground(findViewById(R.id.content));
        expansionHelper.setWebLink(otherLinks);
        expansionHelper.setReplyDetails("@" + screenName + ": " + text, replyStuff);
        expansionHelper.setUser(screenName);
        expansionHelper.setText(text);
        expansionHelper.setVideoDownload(gifVideo);
        expansionHelper.setUpOverflow();
        expansionHelper.setLoadCallback(new ExpansionViewHelper.TweetLoaded() {
            @Override
            public void onLoad(Status status) {
                setTime(status.getCreatedAt().getTime());
            }
        });

        LinearLayout ex = (LinearLayout) findViewById(R.id.expansion_area);
        ex.addView(expansionHelper.getExpansion());
        expansionHelper.startFlowAnimation();
    }

    private void setTime(long time) {
        String timeDisplay;

        if (!settings.militaryTime) {
            timeDisplay = android.text.format.DateFormat.getTimeFormat(context).format(time) + "\n" +
                    android.text.format.DateFormat.getDateFormat(context).format(time);
        } else {
            timeDisplay = new SimpleDateFormat("kk:mm").format(time).replace("24:", "00:") + "\n" +
                    android.text.format.DateFormat.getDateFormat(context).format(time);
        }

        timetv.setText(timeDisplay);
    }

    private ExpansionViewHelper expansionHelper;
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
