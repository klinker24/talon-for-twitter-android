package com.klinker.android.twitter_l.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import androidx.cardview.widget.CardView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.BrowserActivity;
import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.SavedTweetsFragment;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.sq_lite.SavedTweetsDataSource;
import com.klinker.android.twitter_l.views.TweetView;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.ListDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.views.popups.ConversationPopupLayout;
import com.klinker.android.twitter_l.views.popups.TweetInteractionsPopup;
import com.klinker.android.twitter_l.views.popups.WebPopupLayout;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.discover.trends.SearchedTrendsActivity;

import twitter4j.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ExpansionViewHelper {

    private static final int CONVO_CARD_LIST_SIZE = 6;
    private static final int MAX_TWEETS_IN_CONVERSATION = 50;

    public interface TweetLoaded {
        void onLoad(Status status);
    }

    private TweetLoaded loadedCallback;
    public void setLoadCallback(TweetLoaded callback) {
        this.loadedCallback = callback;
    }

    Context context;
    AppSettings settings;
    public long id;

    // root view
    private View expansion;

    // area that is used for the previous tweets in the conversation
    private View inReplyToArea;
    private LinearLayout inReplyToTweets;

    private View countsView;
    private View buttonsRoot;
    private TextView tweetCounts;
    private ImageButton overflowButton;
    private TextView repliesText;
    private View repliesButton;

    private ListView replyList;
    private LinearLayout convoSpinner;
    private View convoLayout;

    private FontPrefTextView tweetSource;

    private ConversationPopupLayout convoPopup;
    private WebPopupLayout webPopup;
    private TweetInteractionsPopup interactionsPopup;

    private ProgressBar convoProgress;
    private FrameLayout convoCard;
    private CardView embeddedTweetCard;
    private LinearLayout convoTweetArea;

    private boolean landscape;

    private TweetButtonUtils tweetButtonUtils;

    public ExpansionViewHelper(Context context, long tweetId) {
        this.tweetButtonUtils = new TweetButtonUtils(context);
        this.context = context;
        this.settings = AppSettings.getInstance(context);
        this.id = tweetId;

        // get the base view
        expansion = ((Activity)context).getLayoutInflater().inflate(R.layout.tweet_expansion, null, false);
        landscape = context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        setViews();
        setClicks();
        getInfo();
    }

    private void setViews() {
        countsView = expansion.findViewById(R.id.counts_layout);
        buttonsRoot = expansion.findViewById(R.id.tweet_buttons);

        tweetCounts = (TextView) expansion.findViewById(R.id.tweet_counts);
        repliesButton = expansion.findViewById(R.id.show_all_tweets_button);
        repliesText = (TextView) expansion.findViewById(R.id.replies_text);
        overflowButton = (ImageButton) expansion.findViewById(R.id.overflow_button);

        tweetSource = (FontPrefTextView) expansion.findViewById(R.id.tweet_source);

        ((LinearLayout.LayoutParams) repliesText.getLayoutParams()).bottomMargin = Utils.getStatusBarHeight(context);

        if (settings.darkTheme && settings.theme == AppSettings.THEME_DARK_BACKGROUND_COLOR) {
            repliesText.setTextColor(settings.themeColors.accentColor);
        } else if (!settings.darkTheme && settings.theme == AppSettings.THEME_WHITE) {
            repliesText.setTextColor(settings.themeColors.accentColor);
        } else {
            repliesText.setTextColor(settings.themeColors.primaryColorLight);
        }

        convoLayout = ((Activity)context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        replyList = (ListView) convoLayout.findViewById(R.id.listView);
        convoSpinner = (LinearLayout) convoLayout.findViewById(R.id.spinner);

        if (settings.darkTheme) {
            expansion.findViewById(R.id.compose_button).setAlpha(.75f);
        }

        tweetSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status != null) {
                    // we allow them to mute the client
                    final String client = android.text.Html.fromHtml(status.getSource()).toString();
                    new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.mute_client) + "?")
                            .setMessage(client)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);


                                    String current = sharedPrefs.getString("muted_clients", "");
                                    sharedPrefs.edit().putString("muted_clients", current + client + "   ").apply();
                                    sharedPrefs.edit().putBoolean("refresh_me", true).apply();

                                    dialogInterface.dismiss();

                                    ((Activity) context).finish();

                                    if (context instanceof DrawerActivity) {
                                        context.startActivity(new Intent(context, MainActivity.class));
                                        ((Activity) context).overridePendingTransition(0,0);
                                    }
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
            }
        });

        convoProgress = (ProgressBar) expansion.findViewById(R.id.convo_spinner);
        convoCard = (FrameLayout) expansion.findViewById(R.id.convo_card);
        embeddedTweetCard = (CardView) expansion.findViewById(R.id.embedded_tweet_card);
        convoTweetArea = (LinearLayout) expansion.findViewById(R.id.tweets_content);
    }

    private void setClicks() {
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
                    convoPopup.show();
                } else {
                    Toast.makeText(context, "Loading Tweet...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tweetCounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (interactionsPopup == null) {
                    interactionsPopup = new TweetInteractionsPopup(context);
                    if (context.getResources().getBoolean(R.bool.isTablet)) {
                        if (landscape) {
                            interactionsPopup.setWidthByPercent(.6f);
                            interactionsPopup.setHeightByPercent(.8f);
                        } else {
                            interactionsPopup.setWidthByPercent(.85f);
                            interactionsPopup.setHeightByPercent(.68f);
                        }
                        interactionsPopup.setCenterInScreen();
                    }
                }

                interactionsPopup.setExpansionPointForAnim(v);

                if (status != null) {
                    interactionsPopup.setInfo(status.getUser().getScreenName(), status.getId());
                } else {
                    interactionsPopup.setInfo(screenName, id);
                }

                interactionsPopup.show();
            }
        });
    }

    private void showEmbeddedCard(TweetView view) {
        embeddedTweetCard.addView(view.getView());
        startAlphaAnimation(embeddedTweetCard,
                AppSettings.getInstance(context).darkTheme ? .75f : 1.0f);
    }

    private void showConvoCard(List<Status> tweets) {
        int numTweets;

        if (tweets.size() >= CONVO_CARD_LIST_SIZE) {
            numTweets = CONVO_CARD_LIST_SIZE;
        } else {
            numTweets = tweets.size();
        }

        if (tweets.size() > CONVO_CARD_LIST_SIZE) {
            repliesButton.setVisibility(View.VISIBLE);
        } else {
            repliesText.setVisibility(View.GONE);
            repliesButton.getLayoutParams().height = Utils.toDP(24, context);
            repliesButton.requestLayout();
        }

        View tweetDivider;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
        List<TweetView> tweetViews = new ArrayList<>();

        for (int i = 0; i < numTweets; i++) {
            TweetView v = new TweetView(context, tweets.get(i));
            v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
            v.setSmallImage(true);

            if (i != 0) {
                tweetDivider = new View(context);
                tweetDivider.setLayoutParams(params);

                if (AppSettings.getInstance(context).darkTheme) {
                    tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.dark_text_drawer));
                } else {
                    tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.light_text_drawer));
                }

                convoTweetArea.addView(tweetDivider);
            }

            tweetViews.add(v);
            convoTweetArea.addView(v.getView());
        }

        hideConvoProgress();
        if (numTweets != 0) {
            convoCard.setVisibility(View.VISIBLE);
            startChainSearch(tweetViews);
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

    public String[] otherLinks;

    public void setWebLink(String[] otherLinks) {
        String webLink = null;
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
        }

        if (webLink != null && webLink.contains("/status/")) {
            long embeddedTweetId = TweetLinkUtils.getTweetIdFromLink(webLink);

            if (embeddedTweetId != 0l) {
                embeddedTweetCard.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void startAlphaAnimation(final View v, float finish) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, View.ALPHA, 0, finish);
        alpha.setDuration(0);
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

    private String screenName;
    public void setUser(String name) {
        screenName = name;
    }

    private String tweet;
    public void setText(String t) {
        tweet = t;
    }

    public void setUpOverflow() {
        final PopupMenu menu = new PopupMenu(context, overflowButton);
        final boolean tweetIsSaved = SavedTweetsDataSource.getInstance(context).isTweetSaved(id, settings.currentAccount);

        if (screenName.equals(AppSettings.getInstance(context).myScreenName)) {
            // my tweet

            final int UPDATE_TWEET = 1;
            final int COPY_LINK = 2;
            final int COPY_TEXT = 3;
            final int OPEN_TO_BROWSER = 4;
            final int DELETE_TWEET = 5;
            final int SAVE_TWEET = 6;

            menu.getMenu().add(Menu.NONE, UPDATE_TWEET, Menu.NONE, context.getString(R.string.update_tweet));

            if (FeatureFlags.SAVED_TWEETS) {
                menu.getMenu().add(Menu.NONE, SAVE_TWEET, Menu.NONE, context.getString(tweetIsSaved ? R.string.remove_from_saved_tweets : R.string.save_for_later));
            }

            menu.getMenu().add(Menu.NONE, COPY_LINK, Menu.NONE, context.getString(R.string.copy_link));
            menu.getMenu().add(Menu.NONE, COPY_TEXT, Menu.NONE, context.getString(R.string.menu_copy_text));
            menu.getMenu().add(Menu.NONE, OPEN_TO_BROWSER, Menu.NONE, context.getString(R.string.open_to_browser));
            menu.getMenu().add(Menu.NONE, DELETE_TWEET, Menu.NONE, context.getString(R.string.menu_delete_tweet));

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case UPDATE_TWEET:
                            updateTweet();
                            break;
                        case SAVE_TWEET:
                            if (tweetIsSaved) {
                                removeSavedTweet();
                            } else {
                                saveTweet();
                            }
                            break;
                        case DELETE_TWEET:
                            new DeleteTweet(new Runnable() {
                                @Override
                                public void run() {
                                    AppSettings.getInstance(context).sharedPrefs
                                            .edit().putBoolean("just_muted", true).apply();

                                    ((Activity)context).finish();

                                    if (context instanceof DrawerActivity) {
                                        context.startActivity(new Intent(context, MainActivity.class));
                                        ((Activity) context).overridePendingTransition(0,0);
                                    }
                                }
                            }).execute();

                            break;
                        case COPY_LINK:
                            copyLink();
                            break;
                        case COPY_TEXT:
                            copyText();
                            break;
                        case OPEN_TO_BROWSER:
                            openToBrowser();
                            break;
                    }
                    return false;
                }
            });
        } else {
            // someone else's tweet

            final int UPDATE_TWEET = 1;
            final int COPY_LINK = 2;
            final int COPY_TEXT = 3;
            final int OPEN_TO_BROWSER = 4;
            final int TRANSLATE = 5;
            final int MARK_SPAM = 6;
            final int SAVE_TWEET = 7;

            menu.getMenu().add(Menu.NONE, UPDATE_TWEET, Menu.NONE, context.getString(R.string.update_tweet));

            if (FeatureFlags.SAVED_TWEETS) {
                menu.getMenu().add(Menu.NONE, SAVE_TWEET, Menu.NONE, context.getString(tweetIsSaved ? R.string.remove_from_saved_tweets : R.string.save_for_later));
            }

            menu.getMenu().add(Menu.NONE, COPY_LINK, Menu.NONE, context.getString(R.string.copy_link));
            menu.getMenu().add(Menu.NONE, COPY_TEXT, Menu.NONE, context.getString(R.string.menu_copy_text));
            menu.getMenu().add(Menu.NONE, OPEN_TO_BROWSER, Menu.NONE, context.getString(R.string.open_to_browser));
            menu.getMenu().add(Menu.NONE, TRANSLATE, Menu.NONE, context.getString(R.string.menu_translate));
            menu.getMenu().add(Menu.NONE, MARK_SPAM, Menu.NONE, context.getString(R.string.menu_spam));

            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case UPDATE_TWEET:
                            updateTweet();
                            break;
                        case SAVE_TWEET:
                            if (tweetIsSaved) {
                                removeSavedTweet();
                            } else {
                                saveTweet();
                            }
                            break;
                        case COPY_LINK:
                            copyLink();
                            break;
                        case COPY_TEXT:
                            copyText();
                            break;
                        case TRANSLATE:
                            String url;

                            try {
                                url = settings.translateUrl + URLEncoder.encode(tweet, "utf-8");
                            } catch (UnsupportedEncodingException e) {
                                url = settings.translateUrl + tweet;
                            }

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
                            webPopup.show();
                            break;
                        case MARK_SPAM:
                            new AlertDialog.Builder(context)
                                    .setMessage(R.string.are_you_sure_spam)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            new MarkSpam(new Runnable() {
                                                @Override
                                                public void run() {
                                                    AppSettings.getInstance(context).sharedPrefs
                                                            .edit().putBoolean("just_muted", true).apply();

                                                    ((Activity)context).finish();

                                                    if (context instanceof DrawerActivity) {
                                                        context.startActivity(new Intent(context, MainActivity.class));
                                                        ((Activity) context).overridePendingTransition(0,0);
                                                    }
                                                }
                                            }).execute();
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

                            break;
                        case OPEN_TO_BROWSER:
                            openToBrowser();
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

    private void updateTweet() {
        convoSpinner.setVisibility(View.VISIBLE);
        convoProgress.setVisibility(View.VISIBLE);
        convoTweetArea.removeAllViews();
        convoCard.setVisibility(View.GONE);
        repliesButton.setVisibility(View.GONE);

        replies = new ArrayList<>();
        isRunning = true;
        firstRun = true;
        cardShown = false;
        query = null;
        adapter = null;

        getInfo();
    }

    private void saveTweet() {
        SavedTweetsDataSource.getInstance(context).createTweet(status, settings.currentAccount);
        context.sendBroadcast(new Intent(SavedTweetsFragment.REFRESH_ACTION));

        SharedPreferences sharedPreferences = AppSettings.getSharedPreferences(context);
        if (sharedPreferences.getBoolean("alert_save_tweet", true)) {
            sharedPreferences.edit().putBoolean("alert_save_tweet", false).apply();
            new AlertDialog.Builder(context)
                    .setMessage(R.string.saved_tweet_description)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (context instanceof Activity) {
                                ((Activity) context).finish();
                            }
                        }
                    }).show();
        } else {
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }

    private void removeSavedTweet() {
        SavedTweetsDataSource.getInstance(context).deleteTweet(status.getId());
        context.sendBroadcast(new Intent(SavedTweetsFragment.REFRESH_ACTION));

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    private void copyLink() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tweet_link", "https://twitter.com/" + screenName + "/status/" + id);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, R.string.copied_link, Toast.LENGTH_SHORT).show();
    }

    private void copyText() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tweet_text", tweetButtonUtils.restoreLinks(tweet));
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void openToBrowser() {
        Intent browser = new Intent(context, BrowserActivity.class);
        browser.putExtra("url","https://twitter.com/" + screenName + "/status/" + id);
        context.startActivity(browser);
    }

    public void setBackground(View v) {
        View background = v;

        background.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return hidePopups();
            }
        });
    }

    public void setInReplyToArea(LinearLayout inReplyToArea) {
        this.inReplyToArea = inReplyToArea;
        this.inReplyToTweets = (LinearLayout) inReplyToArea.findViewById(R.id.conversation_tweets);

        this.inReplyToArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return hidePopups();
            }
        });
    }

    public boolean hidePopups() {
        boolean hidden = false;
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
            if (interactionsPopup.isShowing()) {
                interactionsPopup.hide();
                hidden = true;
            }
        } catch (Exception e) {

        }

        return hidden;
    }

    private boolean secondAcc = false;
    public void setSecondAcc(boolean sec) {
        secondAcc = sec;
        tweetButtonUtils.setIsSecondAcc(sec);
    }

    private boolean fromNotification = false;
    public void fromNotification(boolean fromNotification) {
        this.fromNotification = true;
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

    private Status status = null;

    public void getInfo() {
        getInfo(fromNotification);
    }

    public void getInfo(final boolean fromNotification) {
        tweetButtonUtils.setUpShare(buttonsRoot, id, screenName, tweet);
        Thread getInfo = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                boolean tweetLoadedSuccessfully = false;
                try {
                    Twitter twitter =  getTwitter();

                    status = twitter.showStatus(id);

                    getConversationAndEmbeddedTweet();

                    if (status.isRetweet()) {
                        status = status.getRetweetedStatus();
                        id = status.getId();
                    }

                    tweetLoadedSuccessfully = true;
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (loadedCallback != null) {
                                loadedCallback.onLoad(status);
                            }
                        }
                    });
                } catch (Exception e) {
                    if (fromNotification) {
                        AnalyticsHelper.errorLoadingTweetFromNotification(context, e.getMessage());
                    }
                }

                final boolean loadSuccess = tweetLoadedSuccessfully;
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tweetButtonUtils.setUpButtons(status, id, countsView, buttonsRoot, true, loadSuccess);
                    }
                });
            }
        });

        getInfo.setPriority(Thread.MAX_PRIORITY);
        getInfo.start();
    }

    public void stop() {
        isRunning = false;
    }

    private void getConversationAndEmbeddedTweet() {
        Thread getConvo = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return;
                }

                Twitter twitter = getTwitter();

                try {
                    if (status.getQuotedStatus() != null) {
                        final Status embedded = twitter.showStatus(status.getQuotedStatusId());

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

                    if (status == null) {
                        return;
                    }

                    twitter4j.Status replyStatus = twitter.showStatus(status.getInReplyToStatusId());

                    try {
                        while(!replyStatus.getText().equals("")) {
                            if (!isRunning) {
                                return;
                            }
                            replies.add(replyStatus);

                            replyStatus = twitter.showStatus(replyStatus.getInReplyToStatusId());
                        }

                    } catch (Exception e) {
                        // the list of replies has ended, but we dont want to go to null
                    }

                } catch (TwitterException e) {
                    e.printStackTrace();
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

                                showInReplyToViews(reversed);

                                replies.clear();
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

        getConvo.setPriority(Thread.MAX_PRIORITY);
        getConvo.start();
    }

    public boolean isRunning = true;
    public List<Status> replies;
    public List<Status> profileTweets;
    private boolean foundProfileTweet = false;
    public TimelineArrayAdapter adapter;
    public Query query;
    private boolean cardShown = false;
    private boolean firstRun = true;

    private void getDiscussion() {
        Thread getReplies = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                if (!isRunning || (!firstRun && query == null)) {
                    return;
                }

                ArrayList<twitter4j.Status> all = null;
                Twitter twitter = getTwitter();
                try {
                    Log.v("talon_replies", "looking for discussion");

                    profileTweets = getTwitter().getUserTimeline(status.getUser().getId(), new Paging(1, 40));
                    for (int i = 0; i < profileTweets.size(); i++) {
                        if (profileTweets.get(i).getInReplyToStatusId() == status.getId()) {
                            replies.add(profileTweets.get(i));
                            // we will finish filling the replies from the chain search method
                            foundProfileTweet = true;
                            break;
                        }
                    }

                    long id = status.getId();
                    String screenname = status.getUser().getScreenName();

                    if (query == null) {
                        query = new Query("to:" + screenname);
                        query.setCount(30);

                        firstRun = false;
                    }

                    QueryResult result = twitter.search(query);

                    all = new ArrayList();

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
                                Status tweet = all.get(i);
                                boolean repliesContainsTweet = false;

                                for (Status s : replies) {
                                    if (s.getId() == tweet.getId()) {
                                        repliesContainsTweet = true;
                                        break;
                                    }
                                }

                                if (!repliesContainsTweet) {
                                    replies.add(tweet);
                                }

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
                                                adapter.setCanUseQuickActions(false);
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

                        query.setMaxId(SearchedTrendsActivity.getMaxIdFromList(tweets));

                        result = twitter.search(query);

                        if (replies.size() >= CONVO_CARD_LIST_SIZE && !cardShown) {
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

                        if (!repliesChangedOnThisIteration) {
                            repsWithoutChange++;
                        }

                    } while (query != null && isRunning && repsWithoutChange < 5 && replies.size() < MAX_TWEETS_IN_CONVERSATION);
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
                } else if (replies.size() < CONVO_CARD_LIST_SIZE) {
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

    private void startChainSearch(final List<TweetView> replies) {
        Thread chainSearch = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter = getTwitter();
                try {
                    for (int i = 0; i < replies.size(); i++) {
                        final TweetView status = replies.get(i);
                        final Status firstLevelReply = status.status;

                        final List<Status> filtered = new ArrayList<>();
                        long replyIdForNextTweet = firstLevelReply.getId();

                        List<Status> results;
                        if (i == 0 && profileTweets != null && profileTweets.size() != 0 && foundProfileTweet) {
                            // tweetstorm search
                            results = profileTweets;
                        } else {
                            String replyTweeter = firstLevelReply.getUser().getScreenName();
                            String originalTweeter = screenName;
                            String searchQuery = "((from:" + originalTweeter + " to:" + replyTweeter + ") OR " +
                                    "(from:" + replyTweeter + " to:" + originalTweeter + "))";
                            Query twitterQuery = new Query(searchQuery);
                            query.setCount(20);

                            results = twitter.search(twitterQuery).getTweets();
                        }

                        for (int j = results.size() - 1; j >= 0; j--) {
                            if (results.get(j).getInReplyToStatusId() == replyIdForNextTweet) {
                                filtered.add(results.get(j));
                                replyIdForNextTweet = results.get(j).getId();

                                results.remove(j);
                                j = results.size() - 1;
                            }
                        }

                        if (filtered.size() > 0) {
                            ((Activity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    LinearLayout discussionArea = (LinearLayout) status.getView().findViewById(R.id.replies);
                                    LinearLayout tweetViews = (LinearLayout) discussionArea.findViewById(R.id.inner_expansion);
                                    View line = discussionArea.findViewById(R.id.line);
                                    line.setBackgroundColor(settings.themeColors.accentColor);
                                    discussionArea.setVisibility(View.VISIBLE);

                                    for (Status s : filtered) {
                                        TweetView v = new TweetView(context, s);
                                        v.setCurrentUser(AppSettings.getInstance(context).myScreenName);
                                        v.setSmallImage(true);
                                        v.setUseSmallerMargins(true);

                                        if (filtered.indexOf(s) == filtered.size() - 1) {
                                            v.getView().findViewById(R.id.background).setPadding(0,0,0, Utils.toDP(16,context));
                                        }

                                        tweetViews.addView(v.getView());
                                    }
                                }
                            });
                        }
                    }
                } catch (TwitterException e) {
                    if (e.getMessage().contains("limit exceeded")) {
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "Cannot find conversation - rate limit reached.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception | OutOfMemoryError e) {
                    e.printStackTrace();
                }
            }
        });

        chainSearch.setPriority(8);
        chainSearch.start();
    }

    // expand collapse animation: http://stackoverflow.com/questions/4946295/android-expand-collapse-animation
    public void showInReplyToViews(List<twitter4j.Status> replies) {
        for (int i = 0; i < replies.size(); i++) {
            View statusView = new TweetView(context, replies.get(i)).setUseSmallerMargins(true).getView();
            statusView.findViewById(R.id.background).setPadding(0,Utils.toDP(12, context),0,Utils.toDP(12, context));

            inReplyToTweets.addView(statusView);

            if (i != replies.size() - 1) {
                View tweetDivider = new View(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context));
                tweetDivider.setLayoutParams(params);

                if (AppSettings.getInstance(context).darkTheme) {
                    tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.dark_text_drawer));
                } else {
                    tweetDivider.setBackgroundColor(context.getResources().getColor(R.color.light_text_drawer));
                }

                inReplyToTweets.addView(tweetDivider);
            }
        }

        inReplyToArea.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        inReplyToArea.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = inReplyToArea.getMeasuredHeight() +
                (settings.picturesType == AppSettings.CONDENSED_TWEETS || settings.picturesType == AppSettings.CONDENSED_NO_IMAGES
                        ? 0 : Utils.toDP(28, context));

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        inReplyToArea.getLayoutParams().height = 1;
        inReplyToArea.setVisibility(View.VISIBLE);
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                inReplyToArea.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                inReplyToArea.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationRepeat(Animation animation) { }
            @Override public void onAnimationStart(Animation animation) { }
            @Override public void onAnimationEnd(Animation animation) {
                readjustExpansionArea();
            }
        });

        // 1dp/ms
        //a.setDuration((int)(targetHeight / inReplyToArea.getContext().getResources().getDisplayMetrics().density));
        a.setDuration(200);
        inReplyToArea.startAnimation(a);
    }

    // used on the adapter
    // when the in reply to section is shown, it will create a giant white area at the bottom of the
    // screen that could be half the size. We get rid of that by readjusting the min height of the expansion
    View expandArea;
    public void setExpandArea(View expandArea) {
        this.expandArea = expandArea;
    }
    public void readjustExpansionArea() {
        if (expandArea != null) {
            expandArea.setMinimumHeight(expandArea.getMinimumHeight() - inReplyToArea.getMeasuredHeight());
            expandArea.requestLayout();
        }
    }

    public void removeInReplyToViews() {
        ValueAnimator heightAnimatorContent = ValueAnimator.ofInt(inReplyToArea.getHeight(), 0);
        heightAnimatorContent.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams params = inReplyToArea.getLayoutParams();
                params.height = val;
                inReplyToArea.setLayoutParams(params);

                if (val == 0) {
                    inReplyToTweets.removeAllViews();
                }
            }
        });
        heightAnimatorContent.setDuration(TimeLineCursorAdapter.ANIMATION_DURATION);
        heightAnimatorContent.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        heightAnimatorContent.start();

        ValueAnimator alpha = ValueAnimator.ofFloat(1f, 0f);
        alpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                inReplyToArea.setAlpha(val);

                if (val == 0f) {
                    inReplyToArea.setAlpha(1f);
                }
            }
        });
        alpha.setDuration((int) (TimeLineCursorAdapter.ANIMATION_DURATION * .75));
        alpha.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        alpha.start();

    }

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        Runnable onFinish;

        public DeleteTweet(Runnable onFinish) {
            this.onFinish = onFinish;
        }

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {

                HomeDataSource.getInstance(context).deleteTweet(id);
                MentionsDataSource.getInstance(context).deleteTweet(id);
                ListDataSource.getInstance(context).deleteTweet(id);

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

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).apply();
            onFinish.run();
        }
    }

    class MarkSpam extends AsyncTask<String, Void, Boolean> {

        Runnable onFinish;

        public MarkSpam(Runnable onFinish) {
            this.onFinish = onFinish;
        }

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = getTwitter();

            try {
                HomeDataSource.getInstance(context).deleteTweet(id);
                MentionsDataSource.getInstance(context).deleteTweet(id);
                ListDataSource.getInstance(context).deleteTweet(id);

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

                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).apply();

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

            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("refresh_me", true).apply();

            onFinish.run();
        }
    }
}
