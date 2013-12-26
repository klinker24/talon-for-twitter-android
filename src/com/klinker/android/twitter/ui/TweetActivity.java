package com.klinker.android.twitter.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.manipulations.ExpansionAnimation;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.ui.drawer_activities.trends.SearchedTrendsActivity;
import com.klinker.android.twitter.ui.widgets.EmojiKeyboard;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.QustomDialogBuilder;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.utils.EmojiUtils;
import com.klinker.android.twitter.utils.HtmlUtils;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.photoview.PhotoViewAttacher;

public class TweetActivity extends YouTubeBaseActivity implements
        YouTubePlayer.OnInitializedListener {

    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

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

    private TextView timetv;

    private WebView website;
    private NetworkedCacheableImageView pictureIv;

    private ImageView attachImage;
    private String attachedFilePath = "";

    private boolean isMyTweet = false;
    private boolean isMyRetweet = true;

    private ImageButton emojiButton;
    private EmojiKeyboard emojiKeyboard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        settings = new AppSettings(context);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        getFromIntent();

        setUpTheme();

        if ((settings.advanceWindowed || getIntent().getBooleanExtra("from_widget", false)) && !webpage.contains("youtu")) {
            setUpWindow();
        }

        setContentView(R.layout.tweet_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);

        setUIElements();

    }

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight_Popup);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark_Popup);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack_Popup);
                break;
        }

    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window
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

    PhotoViewAttacher mAttacher;

    public void setUIElements() {
        TextView nametv = (TextView) findViewById(R.id.name);
        TextView screennametv = (TextView) findViewById(R.id.screen_name);
        TextView tweettv = (TextView) findViewById(R.id.tweet);
        timetv = (TextView) findViewById(R.id.time);
        final TextView retweetertv = (TextView) findViewById(R.id.retweeter);
        website = (WebView) findViewById(R.id.webview);
        pictureIv = (NetworkedCacheableImageView) findViewById(R.id.imageView);
        final AsyncListView replyList = (AsyncListView) findViewById(R.id.reply_list);
        LinearLayout progressSpinner = (LinearLayout) findViewById(R.id.list_progress);
        final LinearLayout background = (LinearLayout) findViewById(R.id.linLayout);
        final ImageButton expand = (ImageButton) findViewById(R.id.switchViews);

        final NetworkedCacheableImageView profilePic = (NetworkedCacheableImageView) findViewById(R.id.profile_pic);

        final ImageButton favoriteButton = (ImageButton) findViewById(R.id.favorite);
        final ImageButton retweetButton = (ImageButton) findViewById(R.id.retweet);
        final TextView favoriteCount = (TextView) findViewById(R.id.fav_count);
        final TextView retweetCount = (TextView) findViewById(R.id.retweet_count);
        final EditText reply = (EditText) findViewById(R.id.reply);
        final ImageButton replyButton = (ImageButton) findViewById(R.id.reply_button);
        ImageButton attachButton = (ImageButton) findViewById(R.id.attach_button);

        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);

        emojiButton = (ImageButton) findViewById(R.id.emoji);
        emojiKeyboard = (EmojiKeyboard) findViewById(R.id.emojiKeyboard);

        nametv.setTextSize(settings.textSize +2);
        screennametv.setTextSize(settings.textSize);
        tweettv.setTextSize(settings.textSize);
        timetv.setTextSize(settings.textSize - 3);
        retweetertv.setTextSize(settings.textSize - 3);
        favoriteCount.setTextSize(settings.textSize + 1);
        retweetCount.setTextSize(settings.textSize + 1);
        reply.setTextSize(settings.textSize);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        replyList.setItemManager(builder.build());

        final ImageButton overflow = (ImageButton) findViewById(R.id.overflow_button);
        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout buttons = (LinearLayout) findViewById(R.id.buttons);
                if (buttons.getVisibility() == View.VISIBLE) {

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_left);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);

                    buttons.setVisibility(View.GONE);
                } else {
                    buttons.setVisibility(View.VISIBLE);

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);
                }
            }
        });

        if (settings.theme == 0) {
            nametv.setTextColor(getResources().getColor(android.R.color.black));
            nametv.setShadowLayer(0,0,0, getResources().getColor(android.R.color.transparent));
            screennametv.setTextColor(getResources().getColor(android.R.color.black));
            screennametv.setShadowLayer(0,0,0, getResources().getColor(android.R.color.transparent));
        }

        if (name.contains(settings.myName)) {
            reply.setVisibility(View.GONE);
            replyButton.setVisibility(View.GONE);
            attachButton.setVisibility(View.GONE);
            attachButton.setEnabled(false);
            favoriteButton.setEnabled(false);
            retweetButton.setEnabled(false);

        }

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, UserProfileActivity.class);
                viewProfile.putExtra("name", name);
                viewProfile.putExtra("screenname", screenName);
                viewProfile.putExtra("proPic", proPic);
                viewProfile.putExtra("tweetid", tweetId);
                viewProfile.putExtra("retweet", retweetertv.getVisibility() == View.VISIBLE);

                context.startActivity(viewProfile);
            }
        });

        if (webpage.contains("youtu")) { // there is a youtube video
            youTubeView.setVisibility(View.VISIBLE);
            youTubeView.initialize(settings.YOUTUBE_API_KEY, this);
        } else if (!webpage.equals("") && !picture) { // If there is a web page that isn't a picture already loaded

            progressSpinner.setVisibility(View.GONE);
            website.setVisibility(View.VISIBLE);
            website.getSettings().setJavaScriptEnabled(true);
            website.getSettings().setBuiltInZoomControls(true);
            website.clearCache(true);

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
                    Toast.makeText(activity, getResources().getString(R.string.error_loading_page), Toast.LENGTH_SHORT).show();
                }
            });

            website.loadUrl(webpage);

        } else if(picture) { // if there is a picture already loaded

            progressSpinner.setVisibility(View.GONE);
            pictureIv.setVisibility(View.VISIBLE);

            pictureIv.loadImage(webpage, false, null);

            mAttacher = new PhotoViewAttacher(pictureIv);

        } else { // just show the replys
            progressSpinner.setVisibility(View.VISIBLE);
        }

        if (website.getVisibility() == View.VISIBLE || pictureIv.getVisibility() == View.VISIBLE || youTubeView.getVisibility() == View.VISIBLE) {
            expand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(background.getVisibility() == View.VISIBLE) {
                        Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                        ranim.setFillAfter(true);
                        expand.startAnimation(ranim);
                    } else {
                        Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                        ranim.setFillAfter(true);
                        expand.startAnimation(ranim);
                    }

                    ExpansionAnimation expandAni = new ExpansionAnimation(background, 450);
                    background.startAnimation(expandAni);
                }
            });

            if (website.getVisibility() == View.VISIBLE || youTubeView.getVisibility() == View.VISIBLE) {
                new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    expand.performClick();
                }
            }, 500);
            }
        } else {
            expand.setVisibility(View.GONE);
        }

        nametv.setText(name);
        screennametv.setText("@" + screenName);
        tweettv.setText(Html.fromHtml(tweet));

        if (settings.useEmoji && (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT || EmojiUtils.ios)) {
            if (EmojiUtils.emojiPattern.matcher(tweet).find()) {
                final Spannable span = EmojiUtils.getSmiledText(context, Html.fromHtml(tweet));
                tweettv.setText(span);
            }
        }

        // sets the click listener to display the dialog for the highlighted text
        if (tweet.contains("<font")) {
            tweettv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    ArrayList<String> strings = new ArrayList<String>();

                    if (users != null) {
                        for (String s : users) {
                            if (!s.equals("")) {
                                strings.add("@" + s);
                            }
                        }
                    }
                    if (hashtags != null) {
                        for (String s : hashtags) {
                            if (!s.equals("")) {
                                strings.add(s);
                            }
                        }
                    }
                    if (otherLinks != null) {
                        for (String s : otherLinks) {
                            if (!s.equals("")) {
                                strings.add(s);
                            }
                        }
                    }
                    if (!webpage.equals("") && !webpage.contains("youtu")) {
                        strings.add(webpage);
                    }

                    CharSequence[] items = new CharSequence[strings.size()];

                    for (int i = 0; i < items.length; i++) {
                        String s = strings.get(i);
                        if (s != null) {
                            items[i] = s;
                        }
                    }

                    final CharSequence[] fItems = items;

                    if (fItems.length > 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String touched = fItems[item] + "";

                                if (touched.contains("http")) { //weblink
                                    Uri weburi = Uri.parse(touched);
                                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                                    startActivity(launchBrowser);
                                } else if (touched.contains("@")) { //username
                                    Intent user = new Intent(context, UserProfileActivity.class);
                                    user.putExtra("screenname", touched.replace("@", ""));
                                    user.putExtra("proPic", "");
                                    context.startActivity(user);
                                } else { // hashtag
                                    Intent search = new Intent(context, SearchedTrendsActivity.class);
                                    search.setAction(Intent.ACTION_SEARCH);
                                    search.putExtra(SearchManager.QUERY, touched);
                                    context.startActivity(search);
                                }
                                dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        String touched = fItems[0] + "";

                        if (touched.contains("http")) { //weblink
                            Uri weburi = Uri.parse(touched);
                            Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                            startActivity(launchBrowser);
                        } else if (touched.contains("@")) { //username
                            Intent user = new Intent(context, UserProfileActivity.class);
                            user.putExtra("screenname", touched.replace("@", ""));
                            user.putExtra("proPic", "");
                            context.startActivity(user);
                        } else { // hashtag
                            Intent search = new Intent(context, SearchedTrendsActivity.class);
                            search.setAction(Intent.ACTION_SEARCH);
                            search.putExtra(SearchManager.QUERY, touched);
                            context.startActivity(search);
                        }
                    }
                }
            });
        }

        //Date tweetDate = new Date(time);
        String timeDisplay;

        if (!settings.militaryTime) {
             timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(time);
        } else {
            timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(time);
        }

        timetv.setText(timeDisplay);

        if (retweeter.length() > 0 ) {
            retweetertv.setText(getResources().getString(R.string.retweeter) + retweeter);
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

        profilePic.loadImage(proPic, false, null, NetworkedCacheableImageView.CIRCLE);

        new GetFavoriteCount(favoriteCount, favoriteButton, tweetId).execute();
        new GetRetweetCount(retweetCount, tweetId).execute();
        new GetReplies(replyList, screenName, tweetId, progressSpinner, expand, background).execute();

        String text = tweet;
        String extraNames = "";

        if (text.contains("@")) {
            for (String s : users) {
                if (!s.equals(settings.myScreenName)) {
                    extraNames += "@" + s + " ";
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

        attachImage = (ImageView) findViewById(R.id.attach);

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                //builder.setTitle(getResources().getString(R.string.open_what) + "?");
                builder.setItems(R.array.attach_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if(item == 0) { // take picture
                            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            File f = new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg");

                            if (!f.exists()) {
                                try {
                                    f.getParentFile().mkdirs();
                                    f.createNewFile();
                                } catch (IOException e) {

                                }
                            }

                            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                            startActivityForResult(captureIntent, CAPTURE_IMAGE);
                        } else { // attach picture
                            if (attachedFilePath.equals("")) {
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            } else {
                                attachedFilePath = "";

                                TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.attachButton});
                                int resource = a.getResourceId(0, 0);
                                a.recycle();
                                attachImage.setImageDrawable(context.getResources().getDrawable(resource));
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            }
                        }

                        overflow.performClick();
                    }
                });

                builder.create().show();
            }
        });

        final TextView charRemaining = (TextView) findViewById(R.id.char_remaining);
        charRemaining.setText(140 - reply.getText().length() + "");

        if (isMyTweet) {
            charRemaining.setVisibility(View.GONE);
            overflow.setVisibility(View.GONE);
        }

        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                charRemaining.setText(140 - reply.getText().length() - (attachedFilePath.equals("") ? 0 : 22) + "");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        if (!settings.useEmoji) {
            emojiButton.setVisibility(View.GONE);
        } else {
            emojiKeyboard.setAttached((HoloEditText) reply);

            reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_emoji_keyboard_dark));
                    }
                }
            });

            emojiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager imm = (InputMethodManager)getSystemService(
                                        Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(reply, 0);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_emoji_keyboard_dark));
                    } else {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                emojiKeyboard.setVisibility(true);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.keyboardButton});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_light));
                    }
                }
            });
        }

        Button at = (Button) findViewById(R.id.at_button);
        at.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context, sharedPrefs.getInt("current_account", 1)).
                        setTitle(getResources().getString(R.string.type_user)).
                        setTitleColor(getResources().getColor(R.color.app_color)).
                        setDividerColor(getResources().getColor(R.color.app_color));

                qustomDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                qustomDialogBuilder.setPositiveButton(getResources().getString(R.string.add_user), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reply.append(qustomDialogBuilder.text.getText().toString());
                    }
                });

                qustomDialogBuilder.show();

                overflow.performClick();
            }
        });

    }

    private boolean isFavorited = false;
    private boolean isRetweet = false;

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean b) {
        String video;
        if (webpage.contains("youtube")) { // normal youtube link
            // first get the youtube video code
            int start = webpage.indexOf("v=") + 2;
            int end = webpage.length();
            if (webpage.substring(start).contains("&")) {
                end = webpage.indexOf("&");
            }
            video = webpage.substring(start, end);
        } else { // shortened youtube link
            // first get the youtube video code
            int start = webpage.indexOf(".be/") + 4;
            int end = webpage.length();
            if (webpage.substring(start).contains("&")) {
                end = webpage.indexOf("&");
            }
            video = webpage.substring(start, end);
        }

        player.loadVideo(video);
        player.setShowFullscreenButton(false);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Toast.makeText(context, getResources().getString(R.string.youtube_error), Toast.LENGTH_SHORT).show();
        YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        youTubeView.setVisibility(View.GONE);

        website.setVisibility(View.VISIBLE);
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
                Toast.makeText(activity, getResources().getString(R.string.error_loading_page), Toast.LENGTH_SHORT).show();
            }
        });

        website.loadUrl(webpage);
    }

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
        private String via = "";

        public GetRetweetCount(TextView retweetCount, long tweetId) {
            this.retweetCount = retweetCount;
            this.tweetId = tweetId;
        }

        protected String doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);
                twitter4j.Status status = twitter.showStatus(tweetId);

                via = android.text.Html.fromHtml(status.getSource()).toString();

                return "" + status.getRetweetCount();
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(String count) {
            if (count != null) {
                retweetCount.setText("- " + count);
            }

            try {
                if (!timetv.getText().toString().contains(getResources().getString(R.string.via))) {
                    timetv.append(" " + getResources().getString(R.string.via) + " " + via);
                }
            } catch (Exception e) {

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

    public void removeKeyboard(EditText reply) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);
    }

    class ReplyToStatus extends AsyncTask<String, Void, Boolean> {

        private long tweetId;
        private EditText message;
        private boolean messageToLong = false;

        public ReplyToStatus(EditText message, long tweetId) {
            this.message = message;
            this.tweetId = tweetId;
        }

        protected void onPreExecute() {
            removeKeyboard(message);
            Toast.makeText(context, getResources().getString(R.string.sending) + "...", Toast.LENGTH_SHORT);
        }

        protected Boolean doInBackground(String... urls) {
            try {
                if (message.getText().length() + (attachedFilePath.equals("") ? 0 : 22) <= 140) {
                    Twitter twitter =  Utils.getTwitter(context);

                    twitter4j.StatusUpdate reply = new twitter4j.StatusUpdate(message.getText().toString());
                    reply.setInReplyToStatusId(tweetId);

                    if (!attachedFilePath.equals("")) {
                        reply.setMedia(new File(attachedFilePath));
                    }

                    twitter.updateStatus(reply);
                    return true;
                } else {
                    messageToLong = true;
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean sent) {
            if (sent) {
                Toast.makeText(context, context.getResources().getString(R.string.tweet_success), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                if (messageToLong) {
                    Toast.makeText(context, context.getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getResources().getString(R.string.error_sending_tweet), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    class GetReplies extends AsyncTask<String, Void, ArrayList<Status>> {

        private String username;
        private ListView listView;
        private long tweetId;
        private LinearLayout progressSpinner;
        private LinearLayout background;
        private ImageButton expand;

        public GetReplies(ListView listView, String username, long tweetId, LinearLayout progressBar, ImageButton expand, LinearLayout background) {
            this.listView = listView;
            this.username = username;
            this.tweetId = tweetId;
            this.progressSpinner = progressBar;
            this.expand = expand;
            this.background = background;
        }

        protected ArrayList<twitter4j.Status> doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context);
            try {
                twitter4j.Status status = twitter.showStatus(tweetId);

                twitter4j.Status replyStatus = twitter.showStatus(status.getInReplyToStatusId());

                ArrayList<twitter4j.Status> replies = new ArrayList<twitter4j.Status>();

                try {
                    while(!replyStatus.getText().equals("")) {
                        replies.add(replyStatus);
                        Log.v("reply_status", replyStatus.getText());

                        replyStatus = twitter.showStatus(replyStatus.getInReplyToStatusId());
                    }
                } catch (Exception e) {
                    // the list of replies has ended, but we dont want to go to null
                }

                return replies;

            } catch (TwitterException e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<twitter4j.Status> replies) {
            progressSpinner.setVisibility(View.GONE);

            try {
                if (replies.size() > 0) {
                    listView.setAdapter(new TimelineArrayAdapter(context, replies));
                    expand.setVisibility(View.VISIBLE);
                    expand.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if(background.getVisibility() == View.VISIBLE) {
                                Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate);
                                ranim.setFillAfter(true);
                                expand.startAnimation(ranim);
                            } else {
                                Animation ranim = AnimationUtils.loadAnimation(context, R.anim.rotate_back);
                                ranim.setFillAfter(true);
                                expand.startAnimation(ranim);
                            }

                            ExpansionAnimation expandAni = new ExpansionAnimation(background, 450);
                            background.startAnimation(expandAni);
                        }
                    });
                } else {

                }
            } catch (Exception e) {
                // none and it got the null object
            }

            if(!picture && webpage.equals("")) {
                listView.setVisibility(View.VISIBLE);
            }
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

        if (website.getVisibility() != View.VISIBLE) {
            menu.getItem(MENU_OPEN_WEB).setVisible(false);
        }

        if (pictureIv.getVisibility() != View.VISIBLE) {
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
                ClipData clip = ClipData.newPlainText("tweet_text", tweet);
                clipboard.setPrimaryClip(clip);
                return true;

            case R.id.menu_open_web:
                Uri weburi = Uri.parse(webpage);
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                startActivity(launchBrowser);

                return true;

            case R.id.menu_save_image:

                Bitmap bitmap = ((BitmapDrawable)pictureIv.getDrawable()).getBitmap();

                Random generator = new Random();
                int n = 10000;
                n = generator.nextInt(n);
                String fname = "Image-" + n;

                IOUtils.saveImage(bitmap, fname, context);

                return true;

            case R.id.menu_quote:

                String[] split = tweet.split(" ");
                String placeholder = "";

                for(String s : split) {
                    if (s.contains("http:")) {
                        placeholder += webpage + " ";
                    } else if (s.equals("<font")) {

                    } else {
                        placeholder += s + " ";
                    }
                }

                String text = "\"@" + screenName + ": " + placeholder + "\" ";

                text = HtmlUtils.removeColorHtml(text);

                Intent quote = new Intent(context, ComposeActivity.class);
                quote.putExtra("user", text);
                startActivity(quote);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static final int SELECT_PHOTO = 100;
    private static final int CAPTURE_IMAGE = 101;

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filePath = IOUtils.getPath(selectedImage, context);

                    Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                    attachImage.setImageBitmap(yourSelectedImage);
                    attachImage.setVisibility(View.VISIBLE);

                    attachedFilePath = filePath;
                }
                break;
            case CAPTURE_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg"));
                        String filePath = selectedImage.getPath();
                        //String filePath = IOUtils.getPath(selectedImage, context);
                        Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                        attachImage.setImageBitmap(yourSelectedImage);
                        attachImage.setVisibility(View.VISIBLE);

                        attachedFilePath = filePath;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(this, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        attachImage.setVisibility(View.GONE);
                    }
                }
                break;
        }
    }
}
