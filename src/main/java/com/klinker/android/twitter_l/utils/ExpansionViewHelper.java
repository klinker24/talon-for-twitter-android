package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ArrayListLoader;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.ConversationPopupLayout;
import com.klinker.android.twitter_l.manipulations.MobilizedWebPopupLayout;
import com.klinker.android.twitter_l.manipulations.RetweetersPopupLayout;
import com.klinker.android.twitter_l.manipulations.WebPopupLayout;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.compose.ComposeSecAccActivity;
import com.klinker.android.twitter_l.utils.api_helper.TwitterMultipleImageHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
import twitter4j.conf.Configuration;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpansionViewHelper {

    Context context;
    public long id;

    // root view
    View expansion;

    // background that touching will dismiss the popups
    View background;

    // manage the favorite stuff
    TextView favCount;
    TextView favText;
    View favoriteButton; // linear layout

    // manage the retweet stuff
    TextView retweetCount;
    TextView retweetText;
    View retweetButton; // linear layout
    NetworkedCacheableImageView[] retweeters;
    View viewRetweeters;

    // buttons at the bottom
    ImageButton webButton;
    View repliesButton;
    View composeButton;
    View overflowButton;

    AsyncListView replyList;
    LinearLayout convoSpinner;
    View convoLayout;

    RetweetersPopupLayout retweetersPopup;
    ConversationPopupLayout convoPopup;
    MobilizedWebPopupLayout mobilizedPopup;
    WebPopupLayout webPopup;

    boolean landscape;

    public ExpansionViewHelper(Context context, long tweetId) {
        this.context = context;
        this.id = tweetId;

        // get the base view
        expansion = ((Activity)context).getLayoutInflater().inflate(R.layout.tweet_expansion, null, false);

        landscape = context.getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;

        setViews();
        setClicks();
        getInfo();
    }

    private void setViews() {
        retweeters = new NetworkedCacheableImageView[3];

        favCount = (TextView) expansion.findViewById(R.id.fav_count);
        favText = (TextView) expansion.findViewById(R.id.favorite_text);
        favoriteButton = expansion.findViewById(R.id.favorite);

        retweetCount = (TextView) expansion.findViewById(R.id.retweet_count);
        retweetText = (TextView) expansion.findViewById(R.id.retweet_text);
        retweetButton = expansion.findViewById(R.id.retweet);
        viewRetweeters = expansion.findViewById(R.id.view_retweeters);

        retweeters[0] = (NetworkedCacheableImageView) retweetButton.findViewById(R.id.retweeter_1);
        retweeters[1] = (NetworkedCacheableImageView) retweetButton.findViewById(R.id.retweeter_2);
        retweeters[2] = (NetworkedCacheableImageView) retweetButton.findViewById(R.id.retweeter_3);

        webButton = (ImageButton) expansion.findViewById(R.id.web_button);
        repliesButton = expansion.findViewById(R.id.conversation_button);
        composeButton = expansion.findViewById(R.id.compose_button);
        overflowButton = expansion.findViewById(R.id.overflow_button);

        convoLayout = ((Activity)context).getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        replyList = (AsyncListView) convoLayout.findViewById(R.id.listView);
        convoSpinner = (LinearLayout) convoLayout.findViewById(R.id.spinner);

        for (int i = 0; i < 3; i++) {
            retweeters[i].setClipToOutline(true);
        }

        retweetersPopup = new RetweetersPopupLayout(context);
        if (context.getResources().getBoolean(R.bool.isTablet)) {
            retweetersPopup.setWidthByPercent(.4f);
        } else {
            retweetersPopup.setWidthByPercent(.6f);
        }
        retweetersPopup.setHeightByPercent(.4f);


        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        replyList.setItemManager(builder.build());
    }

    private void setClicks() {

        viewRetweeters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (retweetersPopup != null) {
                    retweetersPopup.setOnTopOfView(viewRetweeters);
                    retweetersPopup.setExpansionPointForAnim(viewRetweeters);
                    retweetersPopup.show();
                }
            }
        });

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                favoriteStatus();
            }
        });

        retweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retweetStatus();
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

        repliesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status != null) {
                    getConversation();
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
                    convoPopup.setExpansionPointForAnim(view);
                    convoPopup.show();
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

                    final LinearLayout main = (LinearLayout) ((Activity)context).getLayoutInflater().inflate(R.layout.mobilized_fragment, null, false);
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
                    mobilizedPopup.show();
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
                    webPopup.show();
                }
            }
        });
    }

    private void shareClick() {
        String text1 = tweetText;
        text1 = text1 + "\n\n" + "https://twitter.com/" + screenName + "/status/" + id;
        Log.v("my_text_on_share", text1);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text1);

        ((Activity)context).getWindow().setExitTransition(null);

        context.startActivity(share);
    }

    String webLink = null;
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

        if (webLink == null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.shareButton});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            webButton.setImageResource(resource);
            shareOnWeb = true;
        }

        webButton.setEnabled(true);
        webButton.setAlpha(1.0f);
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

            final int QUOTE_TWEET = 1;
            final int COPY_TEXT = 2;
            final int MARK_SPAM = 3;
            final int TRANSLATE = 4;
            final int SHARE = 5;

            menu.getMenu().add(Menu.NONE, QUOTE_TWEET, Menu.NONE, context.getString(R.string.menu_quote));
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
                        case QUOTE_TWEET:
                            String text = tweet;

                            if (!AppSettings.getInstance(context).preferRT) {
                                text = "\"@" + screenName + ": " + restoreLinks(text) + "\" ";
                            } else {
                                text = " RT @" + screenName + ": " + restoreLinks(text);
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

                            ((Activity)context).getWindow().setExitTransition(null);

                            context.startActivity(quote);
                            break;
                        case COPY_TEXT:
                            copyText();
                            break;
                        case SHARE:
                            shareClick();
                            break;
                        case TRANSLATE:
                            try {
                                String query = tweet.replaceAll(" ", "+");
                                String url = "http://translate.google.com/#auto|en|" + tweet;
                                Uri uri = Uri.parse(url);

                                Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                                browser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                ((Activity)context).getWindow().setExitTransition(null);

                                context.startActivity(browser);
                            } catch (Exception e) {

                            }
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

    private void copyText() {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("tweet_text", tweet);
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

    boolean isFavorited = false;
    boolean isRetweeted = false;

    public void favoriteStatus() {
        if (!isFavorited) {
            Toast.makeText(context, context.getResources().getString(R.string.favoriting_status), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, context.getResources().getString(R.string.removing_favorite), Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Twitter twitter =  getTwitter();
                    if (isFavorited) {
                        twitter.destroyFavorite(id);
                    } else {
                        twitter.createFavorite(id);
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

    public void retweetStatus() {
        Toast.makeText(context, context.getResources().getString(R.string.retweeting_status), Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  getTwitter();
                    twitter.retweetStatus(id);

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
                    Twitter twitter =  getTwitter();

                    status = twitter.showStatus(id);

                    final String sfavCount;
                    if (status.isRetweet()) {
                        twitter4j.Status status2 = status.getRetweetedStatus();

                        sfavCount = status2.getFavoriteCount() + "";
                    } else {
                        sfavCount = status.getFavoriteCount() + "";
                    }

                    isRetweeted = status.isRetweetedByMe();
                    final String retCount = "" + status.getRetweetCount();

                    if (status.getRetweetCount() > 0) {
                        getRetweeters();
                    } else {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                viewRetweeters.setVisibility(View.GONE);
                            }
                        });
                    }

                    final Status fStatus = status;

                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
                            int textColor = a.getResourceId(0, 0);
                            a.recycle();

                            retweetCount.setText(" " + retCount);

                            if (isRetweeted) {
                                retweetText.setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
                            } else {
                                retweetText.setTextColor(context.getResources().getColor(textColor));
                            }

                            favCount.setText(" " + sfavCount);

                            if (fStatus.isFavorited()) {
                                favText.setTextColor(AppSettings.getInstance(context).themeColors.accentColor);
                                isFavorited = true;
                            } else {
                                favText.setTextColor(context.getResources().getColor(textColor));
                                isFavorited = false;
                            }
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
                            } else {
                                retweetText.setTextColor(context.getResources().getColor(textColor));
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
                                isFavorited = true;
                            } else {
                                favText.setTextColor(context.getResources().getColor(textColor));
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

        private long tweetId;
        private TextView retweetText;

        public RemoveRetweet() {
            this.tweetId = tweetId;
            this.retweetText = retweetText;
        }

        protected void onPreExecute() {
            Toast.makeText(context, context.getResources().getString(R.string.removing_retweet), Toast.LENGTH_SHORT).show();
        }

        protected Boolean doInBackground(String... urls) {
            try {
                AppSettings settings = AppSettings.getInstance(context);
                Twitter twitter =  getTwitter();
                ResponseList<twitter4j.Status> retweets = twitter.getRetweets(tweetId);
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

    public void getConversation() {
        Thread getConvo = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return;
                }

                Twitter twitter = getTwitter();
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

    public void getDiscussion() {

        Thread getReplies = new Thread(new Runnable() {
            @Override
            public void run() {

                if (!isRunning) {
                    return;
                }

                ArrayList<twitter4j.Status> all = null;
                Twitter twitter = getTwitter();
                try {
                    Log.v("talon_replies", "looking for discussion");

                    long id = status.getId();
                    String screenname = status.getUser().getScreenName();

                    query = new Query("@" + screenname + " since_id:" + id);

                    Log.v("talon_replies", "query string: " + query.getQuery());

                    try {
                        query.setCount(30);
                    } catch (Throwable e) {
                        // enlarge buffer error?
                        query.setCount(30);
                    }

                    QueryResult result = twitter.search(query);
                    Log.v("talon_replies", "result: " + result.getTweets().size());

                    all = new ArrayList<twitter4j.Status>();

                    do {
                        Log.v("talon_replies", "do loop repetition");
                        if (!isRunning) {
                            return;
                        }
                        List<Status> tweets = result.getTweets();

                        for(twitter4j.Status tweet : tweets){
                            if (tweet.getInReplyToStatusId() == id) {
                                all.add(tweet);
                                Log.v("talon_replies", tweet.getText());
                            }
                        }

                        if (all.size() > 0) {
                            for (int i = all.size() - 1; i >= 0; i--) {
                                Log.v("talon_replies", "inserting into arraylist:" + all.get(i).getText());
                                replies.add(all.get(i));
                            }

                            all.clear();

                            ((Activity)context).runOnUiThread(new Runnable() {
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
                                        } else {
                                            disableConvoButton();
                                            Toast.makeText(context, R.string.no_replies, Toast.LENGTH_SHORT).show();
                                            try {
                                                convoPopup.hide();
                                            } catch (Exception e) {

                                            }
                                        }
                                    } catch (Exception e) {
                                        // none and it got the null object
                                        e.printStackTrace();
                                        if (replies != null && replies.size() == 0) {
                                            disableConvoButton();
                                            Toast.makeText(context, R.string.no_replies, Toast.LENGTH_SHORT).show();
                                            try {
                                                convoPopup.hide();
                                            } catch (Exception x) {

                                            }
                                        }
                                    }
                                }
                            });
                        }

                        try {
                            Thread.sleep(250);
                        } catch (Exception e) {
                            // since we are changing the arraylist for the adapter in the background, we need to make sure it
                            // gets updated before continuing
                        }

                        query = result.nextQuery();

                        if (query != null)
                            result = twitter.search(query);

                    } while (query != null);

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
                            disableConvoButton();
                            Toast.makeText(context, R.string.no_replies, Toast.LENGTH_SHORT).show();
                            try {
                                convoPopup.hide();
                            } catch (Exception e) {

                            }
                        }
                    });
                }
            }
        });

        getReplies.setPriority(8);
        getReplies.start();

    }

    public void disableConvoButton() {
        if (repliesButton != null) {
            repliesButton.setEnabled(false);
            repliesButton.setAlpha(.5f);
        }
    }

    public void getRetweeters() {
        Thread getRetweeters = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  getTwitter();

                    Status stat = status;
                    if (stat.isRetweet()) {
                        id = stat.getRetweetedStatus().getId();
                    }

                    // can get 100 retweeters is all
                    ResponseList<twitter4j.Status> lists = twitter.getRetweets(id);

                    final ArrayList<String> urls = new ArrayList<String>();
                    final ArrayList<User> users = new ArrayList<User>();

                    for (Status s : lists) {
                        users.add(s.getUser());
                        urls.add(s.getUser().getBiggerProfileImageURL());
                    }

                    if (urls.size() > 3) {
                        urls.subList(0, 2);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            retweetersPopup.setData(users);

                            for (int i = 0; i < (context.getResources().getBoolean(R.bool.isTablet) ? 3 : 2); i++) {
                                try {
                                    retweeters[i].loadImage(urls.get(i), false, null);
                                } catch (Exception e) {
                                    retweeters[i].setVisibility(View.GONE);
                                }
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
                                spinner.setVisibility(View.GONE);
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

        if (otherLink.length > 0) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];

                //if (Patterns.WEB_URL.matcher(s).find()) { // we know the link is cut off
                if (s.contains("...")) { // we know the link is cut off
                    String f = s.replace("...", "").replace("http", "");

                    f = stripTrailingPeriods(f);

                    for (int x = 0; x < otherLink.length; x++) {
                        if (otherLink[x].toLowerCase().contains(f.toLowerCase())) {
                            changed = true;
                            // for some reason it wouldn't match the last "/" on a url and it was stopping it from opening
                            try {
                                if (otherLink[x].substring(otherLink[x].length() - 1, otherLink[x].length()).equals("/")) {
                                    otherLink[x] = otherLink[x].substring(0, otherLink[x].length() - 1);
                                }
                                f = otherLink[x].replace("http://", "").replace("https://", "").replace("www.", "");
                                otherLink[x] = "";
                            } catch (Exception e) {
                                // out of bounds exception?
                            }
                            break;
                        }
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
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                s = s.replace("...", "");

                if (Patterns.WEB_URL.matcher(s).find() && (s.startsWith("t.co/") || s.contains("twitter.com/"))) { // we know the link is cut off
                    String replace = otherLinks[otherLinks.length - 1];
                    if (replace.replace(" ", "").equals("")) {
                        replace = webLink;
                    }
                    split[i] = replace;
                    changed = true;
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

        return full;
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
