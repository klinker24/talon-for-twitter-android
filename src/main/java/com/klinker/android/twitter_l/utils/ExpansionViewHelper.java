package com.klinker.android.twitter_l.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ArrayListLoader;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.*;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeSecAccActivity;
import com.r0adkll.slidr.model.SlidrInterface;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import lombok.Setter;
import twitter4j.*;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExpansionViewHelper {

    public interface TweetLoaded {
        void onLoad(Status status);
    }

    private TweetLoaded loadedCallback;
    public void setLoadCallback(TweetLoaded callback) {
        this.loadedCallback = callback;
    }

    private static final long NETWORK_ACTION_DELAY = 200;

    Context context;
    AppSettings settings;
    public long id;

    // root view
    View expansion;

    // background that touching will dismiss the popups
    View background;

    // manage the favorite stuff
    TextView favCount;
    TextView favText;
    ImageView favoriteIcon;
    View favoriteButton; // linear layout

    // manage the retweet stuff
    TextView retweetCount;
    TextView retweetText;
    ImageView retweetIcon;
    View retweetButton; // linear layout
    ImageView retweeters;
    ImageView favoriters;
    View viewRetweeters;
    View viewFavoriters;

    // buttons at the bottom
    ImageButton webButton;
    Button repliesButton;
    View composeButton;
    View overflowButton;
    View quoteButton;

    AsyncListView replyList;
    LinearLayout convoSpinner;
    View convoLayout;

    HoloTextView tweetSource;

    RetweetersPopupLayout retweetersPopup;
    FavoritersPopupLayout favoritersPopup;
    ConversationPopupLayout convoPopup;
    MobilizedWebPopupLayout mobilizedPopup;
    WebPopupLayout webPopup;

    ProgressBar convoProgress;
    RelativeLayout convoArea;
    CardView convoCard;
    CardView embeddedTweetCard;
    LinearLayout convoTweetArea;

    boolean landscape;

    public ExpansionViewHelper(Context context, long tweetId) {
        this(context, tweetId, false);
    }

    public ExpansionViewHelper(Context context, long tweetId, boolean windowedPopups) {
        this.context = context;
        this.settings = AppSettings.getInstance(context);
        this.id = tweetId;

        // get the base view
        expansion = ((Activity)context).getLayoutInflater().inflate(R.layout.tweet_expansion, null, false);

        landscape = context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        this.windowedPopups = windowedPopups;

        setViews(windowedPopups);
        setClicks(windowedPopups);
        getInfo();
    }

    boolean windowedPopups;

    private SlidrInterface slidr;
    public void setSlidr(SlidrInterface slidr) {
        this.slidr = slidr;
    }

    private void setViews(boolean windowedPopups) {
        favCount = (TextView) expansion.findViewById(R.id.fav_count);
        favText = (TextView) expansion.findViewById(R.id.favorite_text);
        favoriteIcon = (ImageView) expansion.findViewById(R.id.heart_icon);
        favoriteButton = expansion.findViewById(R.id.favorite);

        retweetCount = (TextView) expansion.findViewById(R.id.retweet_count);
        retweetText = (TextView) expansion.findViewById(R.id.retweet_text);
        retweetButton = expansion.findViewById(R.id.retweet);
        retweetIcon = (ImageView) expansion.findViewById(R.id.retweet_icon);
        viewRetweeters = expansion.findViewById(R.id.view_retweeters);
        viewFavoriters = expansion.findViewById(R.id.view_favoriters);

        retweeters = (ImageView) expansion.findViewById(R.id.retweeters);

        favoriters = (ImageView) expansion.findViewById(R.id.favoriters);

        webButton = (ImageButton) expansion.findViewById(R.id.web_button);
        repliesButton = (Button)expansion.findViewById(R.id.show_all_tweets_button);
        composeButton = expansion.findViewById(R.id.compose_button);
        overflowButton = expansion.findViewById(R.id.overflow_button);
        quoteButton = expansion.findViewById(R.id.quote_button);

        tweetSource = (HoloTextView) expansion.findViewById(R.id.tweet_source);

        repliesButton.setTextColor(AppSettings.getInstance(context).themeColors.primaryColorLight);

        convoLayout = ((Activity)context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        replyList = (AsyncListView) convoLayout.findViewById(R.id.listView);
        convoSpinner = (LinearLayout) convoLayout.findViewById(R.id.spinner);

        tweetSource.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (status != null) {
                    // we allow them to mute the client
                    final String client = android.text.Html.fromHtml(status.getSource()).toString();
                    new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.mute_client) + "?")
                            .setMessage(client)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                                                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

                                    String current = sharedPrefs.getString("muted_clients", "");
                                    sharedPrefs.edit().putString("muted_clients", current + client + "   ").commit();
                                    sharedPrefs.edit().putBoolean("refresh_me", true).commit();

                                    dialogInterface.dismiss();

                                    ((Activity) context).finish();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .create()
                            .show();
                } else {
                    // tell them the client hasn't been found
                    Toast.makeText(context, R.string.client_not_found, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });


        retweetersPopup = new RetweetersPopupLayout(context);
        if (context.getResources().getBoolean(R.bool.isTablet)) {
            retweetersPopup.setWidthByPercent(.5f);
        } else {
            retweetersPopup.setWidthByPercent(.8f);
        }
        retweetersPopup.setHeightByPercent(.6f);

        favoritersPopup = new FavoritersPopupLayout(context);
        if (context.getResources().getBoolean(R.bool.isTablet)) {
            favoritersPopup.setWidthByPercent(.5f);
        } else {
            favoritersPopup.setWidthByPercent(.8f);
        }
        favoritersPopup.setHeightByPercent(.6f);

        convoArea = (RelativeLayout) expansion.findViewById(R.id.convo_area);
        convoProgress = (ProgressBar) expansion.findViewById(R.id.convo_spinner);
        convoCard = (CardView) expansion.findViewById(R.id.convo_card);
        embeddedTweetCard = (CardView) expansion.findViewById(R.id.embedded_tweet_card);
        convoTweetArea = (LinearLayout) expansion.findViewById(R.id.tweets_content);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        replyList.setItemManager(builder.build());
    }

    private void setClicks(final boolean windowedPopups) {

        viewRetweeters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (retweetersPopup != null) {
                    retweetersPopup.setOnTopOfView(viewRetweeters);
                    retweetersPopup.setExpansionPointForAnim(viewRetweeters);
                    retweetersPopup.show(slidr);
                }
            }
        });

        viewFavoriters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (favoritersPopup != null) {
                    favoritersPopup.setOnTopOfView(viewFavoriters);
                    favoritersPopup.setExpansionPointForAnim(viewFavoriters);
                    favoritersPopup.show(slidr);
                }
            }
        });

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFavorited || !settings.crossAccActions) {
                    favoriteStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                } else if (settings.crossAccActions) {
                    // dialog for favoriting
                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
                    options[2] = context.getString(R.string.both_accounts);

                    new AlertDialog.Builder(context)
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int item) {
                                    favoriteStatus(item + 1);
                                }
                            })
                            .create().show();
                }
            }
        });

        retweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!settings.crossAccActions) {
                    retweetStatus(secondAcc ? TYPE_ACC_TWO : TYPE_ACC_ONE);
                } else {
                    // dialog for favoriting
                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
                    options[2] = context.getString(R.string.both_accounts);

                    new AlertDialog.Builder(context)
                            .setItems(options, new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, final int item) {
                                    retweetStatus(item + 1);
                                }
                            })
                            .create().show();
                }
            }
        });

        retweetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle(context.getResources().getString(R.string.remove_retweet))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new RemoveRetweet().execute();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });

        quoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = tweet;

                switch (AppSettings.getInstance(context).quoteStyle) {
                    case AppSettings.QUOTE_STYLE_TWITTER:
                        text = " " + "https://twitter.com/" + screenName + "/status/" + id;
                        break;
                    case AppSettings.QUOTE_STYLE_TALON:
                        text = restoreLinks(text);
                        text = "\"@" + screenName + ": " + text + "\" ";
                        break;
                    case AppSettings.QUOTE_STYLE_RT:
                        text = restoreLinks(text);
                        text = " RT @" + screenName + ": " + text;
                        break;
                }

                Intent quote;
                if (!secondAcc) {
                    quote = new Intent(context, ComposeActivity.class);
                } else {
                    quote = new Intent(context, ComposeSecAccActivity.class);
                }
                quote.putExtra("user", text);
                quote.putExtra("id", id);
                quote.putExtra("reply_to_text", "@" + screenName + ": " + tweet);

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                quote.putExtra("already_animated", true);

                context.startActivity(quote, opts.toBundle());
            }
        });

        repliesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status != null) {
                    if (convoPopup == null) {
                        convoPopup = new ConversationPopupLayout(context, convoLayout);
                        if (context.getResources().getBoolean(R.bool.isTablet)) {
                            if (landscape) {
                                convoPopup.setWidthByPercent(.6f);
                                convoPopup.setHeightByPercent(.8f);
                            } else {
                                convoPopup.setWidthByPercent(.85f);
                                convoPopup.setHeightByPercent(.68f);
                            }
                            convoPopup.setCenterInScreen();
                        }
                    }

                    isRunning = true;
                    getDiscussion();

                    convoPopup.setExpansionPointForAnim(view);
                    convoPopup.show(slidr);
                } else {
                    Toast.makeText(context, "Loading Tweet...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent compose;
                if (!secondAcc) {
                    compose = new Intent(context, ComposeActivity.class);
                } else {
                    compose = new Intent(context, ComposeSecAccActivity.class);
                }
                compose.putExtra("user", composeText);
                compose.putExtra("id", id);
                compose.putExtra("reply_to_text", tweetText);

                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());
                compose.putExtra("already_animated", true);

                context.startActivity(compose, opts.toBundle());
            }
        });

        webButton.setEnabled(false);
        webButton.setAlpha(.5f);
        webButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (shareOnWeb) {
                    shareClick();
                    return;
                }
                AppSettings settings = AppSettings.getInstance(context);
                if ((settings.alwaysMobilize ||
                        (Utils.getConnectionStatus(context) && settings.mobilizeOnData))) {

                    final FrameLayout main = (FrameLayout) ((Activity)context).getLayoutInflater().inflate(R.layout.mobilized_fragment, null, false);
                    final ScrollView scrollView = (ScrollView) main.findViewById(R.id.scrollview);
                    View spinner = main.findViewById(R.id.spinner);
                    HoloTextView mobilizedBrowser = (HoloTextView) scrollView.findViewById(R.id.webpage_text);
                    getTextFromSite(webLink, mobilizedBrowser, spinner, scrollView);

                    if (mobilizedPopup == null) {
                        mobilizedPopup = new MobilizedWebPopupLayout(context, main);
                        if (context.getResources().getBoolean(R.bool.isTablet)) {
                            if (landscape) {
                                mobilizedPopup.setWidthByPercent(.6f);
                                mobilizedPopup.setHeightByPercent(.8f);
                            } else {
                                mobilizedPopup.setWidthByPercent(.85f);
                                mobilizedPopup.setHeightByPercent(.68f);
                            }
                            mobilizedPopup.setCenterInScreen();
                        }
                    }
                    mobilizedPopup.setExpansionPointForAnim(webButton);
                    mobilizedPopup.show(slidr);
                } else {
                    final LinearLayout webLayout = (LinearLayout) ((Activity)context).getLayoutInflater().inflate(R.layout.web_popup_layout, null, false);
                    final WebView web = (WebView) webLayout.findViewById(R.id.webview);

                    web.getSettings().setBuiltInZoomControls(true);
                    web.getSettings().setDisplayZoomControls(false);
                    web.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
                    web.getSettings().setUseWideViewPort(true);
                    web.getSettings().setLoadWithOverviewMode(true);
                    web.getSettings().setSavePassword(true);
                    web.getSettings().setSaveFormData(true);
                    web.getSettings().setJavaScriptEnabled(true);
                    web.getSettings().setAppCacheEnabled(false);
                    web.getSettings().setPluginState(WebSettings.PluginState.OFF);

                    // enable navigator.geolocation
                    web.getSettings().setGeolocationEnabled(true);
                    web.getSettings().setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");

                    // enable Web Storage: localStorage, sessionStorage
                    web.getSettings().setDomStorageEnabled(true);

                    web.setWebViewClient(new HelloWebViewClient());

                    web.loadUrl(webLink);
                    if (webPopup == null) {
                        webPopup = new WebPopupLayout(context, webLayout);
                        if (context.getResources().getBoolean(R.bool.isTablet)) {
                            if (landscape) {
                                webPopup.setWidthByPercent(.6f);
                                webPopup.setHeightByPercent(.8f);
                            } else {
                                webPopup.setWidthByPercent(.85f);
                                webPopup.setHeightByPercent(.68f);
                            }
                            webPopup.setCenterInScreen();
                        }
                    }
                    webPopup.setExpansionPointForAnim(webButton);
                    webPopup.show(slidr);
                }
            }
        });
    }

    private void showEmbeddedCard(TweetView view) {
        embeddedTweetCard.addView(view.getView());

        startAlphaAnimation(embeddedTweetCard, 0, AppSettings.getInstance(context).darkTheme ? .75f : 1.0f);
    }

    private void showConvoCard(ArrayList<Status> tweets) {
        int numTweets = 0;

        if (tweets.size() >= 3) {
            numTweets = 3;
        } else if (tweets.size() == 2) {
            numTweets = 2;
        } else if (tweets.size() == 1) {
            numTweets = 1;
        }

        View tweetDivider = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
        tweetDivider.setLayoutParams(params);

        tweetDivider.setBackgroundColor(AppSettings.getInstance(context).themeColors.primaryColor);

        convoTweetArea.addView(tweetDivider);
        for (int i = 0; i < numTweets; i++) {
            TweetView v = new TweetView(context, tweets.get(i));
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            if (i != 0) {
                tweetDivider = new View(context);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
                tweetDivider.setLayoutParams(params);

                if (AppSettings.getInstance(context).darkTheme) {
                    tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.dark_text_drawer));
                } else {
                    tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.light_text_drawer));
                }

                convoTweetArea.addView(tweetDivider);
            }

            convoTweetArea.addView(v.getView());
        }

        hideConvoProgress();
        if (numTweets != 0) {
            startAlphaAnimation(convoCard, 0, AppSettings.getInstance(context).darkTheme ? .75f : 1.0f);
        }
    }

    private void hideConvoProgress() {
        final View spinner = convoProgress;
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (spinner.getVisibility() != View.INVISIBLE) {
                    spinner.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        anim.setDuration(250);
        spinner.startAnimation(anim);
    }

    private void shareClick() {
        String text1 = restoreLinks(tweetText);
        text1 = text1 + "\n\n" + "https://twitter.com/" + screenName + "/status/" + id;
        Log.v("my_text_on_share", text1);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((Activity)context).getWindow().setExitTransition(null);
        }

        context.startActivity(Intent.createChooser(share, "Share with:"));
    }


    private boolean showEmbeddded = true;

    public void showEmbedded(boolean show) {
        showEmbeddded = show;
        embeddedTweetCard.setVisibility(View.GONE);
    }

    String webLink = null;
    long embeddedTweetId = 0l;
    public boolean shareOnWeb = false;
    public String[] otherLinks;

    public void setWebLink(String[] otherLinks) {

        this.otherLinks = otherLinks;

        ArrayList<String> webpages = new ArrayList<String>();

        if (otherLinks.length > 0 && !otherLinks[0].equals("")) {
            for (String s : otherLinks) {
                if (!s.contains("youtu")) {
                    if (!s.contains("pic.twitt")) {
                        webpages.add(s);
                    }
                }
            }

            if (webpages.size() >= 1) {
                webLink = webpages.get(0);
            } else {
                webLink = null;
            }

        } else {
            webLink = null;
        }

        if (webLink == null || webLink.contains("/status/")) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.shareButton});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            webButton.setImageResource(resource);
            shareOnWeb = true;

            if (webLink != null && webLink.contains("/status/")) {
                embeddedTweetId = TweetLinkUtils.getTweetIdFromLink(webLink);

                if (embeddedTweetId != 0l) {
                    embeddedTweetCard.setVisibility(View.INVISIBLE);
                }
            }
        }

        webButton.setEnabled(true);
        webButton.setAlpha(1.0f);
    }

    public void startFlowAnimation() {
        favoriteButton.setVisibility(View.INVISIBLE);
        retweetButton.setVisibility(View.INVISIBLE);
        webButton.setVisibility(View.INVISIBLE);
        quoteButton.setVisibility(View.INVISIBLE);
        composeButton.setVisibility(View.INVISIBLE);
        overflowButton.setVisibility(View.INVISIBLE);
        convoProgress.setVisibility(View.INVISIBLE);

        startAlphaAnimation(favoriteButton, 0);
        startAlphaAnimation(retweetButton, 75);
        startAlphaAnimation(webButton, 75);
        startAlphaAnimation(quoteButton, 150);
        startAlphaAnimation(convoProgress, 175);
        startAlphaAnimation(composeButton, 225);
        startAlphaAnimation(overflowButton, 250);
    }

    private void startAlphaAnimation(final View v, long offset) {
        startAlphaAnimation(v, offset, 0f, 1.0f);
    }

    private void startAlphaAnimation(final View v, long offset, float finish) {
        startAlphaAnimation(v, offset, 0f, finish);
    }

    private void startAlphaAnimation(final View v, long offset, float start, float finish) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, View.ALPHA, start, finish);
        alpha.setDuration(1000);
        alpha.setStartDelay(offset);
        alpha.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        alpha.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) { }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
        alpha.start();
    }

    String tweetText = null;
    String composeText = null;
    public void setReplyDetails(String t, String replyText) {
        this.tweetText = t;
        this.composeText = replyText;
    }

    private String screenName;
    public void setUser(String name) {
        screenName = name;
    }

    private String tweet;
    public void setText(String t) {
        tweet = t;
    }

    private String videoUrl = null;
    private boolean videoIsGif = false;
    public void setVideoDownload(String url) {
        if (url != null) {
            videoUrl = url;
        }
    }
    public void setGifDownload(String url) {
        if (url != null) {
            videoUrl = url;
            videoIsGif = true;
        }
    }

    public void setUpOverflow() {
        final PopupMenu menu = new PopupMenu(context, overflowButton);

        if (screenName.equals(AppSettings.getInstance(context).myScreenName)) {
            // my tweet

            final int DELETE_TWEET = 1;
            final int COPY_TEXT = 2;
            final int SHARE_TWEET = 3;

            menu.getMenu().add(Menu.NONE, DELETE_TWEET, Menu.NONE, context.getString(R.string.menu_delete_tweet));
            menu.getMenu().add(Menu.NONE, COPY_TEXT, Menu.NONE, context.getString(R.string.menu_copy_text));

            if (!shareOnWeb) { //share button isn't on top of the web button
                menu.getMenu().add(Menu.NONE, SHARE_TWEET, Menu.NONE, context.getString(R.string.menu_share));
            }

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case DELETE_TWEET:
                            new DeleteTweet().execute();
                            context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                                    Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                                    .edit().putBoolean("just_muted", true).commit();

                            ((Activity)context).recreate();
                            break;
                        case COPY_TEXT:
                            copyText();
                            break;
                        case SHARE_TWEET:
                            shareClick();
                            break;
                    }
                    return false;
                }
            });
        } else {
            // someone else's tweet

            final int COPY_LINK = 1;
            final int COPY_TEXT = 2;
            final int MARK_SPAM = 3;
            final int TRANSLATE = 4;
            final int SHARE = 5;

            menu.getMenu().add(Menu.NONE, COPY_LINK, Menu.NONE, context.getString(R.string.copy_link));
            menu.getMenu().add(Menu.NONE, COPY_TEXT, Menu.NONE, context.getString(R.string.menu_copy_text));
            menu.getMenu().add(Menu.NONE, MARK_SPAM, Menu.NONE, context.getString(R.string.menu_spam));
            menu.getMenu().add(Menu.NONE, TRANSLATE, Menu.NONE, context.getString(R.string.menu_translate));

            if (!shareOnWeb) { //share button isn't on top of the web button
                menu.getMenu().add(Menu.NONE, SHARE, Menu.NONE, context.getString(R.string.menu_share));
            }

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case COPY_LINK:
                            copyLink();
                            break;
                        case COPY_TEXT:
                            copyText();
                            break;
                        case SHARE:
                            shareClick();
                            break;
                        case TRANSLATE:
                            String url = "http://translate.google.com/#auto|en|" + tweet;

                            final LinearLayout webLayout = (LinearLayout) ((Activity)context).getLayoutInflater().inflate(R.layout.web_popup_layout, null, false);
                            final WebView web = (WebView) webLayout.findViewById(R.id.webview);

                            web.getSettings().setBuiltInZoomControls(true);
                            web.getSettings().setDisplayZoomControls(false);
                            web.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
                            web.getSettings().setUseWideViewPort(true);
                            web.getSettings().setLoadWithOverviewMode(true);
                            web.getSettings().setSavePassword(true);
                            web.getSettings().setSaveFormData(true);
                            web.getSettings().setJavaScriptEnabled(true);
                            web.getSettings().setAppCacheEnabled(false);
                            web.getSettings().setPluginState(WebSettings.PluginState.OFF);

                            // enable navigator.geolocation
                            web.getSettings().setGeolocationEnabled(true);
                            web.getSettings().setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");

                            // enable Web Storage: localStorage, sessionStorage
                            web.getSettings().setDomStorageEnabled(true);

                            web.setWebViewClient(new HelloWebViewClient());

                            web.loadUrl(url);
                            if (webPopup == null) {
                                webPopup = new WebPopupLayout(context, webLayout);
                                if (context.getResources().getBoolean(R.bool.isTablet)) {
                                    if (landscape) {
                                        webPopup.setWidthByPercent(.6f);
                                        webPopup.setHeightByPercent(.8f);
                                    } else {
                                        webPopup.setWidthByPercent(.85f);
                                        webPopup.setHeightByPercent(.68f);
                                    }
                                    webPopup.setCenterInScreen();
                                }
                            }
                            webPopup.show(slidr);
                            break;
                        case MARK_SPAM:
                            new MarkSpam().execute();
                            break;
                    }
                    return false;
                }
            });
        }

        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.show();
            }
        });
    }

    private void copyLink() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tweet_link", "https://twitter.com/" + screenName + "/status/" + id);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void copyText() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tweet_text", restoreLinks(tweet));
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    public void setBackground(View v) {
        background = v;

        background.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return hidePopups();
            }
        });
    }

    public boolean hidePopups() {
        boolean hidden = false;
        try {
            if (retweetersPopup.isShowing()) {
                retweetersPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }
        try {
            if (favoritersPopup.isShowing()) {
                favoritersPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }
        try {
            if (convoLayout.isShown()) {
                convoPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }
        try {
            if (webPopup.isShowing()) {
                webPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }
        try {
            if (mobilizedPopup.isShowing()) {
                mobilizedPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }

        return hidden;
    }

    private boolean secondAcc = false;
    public void setSecondAcc(boolean sec) {
        secondAcc = sec;
    }

    private Twitter getTwitter() {
        if (secondAcc) {
            return Utils.getSecondTwitter(context);
        } else {
            return Utils.getTwitter(context, AppSettings.getInstance(context));
        }
    }

    public View getExpansion() {
        return expansion;
    }

    private final int TYPE_ACC_ONE = 1;
    private final int TYPE_ACC_TWO = 2;
    private final int TYPE_BOTH_ACC = 3;

    boolean isFavorited = false;
    boolean isRetweeted = false;

    public void favoriteStatus(final int type) {
        if (isFavorited) {
            Toast.makeText(context, context.getResources().getString(R.string.removing_favorite), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getResources().getString(R.string.favoriting_status), Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Twitter twitter = null;
                    Twitter secTwitter = null;
                    if (type == TYPE_ACC_ONE) {
                        twitter = Utils.getTwitter(context, settings);
                    } else if (type == TYPE_ACC_TWO) {
                        secTwitter = Utils.getSecondTwitter(context);
                    } else {
                        twitter = Utils.getTwitter(context, settings);
                        secTwitter = Utils.getSecondTwitter(context);
                    }

                    if (isFavorited && twitter != null) {
                        twitter.destroyFavorite(id);
                    } else if (twitter != null) {
                        try {
                            twitter.createFavorite(id);
                        } catch (TwitterException e) {
                            // already been favorited by this account
                        }
                    }

                    if (secTwitter != null) {
                        try {
                            secTwitter.createFavorite(id);
                        } catch (Exception e) {

                        }
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, context.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                                getFavoriteCount();
                            } catch (Exception e) {
                                // they quit out of the activity
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void retweetStatus(final int type) {
        Toast.makeText(context, context.getResources().getString(R.string.retweeting_status), Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // if they have a protected account, we want to still be able to retweet their retweets
                    long idToRetweet = id;
                    if (status != null && status.isRetweet()) {
                        idToRetweet = status.getRetweetedStatus().getId();
                    }

                    Twitter twitter = null;
                    Twitter secTwitter = null;
                    if (type == TYPE_ACC_ONE) {
                        twitter = Utils.getTwitter(context, settings);
                    } else if (type == TYPE_ACC_TWO) {
                        secTwitter = Utils.getSecondTwitter(context);
                    } else {
                        twitter = Utils.getTwitter(context, settings);
                        secTwitter = Utils.getSecondTwitter(context);
                    }

                    if (twitter != null) {
                        try {
                            twitter.retweetStatus(idToRetweet);
                        } catch (TwitterException e) {

                        }
                    }

                    if (secTwitter != null) {
                        secTwitter.retweetStatus(idToRetweet);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, context.getResources().getString(R.string.retweet_success), Toast.LENGTH_SHORT).show();
                                getRetweetCount();
                            } catch (Exception e) {

                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    private Status status = null;

    public void getInfo() {

        Thread getInfo = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(NETWORK_ACTION_DELAY);
                } catch (Exception e) {

                }

                try {
                    Twitter twitter =  getTwitter();

                    status = twitter.showStatus(id);

                    getConversationAndEmbeddedTweet();

                    if (status.isRetweet()) {
                        status = status.getRetweetedStatus();
                    }



                    final String sfavCount = status.getFavoriteCount() + "";

                    isRetweeted = status.isRetweetedByMe();
                    final String retCount = "" + status.getRetweetCount();

                    if (status.getRetweetCount() > 0) {
                        getRetweeters();
                    } else {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewRetweeters.setVisibility(View.INVISIBLE);
                            }
                        });
                    }

                    if (status.getFavoriteCount() > 0) {
                        getFavoriters();
                    } else {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewFavoriters.setVisibility(View.INVISIBLE);
                            }
                        });
                    }

                    final Status fStatus = status;

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (loadedCallback != null) {
                                loadedCallback.onLoad(status);
                            }

                            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
                            int textColor = a.getResourceId(0, 0);
                            a.recycle();

                            retweetCount.setText(" " + retCount);

                            if (isRetweeted) {
                                retweetText.setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
                                retweetIcon.setColorFilter(AppSettings.getInstance(context).themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
                            } else {
                                retweetText.setTextColor(context.getResources().getColor(textColor));
                                retweetIcon.clearColorFilter();
                            }

                            favCount.setText(" " + sfavCount);

                            if (fStatus.isFavorited()) {
                                favText.setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
                                favoriteIcon.setColorFilter(AppSettings.getInstance(context).themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
                                isFavorited = true;
                            } else {
                                favText.setTextColor(context.getResources().getColor(textColor));
                                favoriteIcon.clearColorFilter();
                                isFavorited = false;
                            }

                            String via = context.getResources().getString(R.string.via) + " " + android.text.Html.fromHtml(status.getSource()).toString();
                            tweetSource.setText(via);
                        }
                    });
                } catch (Exception e) {

                }
            }
        });

        getInfo.setPriority(Thread.MAX_PRIORITY);
        getInfo.start();
    }

    public void getRetweetCount() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean retweetedByMe;
                try {
                    Twitter twitter =  getTwitter();
                    twitter4j.Status status = twitter.showStatus(id);

                    retweetedByMe = status.isRetweetedByMe();
                    final String retCount = "" + status.getRetweetCount();


                    final boolean fRet = retweetedByMe;
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
                            int textColor = a.getResourceId(0, 0);
                            a.recycle();

                            retweetCount.setText(" " + retCount);

                            if (fRet) {
                                retweetText.setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
                                retweetIcon.setColorFilter(AppSettings.getInstance(context).themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
                            } else {
                                retweetText.setTextColor(context.getResources().getColor(textColor));
                                retweetIcon.clearColorFilter();
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void getFavoriteCount() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  getTwitter();
                    Status status = twitter.showStatus(id);
                    if (status.isRetweet()) {
                        Status retweeted = status.getRetweetedStatus();
                        status = retweeted;
                    }

                    final Status fStatus = status;
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            favCount.setText(" " + fStatus.getFavoriteCount());

                            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
                            int textColor = a.getResourceId(0, 0);
                            a.recycle();

                            if (fStatus.isFavorited()) {
                                favText.setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
                                favoriteIcon.setColorFilter(AppSettings.getInstance(context).themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
                                isFavorited = true;
                            } else {
                                favText.setTextColor(context.getResources().getColor(textColor));
                                favoriteIcon.clearColorFilter();
                                isFavorited = false;
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    class RemoveRetweet extends AsyncTask<String, Void, Boolean> {


        public RemoveRetweet() {

        }

        protected void onPreExecute() {
            Toast.makeText(context, context.getResources().getString(R.string.removing_retweet), Toast.LENGTH_SHORT).show();
        }

        protected Boolean doInBackground(String... urls) {
            try {
                AppSettings settings = AppSettings.getInstance(context);
                Twitter twitter =  getTwitter();
                ResponseList<twitter4j.Status> retweets = twitter.getRetweets(id);
                for (twitter4j.Status retweet : retweets) {
                    if(retweet.getUser().getId() == settings.myId)
                        twitter.destroyStatus(retweet.getId());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {

            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
            int textColor = a.getResourceId(0, 0);
            a.recycle();

            if (retweetText != null) {
                retweetText.setTextColor(context.getResources().getColor(textColor));
            }

            try {
                if (deleted) {
                    Toast.makeText(context, context.getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // user has gone away from the window
            }
        }
    }

    public void stop() {
        isRunning = false;
    }

    public void getConversationAndEmbeddedTweet() {
        Thread getConvo = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(NETWORK_ACTION_DELAY);
                } catch (Exception e) {

                }

                if (!isRunning) {
                    return;
                }

                Twitter twitter = getTwitter();

                try {
                    if (embeddedTweetId != 0l && showEmbeddded) {
                        final Status embedded = twitter.showStatus(embeddedTweetId);

                        if (embedded != null) {
                            ((Activity)context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TweetView v = new TweetView(context, embedded);
                                    v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
                                    v.setSmallImage(true);

                                    showEmbeddedCard(v);
                                }
                            });
                        }
                    }
                } catch (Exception e) { }
                replies = new ArrayList<twitter4j.Status>();
                try {

                    if (status.isRetweet()) {
                        status = status.getRetweetedStatus();
                    }

                    twitter4j.Status replyStatus = twitter.showStatus(status.getInReplyToStatusId());

                    try {
                        while(!replyStatus.getText().equals("")) {
                            if (!isRunning) {
                                return;
                            }
                            replies.add(replyStatus);
                            Log.v("reply_status", replyStatus.getText());

                            replyStatus = twitter.showStatus(replyStatus.getInReplyToStatusId());
                        }

                    } catch (Exception e) {
                        // the list of replies has ended, but we dont want to go to null
                    }



                } catch (TwitterException e) {
                    e.printStackTrace();
                }

                if (status != null && replies.size() > 0) {
                    replies.add(0, status);
                }

                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (replies.size() > 0) {

                                ArrayList<twitter4j.Status> reversed = new ArrayList<twitter4j.Status>();
                                for (int i = replies.size() - 1; i >= 0; i--) {
                                    reversed.add(replies.get(i));
                                }

                                replies = reversed;

                                adapter = new TimelineArrayAdapter(context, replies);
                                replyList.setAdapter(adapter);
                                replyList.setVisibility(View.VISIBLE);
                                //adjustConversationSectionSize(replyList);
                                convoSpinner.setVisibility(View.GONE);

                            }
                        } catch (Exception e) {
                            // none and it got the null object
                        }


                        if (status != null) {
                            // everything here worked, so get the discussion on the tweet
                            getDiscussion();
                        }
                    }
                });
            }
        });

        getConvo.setPriority(Thread.NORM_PRIORITY);
        getConvo.start();
    }
    public boolean isRunning = true;
    public ArrayList<Status> replies;
    public TimelineArrayAdapter adapter;
    public Query query;
    private boolean cardShown = false;
    private boolean firstRun = true;

    public void getDiscussion() {

        Thread getReplies = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(NETWORK_ACTION_DELAY);
                } catch (Exception e) {

                }

                if (!isRunning || (!firstRun && query == null)) {
                    return;
                }

                ArrayList<twitter4j.Status> all = null;
                Twitter twitter = getTwitter();
                try {
                    Log.v("talon_replies", "looking for discussion");

                    long id = status.getId();
                    String screenname = status.getUser().getScreenName();

                    if (query == null) {
                        query = new Query("to:" + screenname);
                        query.sinceId(id);

                        try {
                            query.setCount(30);
                        } catch (Throwable e) {
                            // enlarge buffer error?
                            query.setCount(30);
                        }
                        firstRun = false;
                    }

                    QueryResult result = twitter.search(query);

                    all = new ArrayList<>();

                    int repsWithoutChange = 0;

                    do {
                        boolean repliesChangedOnThisIteration = false;

                        Log.v("talon_replies", "do loop repetition");
                        if (!isRunning) {
                            return;
                        }
                        List<Status> tweets = result.getTweets();

                        for (twitter4j.Status tweet : tweets) {
                            if (tweet.getInReplyToStatusId() == id) {
                                all.add(tweet);
                            }
                        }

                        if (all.size() > 0) {
                            for (int i = all.size() - 1; i >= 0; i--) {
                                Log.v("talon_replies", all.get(i).getText());
                                replies.add(all.get(i));

                                repliesChangedOnThisIteration = true;
                            }

                            all.clear();

                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    convoSpinner.setVisibility(View.GONE);
                                    try {
                                        if (replies.size() > 0) {
                                            if (adapter == null || adapter.getCount() == 0) {
                                                adapter = new TimelineArrayAdapter(context, replies);
                                                replyList.setAdapter(adapter);
                                                replyList.setVisibility(View.VISIBLE);
                                            } else {
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    } catch (Exception e) {
                                        // none and it got the null object
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        query = result.nextQuery();

                        if (query != null) {
                            result = twitter.search(query);
                        }

                        if (replies.size() >= 3 && !cardShown) {
                            cardShown = true;
                            isRunning = false;
                            // we will start showing them below the buttons
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showConvoCard(replies);
                                }
                            });

                            return;
                        }

                        try {
                            Thread.sleep(250);
                        } catch (Exception e) {
                            // since we are changing the arraylist for the adapter in the background, we need to make sure it
                            // gets updated before continuing
                        }

                        if (!repliesChangedOnThisIteration) {
                            repsWithoutChange++;
                        }

                    } while (query != null && isRunning && repsWithoutChange < 5);
                } catch (TwitterException e) {
                    if (e.getMessage().contains("limit exceeded")) {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Cannot find conversation - rate limit reached.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                }

                if (replies.size() == 0) {
                    // nothing to show, so tell them that
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hideConvoProgress();
                            try {
                                convoPopup.hide();
                            } catch (Exception e) {

                            }
                        }
                    });
                } else if (replies.size() < 3) {
                    cardShown = true;
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showConvoCard(replies);
                        }
                    });
                }
            }
        });

        getReplies.setPriority(8);
        getReplies.start();

    }

    public void getRetweeters() {
        Thread getRetweeters = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(NETWORK_ACTION_DELAY);
                } catch (Exception e) {

                }

                try {
                    Twitter twitter =  getTwitter();

                    Status stat = status;
                    if (stat.isRetweet()) {
                        id = stat.getRetweetedStatus().getId();
                    }

                    // can get 100 retweeters is all
                    ResponseList<twitter4j.Status> lists = twitter.getRetweets(id);

                    List<String> urls = new ArrayList<String>();
                    final ArrayList<User> users = new ArrayList<User>();

                    for (Status s : lists) {
                        users.add(s.getUser());
                        urls.add(s.getUser().getBiggerProfileImageURL());
                    }

                    if (urls.size() > 4) {
                        urls = urls.subList(0, 3);
                    }

                    final List<String> furls = urls;
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            retweetersPopup.setData(users);

                            String combined = "";
                            for (int i = 0; i < furls.size(); i++) {
                                combined += furls.get(i) + " ";
                            }

                            if (android.text.TextUtils.isEmpty(combined)) {
                                viewRetweeters.setVisibility(View.INVISIBLE);
                                viewRetweeters.setEnabled(false);
                            } else {
                                ImageUtils.loadImage(context, retweeters, combined, App.getInstance(context).getBitmapCache());
                                viewRetweeters.setVisibility(View.VISIBLE);
                                viewRetweeters.setEnabled(true);
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();

                } catch (OutOfMemoryError e) {
                    e.printStackTrace();

                }
            }
        });

        getRetweeters.setPriority(Thread.MAX_PRIORITY);
        getRetweeters.start();
    }

    public void getFavoriters() {
        Thread getFavoriters = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(NETWORK_ACTION_DELAY);
                } catch (Exception e) {

                }

                try {

                    Status stat = status;
                    long id = stat.getId();

                    final List<User> users = (new FavoriterUtils()).getFavoriters(context, id);
                    List<String> urls = new ArrayList<String>();

                    for (User s : users) {
                        urls.add(s.getBiggerProfileImageURL());
                    }

                    if (urls.size() > 4) {
                        urls = urls.subList(0, 3);
                    }

                    final List<String> furls = urls;
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            favoritersPopup.setData(users);

                            String combined = "";
                            for (int i = 0; i < furls.size(); i++) {
                                combined += furls.get(i) + " ";
                            }

                            if (android.text.TextUtils.isEmpty(combined)) {
                                viewFavoriters.setVisibility(View.INVISIBLE);
                                viewFavoriters.setEnabled(false);
                            } else {
                                ImageUtils.loadImage(context, favoriters, combined, App.getInstance(context).getBitmapCache());
                                viewFavoriters.setVisibility(View.VISIBLE);
                                viewFavoriters.setEnabled(true);
                            }
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();

                } catch (OutOfMemoryError e) {
                    e.printStackTrace();

                }
            }
        });

        getFavoriters.setPriority(Thread.MAX_PRIORITY);
        getFavoriters.start();
    }

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    public void getTextFromSite(final String url, final HoloTextView browser, final View spinner, final ScrollView scroll) {
        Thread getText = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(NETWORK_ACTION_DELAY);
                } catch (Exception e) {

                }

                try {
                    Document doc = Jsoup.connect(url).get();

                    String text = "";
                    String title = doc.title();

                    if(doc != null) {
                        Elements paragraphs = doc.getElementsByTag("p");

                        if (paragraphs.hasText()) {
                            for (int i = 0; i < paragraphs.size(); i++) {
                                Element s = paragraphs.get(i);
                                if (!s.html().contains("<![CDATA")) {
                                    text += paragraphs.get(i).html().replaceAll("<br/>", "") + "<br/><br/>";
                                }
                            }
                        }
                    }

                    final String article =
                            "<strong><big>" + title + "</big></strong>" +
                                    "<br/><br/>" +
                                    text.replaceAll("<img.+?>", "") +
                                    "<br/>"; // one space at the bottom to make it look nicer

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                browser.setText(Html.fromHtml(article));
                                browser.setMovementMethod(LinkMovementMethod.getInstance());
                                browser.setTextSize(AppSettings.getInstance(context).textSize);
                                scroll.setVisibility(View.VISIBLE);
                                spinner.setVisibility(View.INVISIBLE);
                            } catch (Exception e) {
                                // fragment not attached
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    browser.setText(context.getResources().getString(R.string.error_loading_page));
                                } catch (Exception e) {
                                    // fragment not attached
                                }
                            }
                        });
                    } catch (Exception x) {
                        // not attached
                    }
                } catch (OutOfMemoryError e) {
                    e.printStackTrace();
                    try {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    browser.setText(context.getResources().getString(R.string.error_loading_page));
                                } catch (Exception e) {
                                    // fragment not attached
                                }
                            }
                        });
                    } catch (Exception x) {
                        // not attached
                    }
                }
            }
        });

        getText.setPriority(8);
        getText.start();
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {

                HomeDataSource.getInstance(context).deleteTweet(id);
                MentionsDataSource.getInstance(context).deleteTweet(id);

                try {
                    twitter.destroyStatus(id);
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
                Toast.makeText(context, context.getResources().getString(R.string.deleted_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
            }

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).commit();

            ((Activity)context).recreate();
        }
    }

    class MarkSpam extends AsyncTask<String, Void, Boolean> {

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {
                HomeDataSource.getInstance(context).deleteTweet(id);
                MentionsDataSource.getInstance(context).deleteTweet(id);

                try {
                    twitter.reportSpam(screenName.replace(" ", "").replace("@", ""));
                } catch (Throwable t) {
                    // for somme reason this causes a big "naitive crash" on some devices
                    // with a ton of random letters on play store reports... :/ hmm
                }

                try {
                    twitter.destroyStatus(id);
                } catch (Exception x) {

                }

                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).commit();

                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                return false;
            }
        }

        protected void onPostExecute(Boolean deleted) {
            if (deleted) {
                Toast.makeText(context, context.getResources().getString(R.string.deleted_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
            }

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).commit();

            ((Activity)context).recreate();
        }
    }

    public String restoreLinks(String text) {
        String full = text;

        String[] split = text.split("\\s");
        String[] otherLink = new String[otherLinks.length];

        for (int i = 0; i < otherLinks.length; i++) {
            otherLink[i] = "" + otherLinks[i];
        }

        for (String s : otherLink) {
            Log.v("talon_links", ":" + s + ":");
        }

        boolean changed = false;
        int otherIndex = 0;

        if (otherLink.length > 0) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];

                //if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                    String f = s.replace("...", "").replace("http", "");

                    f = stripTrailingPeriods(f);

                    try {
                        if (otherIndex < otherLinks.length) {
                            if (otherLink[otherIndex].substring(otherLink[otherIndex].length() - 1, otherLink[otherIndex].length()).equals("/")) {
                                otherLink[otherIndex] = otherLink[otherIndex].substring(0, otherLink[otherIndex].length() - 1);
                            }
                            f = otherLink[otherIndex].replace("http://", "").replace("https://", "").replace("www.", "");
                            otherLink[otherIndex] = "";
                            otherIndex++;

                            changed = true;
                        }
                    } catch (Exception e) {

                    }

                    if (changed) {
                        split[i] = f;
                    } else {
                        split[i] = s;
                    }
                } else {
                    split[i] = s;
                }

            }
        }

        if (webLink != null && !webLink.equals("")) {
            for (int i = split.length - 1; i >= 0; i--) {
                String s = split[i];
                if (Patterns.WEB_URL.matcher(s).find()) {
                    String replace = otherLinks[otherLinks.length - 1];
                    if (replace.replace(" ", "").equals("")) {
                        replace = webLink;
                    }
                    split[i] = replace;
                    changed = true;
                    break;
                }
            }
        }

        if(changed) {
            full = "";
            for (String p : split) {
                full += p + " ";
            }

            full = full.substring(0, full.length() - 1);
        }

        return full.replaceAll("  ", " ");
    }
    private static String stripTrailingPeriods(String url) {
        try {
            if (url.substring(url.length() - 1, url.length()).equals(".")) {
                return stripTrailingPeriods(url.substring(0, url.length() - 1));
            } else {
                return url;
            }
        } catch (Exception e) {
            return url;
        }
    }
}
