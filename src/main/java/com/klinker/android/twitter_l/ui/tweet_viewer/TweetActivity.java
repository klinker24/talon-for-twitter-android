package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.app.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.util.Log;
import android.util.Patterns;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.*;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.manipulations.*;
import com.klinker.android.twitter_l.manipulations.widgets.*;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.profile_viewer.ProfilePager;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetYouTubeFragment;
import com.klinker.android.twitter_l.utils.EmojiUtils;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.klinker.android.twitter_l.utils.api_helper.TwitterMultipleImageHelper;
import com.klinker.android.twitter_l.utils.text.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class TweetActivity extends YouTubeBaseActivity {

    public Context context;
    public AppSettings settings;
    public SharedPreferences sharedPrefs;

    private TextView timetv;

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

    protected boolean fromLauncher = false;

    public WebPopupLayout webPopup = null;
    public MobilizedWebPopupLayout mobilizedPopup = null;
    public RetweetersPopupLayout retweetersPopup = null;
    public ConversationPopupLayout convoPopup = null;

    public AsyncListView replyList;
    public LinearLayout convoSpinner;

    public View dim;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().requestFeature(Window.FEATURE_PROGRESS);
        } catch (Exception e) {

        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        context = this;
        settings = AppSettings.getInstance(this);

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

        getFromIntent();

        Utils.setUpTweetTheme(context, settings);

        setContentView(R.layout.tweet_activity);

        final View convo = getLayoutInflater().inflate(R.layout.convo_popup_layout, null, false);
        replyList = (AsyncListView) convo.findViewById(R.id.listView);
        convoSpinner = (LinearLayout) convo.findViewById(R.id.spinner);
        dim = findViewById(R.id.dim);

        setUpTheme();

        setUIElements(getWindow().getDecorView().findViewById(android.R.id.content));

        boolean youtube = false;

        ArrayList<String> webpages = new ArrayList<String>();

        if (otherLinks == null) {
            otherLinks = new String[0];
        }

        boolean hasWebpage;
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

        ImageButton webButton = (ImageButton) findViewById(R.id.web_button);
        if (hasWebpage && (settings.alwaysMobilize ||
                (Utils.getConnectionStatus(context) && settings.mobilizeOnData))) {

            final LinearLayout main = (LinearLayout) getLayoutInflater().inflate(R.layout.mobilized_fragment, null, false);
            final ScrollView scrollView = (ScrollView) main.findViewById(R.id.scrollview);
            View spinner = main.findViewById(R.id.spinner);
            HoloTextView mobilizedBrowser = (HoloTextView) scrollView.findViewById(R.id.webpage_text);
            getTextFromSite(webpages.get(0), mobilizedBrowser, spinner, scrollView);

            webButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!hidePopups()) {
                        if (mobilizedPopup == null) {
                            mobilizedPopup = new MobilizedWebPopupLayout(context, main);
                        }
                        mobilizedPopup.show();
                        showDim();
                    }
                }
            });
        } else if (hasWebpage) {
            final LinearLayout webLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.web_popup_layout, null, false);
            final WebView web = (WebView) webLayout.findViewById(R.id.webview);
            web.loadUrl(webpages.get(0));
            //webSpinner.setVisibility(View.GONE);

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

            webButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!hidePopups()) {
                        if (webPopup == null) {
                            webPopup = new WebPopupLayout(context, webLayout);
                        }
                        webPopup.show();
                        showDim();
                    }
                }
            });

        } else {
            webButton.setEnabled(false);
            webButton.setAlpha(.5f);
        }

        // youtube player isn't working right now
        if (false){//youtube) {
            TweetYouTubeFragment frag = new TweetYouTubeFragment(settings, youtubeVideo);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.youtube_view, frag);
            ft.commit();
        } else {
            findViewById(R.id.youtube_divider).setVisibility(View.GONE);
            findViewById(R.id.youtube_text).setVisibility(View.GONE);
        }


        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        replyList.setItemManager(builder.build());

        viewReplyButton = (ImageButton) findViewById(R.id.conversation_button);
        viewReplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    if (convoPopup == null) {
                        convoPopup = new ConversationPopupLayout(context, convo);
                    }
                    convoPopup.show();
                    showDim();
                }
            }
        });

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
                                browser.setTextSize(settings.textSize);
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
                                    browser.setText(getResources().getString(R.string.error_loading_page));
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
                                    browser.setText(getResources().getString(R.string.error_loading_page));
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

    String youtubeVideo = "";

    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if (!hidePopups()) {
            hideExtraContent();
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getWindow().setExitTransition(new Slide(3));

        dim.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();

        hidePopups();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    @Override
    public void finish() {
        isRunning = false;
        super.finish();
    }

    public boolean hidePopups() {
        hideDim();

        if (retweetersPopup != null && retweetersPopup.isShowing()) {
            retweetersPopup.hide();
            return true;
        } else if (webPopup != null && webPopup.isShowing()) {
            webPopup.hide();
            return true;
        } else if (mobilizedPopup != null && mobilizedPopup.isShowing()) {
            mobilizedPopup.hide();
            return true;
        } else if (convoPopup != null && convoPopup.isShowing()) {
            convoPopup.hide();
            return true;
        }

        return false;
    }

    public void showDim() {
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dim.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(300);
        dim.startAnimation(anim);
    }

    public void hideDim() {
        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dim.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(300);
        dim.startAnimation(anim);
    }

    public View insetsBackground;

    public void setUpTheme() {

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(new ColorDrawable(android.R.color.transparent));
        actionBar.setTitle("");
        actionBar.setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        insetsBackground = findViewById(R.id.actionbar_and_status_bar);

        ViewGroup.LayoutParams statusParams = insetsBackground.getLayoutParams();
        statusParams.height = Utils.getActionBarHeight(this) + Utils.getStatusBarHeight(this);
        insetsBackground.setLayoutParams(statusParams);
        insetsBackground.setAlpha(0);

        final int abHeight = Utils.getActionBarHeight(context);
        final int sbHeight = Utils.getStatusBarHeight(context);
        final View header = findViewById(R.id.profile_pic_contact);

        View status = findViewById(R.id.status_bar);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) status.getLayoutParams();
        params.height = sbHeight;
        status.setLayoutParams(params);

        View action = findViewById(R.id.actionbar_bar);
        params = (LinearLayout.LayoutParams) action.getLayoutParams();
        params.height = abHeight;
        action.setLayoutParams(params);

        action.setBackgroundColor(settings.themeColors.primaryColor);
        status.setBackgroundColor(settings.themeColors.primaryColorDark);

        final NotifyScrollView scroll = (NotifyScrollView) findViewById(R.id.notify_scroll_view);
        scroll.setOnScrollChangedListener(new NotifyScrollView.OnScrollChangedListener() {
            @Override
            public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
                final int headerHeight = header.getHeight() - abHeight;
                final float ratio = (float) Math.min(Math.max(t, 0), headerHeight) / headerHeight;
                insetsBackground.setAlpha(ratio);
            }
        });
        scroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (view instanceof ImageButton) {
                    return false;
                }
                if (retweetersPopup != null && retweetersPopup.isShowing()) {
                    retweetersPopup.hide();
                    hideDim();
                    return true;
                } else if (webPopup != null && webPopup.isShowing()) {
                    webPopup.hide();
                    hideDim();
                    return true;
                } else if (mobilizedPopup != null && mobilizedPopup.isShowing()) {
                    mobilizedPopup.hide();
                    hideDim();
                    return true;
                } else if (convoPopup != null && convoPopup.isShowing()) {
                    convoPopup.hide();
                    hideDim();
                    return true;
                } else {
                    return false;
                }
            }
        });

        View navBarSeperator = findViewById(R.id.nav_bar_seperator);
        LinearLayout.LayoutParams navBar = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
        navBarSeperator.setLayoutParams(navBar);

        if (Utils.hasNavBar(context) && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            navBarSeperator.setVisibility(View.VISIBLE);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            navBarSeperator.setVisibility(View.GONE);
        }

        if (getResources().getBoolean(R.bool.isTablet)) {
            navBarSeperator.setVisibility(View.VISIBLE);
        }
    }

    public void setUpWindow(boolean youtube) {

        requestWindowFeature(Window.FEATURE_ACTION_BAR | Window.FEATURE_PROGRESS);
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
            linkString = from.getStringExtra("other_links");
            otherLinks = linkString.split("  ");
        } catch (Exception e) {
            otherLinks = null;
        }

        if (screenName.equals(settings.myScreenName)) {
            isMyTweet = true;
        } else if (screenName.equals(retweeter)) {
            isMyRetweet = true;
        }

        tweet = restoreLinks(tweet);

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

    class DeleteTweet extends AsyncTask<String, Void, Boolean> {

        protected void onPreExecute() {
            finish();
        }

        protected Boolean doInBackground(String... urls) {
            Twitter twitter = Utils.getTwitter(context, settings);

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
            Twitter twitter = Utils.getTwitter(context, settings);

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
        String text1 = tweet;
        text1 = "@" + screenName + ": " + text1 + "\n\n" + "https://twitter.com/" + screenName + "/status/" + tweetId;
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text1);
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Tweet by @" + screenName);
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
        final int MENU_SPAM = 6;

        if (!isMyTweet) {
            menu.getItem(MENU_DELETE_TWEET).setVisible(false);
        } else {
            menu.getItem(MENU_QUOTE).setVisible(false);
            menu.getItem(MENU_SPAM).setVisible(false);
        }

        /*if (!mSectionsPagerAdapter.getHasWebpage()) {
            menu.getItem(MENU_OPEN_WEB).setVisible(false);
        }*/

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
                getSharedPreferences("com.klinker.android.twitter_world_preferences",
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                        .edit().putBoolean("just_muted", true).commit();
                return true;

            case R.id.menu_share:
                String text1 = tweet;
                text1 = "@" + screenName + ": " + text1 + "\n\n" + "https://twitter.com/" + screenName + "/status/" + tweetId;
                Log.v("my_text_on_share", text1);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, text1);

                getWindow().setExitTransition(null);

                startActivity(share);
                return true;

            case R.id.menu_copy_text:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("tweet_text", tweet);
                clipboard.setPrimaryClip(clip);
                return true;

            case R.id.menu_open_web:
                Uri weburi;
                try {
                    weburi = Uri.parse(otherLinks[0]);
                } catch (Exception e) {
                    weburi = Uri.parse(webpage);
                }
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);

                getWindow().setExitTransition(null);

                startActivity(launchBrowser);

                return true;

            case R.id.menu_save_image:

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        Looper.prepare();

                        try {
                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(context)
                                            .setSmallIcon(R.drawable.ic_stat_icon)
                                            .setTicker(getResources().getString(R.string.downloading) + "...")
                                            .setContentTitle(getResources().getString(R.string.app_name))
                                            .setContentText(getResources().getString(R.string.saving_picture) + "...")
                                            .setProgress(100, 100, true)
                                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_save));

                            NotificationManager mNotificationManager =
                                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(6, mBuilder.build());

                            String url = webpage;
                            if (webpage.contains("insta")) {
                                url = url.substring(0, url.length() - 1) + "l";
                            }
                            URL mUrl = new URL(url);

                            Bitmap bitmap = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());

                            Random generator = new Random();
                            int n = 1000000;
                            n = generator.nextInt(n);
                            String fname = "Image-" + n;

                            Uri uri = IOUtils.saveImage(bitmap, fname, context);
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "image/*");

                            PendingIntent pending = PendingIntent.getActivity(context, 91, intent, 0);

                            mBuilder =
                                    new NotificationCompat.Builder(context)
                                            .setContentIntent(pending)
                                            .setSmallIcon(R.drawable.ic_stat_icon)
                                            .setTicker(getResources().getString(R.string.saved_picture) + "...")
                                            .setContentTitle(getResources().getString(R.string.app_name))
                                            .setContentText(getResources().getString(R.string.saved_picture) + "!")
                                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_save));

                            mNotificationManager.notify(6, mBuilder.build());
                        } catch (Exception e) {
                            e.printStackTrace();
                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(context)
                                            .setSmallIcon(R.drawable.ic_stat_icon)
                                            .setTicker(getResources().getString(R.string.error) + "...")
                                            .setContentTitle(getResources().getString(R.string.app_name))
                                            .setContentText(getResources().getString(R.string.error) + "...")
                                            .setProgress(100, 100, true)
                                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_save));

                            NotificationManager mNotificationManager =
                                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(6, mBuilder.build());
                        }
                    }
                }).start();

                return true;

            case R.id.menu_quote:
                String text = tweet;

                if (!settings.preferRT) {
                    text = "\"@" + screenName + ": " + text + "\" ";
                } else {
                    text = " RT @" + screenName + ": " + text;
                }

                Intent quote = new Intent(context, ComposeActivity.class);
                quote.putExtra("user", text);
                quote.putExtra("id", tweetId);

                getWindow().setExitTransition(null);

                startActivity(quote);

                return true;

            case R.id.menu_spam:
                new MarkSpam().execute();
                getSharedPreferences("com.klinker.android.twitter_world_preferences",
                        Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                        .edit().putBoolean("just_muted", true).commit();
                return super.onOptionsItemSelected(item);

            case R.id.menu_mute_hashtags:
                if (!hashtags[0].equals("")) {
                    ArrayList<String> tags = new ArrayList<String>();
                    if (hashtags != null) {
                        for (String s : hashtags) {
                            if (!s.equals("")) {
                                tags.add("#" + s);
                            }
                        }
                    }

                    final CharSequence[] fItems = new CharSequence[tags.size()];

                    for (int i = 0; i < tags.size(); i++) {
                        fItems[i] = tags.get(i);
                    }

                    final SharedPreferences sharedPreferences = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                            Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);

                    if (fItems.length > 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setItems(fItems, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String touched = fItems[item] + "";
                                Toast.makeText(context, getResources().getString(R.string.muted) + " " + touched, Toast.LENGTH_SHORT).show();
                                touched = touched.replace("#", "") + " ";

                                String current = sharedPreferences.getString("muted_hashtags", "");
                                sharedPreferences.edit().putString("muted_hashtags", current + touched).commit();
                                sharedPreferences.edit().putBoolean("refresh_me", true).commit();

                                dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        String touched = fItems[0] + "";
                        Toast.makeText(context, getResources().getString(R.string.muted) + " " + touched, Toast.LENGTH_SHORT).show();
                        touched = touched.replace("#", "") + " ";

                        String current = sharedPreferences.getString("muted_hashtags", "");
                        sharedPreferences.edit().putString("muted_hashtags", current + touched).commit();
                        sharedPreferences.edit().putBoolean("refresh_me", true).commit();

                    }
                } else {
                    Toast.makeText(context, getResources().getString(R.string.no_hashtags), Toast.LENGTH_SHORT).show();
                }

                PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("just_muted", true).commit();
                return super.onOptionsItemSelected(item);

            case R.id.menu_share_links:
                if (!otherLinks[0].equals("")) {
                    ArrayList<String> urls = new ArrayList<String>();
                    if (otherLinks != null) {
                        for (String s : otherLinks) {
                            if (!s.equals("")) {
                                urls.add(s);
                            }
                        }
                    }

                    final CharSequence[] fItems = new CharSequence[urls.size()];

                    for (int i = 0; i < urls.size(); i++) {
                        fItems[i] = urls.get(i);
                    }

                    if (fItems.length > 1) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setItems(fItems, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String touched = fItems[item] + "";

                                Intent intent=new Intent(android.content.Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                intent.putExtra(Intent.EXTRA_TEXT, touched);
                                getWindow().setExitTransition(null);
                                context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.menu_share)));

                                dialog.dismiss();
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    } else {
                        String touched = fItems[0] + "";

                        Intent intent=new Intent(android.content.Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        intent.putExtra(Intent.EXTRA_TEXT, touched);
                        getWindow().setExitTransition(null);
                        context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.menu_share)));
                    }
                } else {
                    Toast.makeText(context, getResources().getString(R.string.no_links), Toast.LENGTH_SHORT).show();
                }
                return super.onOptionsItemSelected(item);
            case R.id.menu_translate:
                try {
                    String query = tweet.replaceAll(" ", "+");
                    String url = "http://translate.google.com/#auto|en|" + tweet;
                    Uri uri = Uri.parse(url);

                    Intent browser = new Intent(Intent.ACTION_VIEW, uri);
                    browser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    getWindow().setExitTransition(null);

                    startActivity(browser);
                } catch (Exception e) {

                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

        if (!webpage.equals("")) {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                s = s.replace("...", "");

                if (Patterns.WEB_URL.matcher(s).find() && (s.startsWith("t.co/") || s.contains("twitter.com/"))) { // we know the link is cut off
                    String replace = otherLinks[otherLinks.length - 1];
                    if (replace.replace(" ", "").equals("")) {
                        replace = webpage;
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

    public NetworkedCacheableImageView profilePic;
    public NetworkedCacheableImageView[] retweeters;
    public LinearLayout viewRetweeters;

    public void setUIElements(final View layout) {
        TextView nametv;
        TextView screennametv;
        TextView tweettv;
        ImageButton quote = null;
        final TextView retweetertv;
        final LinearLayout favoriteButton;
        final LinearLayout retweetButton;
        final TextView favoriteCount;
        final TextView retweetCount;
        final ImageButton replyButton;
        retweeters = new NetworkedCacheableImageView[3];

        nametv = (TextView) layout.findViewById(R.id.name);
        screennametv = (TextView) layout.findViewById(R.id.screen_name);
        tweettv = (TextView) layout.findViewById(R.id.tweet);
        retweetertv = (TextView) layout.findViewById(R.id.retweeter);
        profilePic = (NetworkedCacheableImageView) layout.findViewById(R.id.profile_pic_contact);
        favoriteButton = (LinearLayout) layout.findViewById(R.id.favorite);
        quote = (ImageButton) layout.findViewById(R.id.quote_button);
        retweetButton = (LinearLayout) layout.findViewById(R.id.retweet);
        favoriteCount = (TextView) layout.findViewById(R.id.fav_count);
        retweetCount = (TextView) layout.findViewById(R.id.retweet_count);
        timetv = (TextView) layout.findViewById(R.id.time);
        viewRetweeters = (LinearLayout) layout.findViewById(R.id.view_retweeters);
        replyButton = (ImageButton) layout.findViewById(R.id.send_button);

        View sendLayout = findViewById(R.id.send_layout);
        Drawable d = sendLayout.getBackground();
        d.setColorFilter(settings.themeColors.accentColor, PorterDuff.Mode.MULTIPLY);
        sendLayout.setBackground(d);

        retweeters[0] = (NetworkedCacheableImageView) layout.findViewById(R.id.retweeter_1);
        retweeters[1] = (NetworkedCacheableImageView) layout.findViewById(R.id.retweeter_2);
        retweeters[2] = (NetworkedCacheableImageView) layout.findViewById(R.id.retweeter_3);

        for (int i = 0; i < 3; i++) {
            retweeters[i].setClipToOutline(true);
        }

        retweetersPopup = new RetweetersPopupLayout(context);
        if (getResources().getBoolean(R.bool.isTablet)) {
            retweetersPopup.setWidthByPercent(.4f);
        } else {
            retweetersPopup.setWidthByPercent(.6f);
        }
        retweetersPopup.setHeightByPercent(.4f);

        viewRetweeters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    if (retweetersPopup != null) {
                        retweetersPopup.setOnTopOfView(viewRetweeters);
                        retweetersPopup.show();
                        showDim();
                    }
                }
            }
        });

        if (quote != null) {
            quote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String text = tweet;

                    if (!settings.preferRT) {
                        text = "\"@" + screenName + ": " + text + "\" ";
                    } else {
                        text = " RT @" + screenName + ": " + text;
                    }

                    Intent intent = new Intent(context, ComposeActivity.class);
                    intent.putExtra("user", text);
                    intent.putExtra("id", tweetId);

                    getWindow().setExitTransition(null);

                    startActivity(intent);
                }
            });
        }

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

                    getWindow().setExitTransition(null);

                    context.startActivity(viewProfile);
                }
            }
        };

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;

        if (picture) { // if there is a picture already loaded

            profilePic.loadImage(webpage, false, null);
            profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!hidePopups()) {
                        getWindow().setExitTransition(null);
                        context.startActivity(new Intent(context, PhotoViewerDialog.class).putExtra("url", webpage));
                    }
                }
            });

            View proPicContainer = findViewById(R.id.profile_pic_contact);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) proPicContainer.getLayoutParams();
            params.height = (int) (height * .5);
            proPicContainer.setLayoutParams(params);

            findViewById(R.id.person_info).setOnClickListener(viewPro);
            nametv.setOnClickListener(viewPro);
            screennametv.setOnClickListener(viewPro);

        } else {
            profilePic.loadImage(proPic, false, null);
            profilePic.setOnClickListener(viewPro);

            View proPicContainer = findViewById(R.id.profile_pic_contact);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) proPicContainer.getLayoutParams();
            params.height = (int) (height * .4);
            proPicContainer.setLayoutParams(params);

            findViewById(R.id.person_info).setOnClickListener(viewPro);
            nametv.setOnClickListener(viewPro);
            screennametv.setOnClickListener(viewPro);
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
            timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(time);
        } else {
            timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(time) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(time);
        }

        timetv.setText(timeDisplay);
        timetv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    String data = "twitter.com/" + screenName + "/status/" + tweetId;
                    Uri weburi = Uri.parse("http://" + data);
                    Intent launchBrowser = new Intent(Intent.ACTION_VIEW, weburi);
                    launchBrowser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getWindow().setExitTransition(null);
                    context.startActivity(launchBrowser);
                }
            }
        });

        timetv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (status != null) {
                    // we allow them to mute the client
                    final String client = android.text.Html.fromHtml(status.getSource()).toString();
                    new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.mute_client) + "?")
                            .setMessage(client)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
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

        if (retweeter.length() > 0 ) {
            retweetertv.setText(getResources().getString(R.string.retweeter) + retweeter);
            retweetertv.setVisibility(View.VISIBLE);
            isRetweet = true;
        }

        final TextView favoriteText = (TextView) findViewById(R.id.favorite_text);
        final TextView retweetText = (TextView) findViewById(R.id.retweet_text);

        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    favoriteStatus(favoriteCount, favoriteText, tweetId);
                }
            }
        });

        retweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    retweetStatus(retweetCount, tweetId, retweetText);
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
                                new RemoveRetweet(tweetId, retweetText).execute();
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

        getInfo(favoriteText, favoriteCount, retweetCount, tweetId, retweetText);

        String text = tweet;
        String extraNames = "";

        if (text.contains("@")) {
            for (String s : users) {
                if (!s.equals(settings.myScreenName) && !extraNames.contains(s)  && !s.equals(screenName)) {
                    extraNames += "@" + s + " ";
                }
            }
        }

        if (retweeter != null && !retweeter.equals("") && !retweeter.equals(settings.myScreenName) && !extraNames.contains(retweeter)) {
            extraNames += "@" + retweeter + " ";
        }

        String sendString;
        if (!screenName.equals(settings.myScreenName)) {
            sendString = "@" + screenName + " " + extraNames;
        } else {
            sendString = extraNames;
        }

        if (settings.autoInsertHashtags && hashtags != null) {
            for (String s : hashtags) {
                if (!s.equals("")) {
                    sendString += "#" + s + " ";
                }
            }
        }

        final String fsendString = sendString;
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!hidePopups()) {
                    Intent compose = new Intent(context, ComposeActivity.class);
                    compose.putExtra("user", fsendString.substring(0, fsendString.length() - 1)); // for some reason it puts a extra space here
                    compose.putExtra("id", tweetId);
                    compose.putExtra("reply_to_text", "@" + screenName + ": " + tweet);

                    getWindow().setExitTransition(null);

                    startActivity(compose);
                }
            }
        };
        replyButton.setOnClickListener(clickListener);


        // last bool is whether it should open in the external browser or not
        TextUtils.linkifyText(context, retweetertv, null, true, "", true);
        TextUtils.linkifyText(context, tweettv, null, true, "", true);
    }

    private boolean isFavorited = false;
    private boolean isRetweet = false;

    public void getFavoriteCount(final TextView favs, final TextView favText, final long tweetId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    Status status = twitter.showStatus(tweetId);
                    if (status.isRetweet()) {
                        Status retweeted = status.getRetweetedStatus();
                        status = retweeted;
                    }

                    final Status fStatus = status;
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            favs.setText(" " + fStatus.getFavoriteCount());

                            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
                            int textColor = a.getResourceId(0, 0);
                            a.recycle();

                            if (fStatus.isFavorited()) {
                                favText.setTextColor(settings.themeColors.accentColor);
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

        public RemoveRetweet(long tweetId, TextView retweetText) {
            this.tweetId = tweetId;
            this.retweetText = retweetText;
        }

        protected void onPreExecute() {
            Toast.makeText(context, getResources().getString(R.string.removing_retweet), Toast.LENGTH_SHORT).show();
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);
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

            retweetText.setTextColor(context.getResources().getColor(textColor));

            try {
                if (deleted) {
                    Toast.makeText(context, getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // user has gone away from the window
            }
        }
    }

    public void hideExtraContent() {
        final LinearLayout extra = (LinearLayout) findViewById(R.id.extra_content);
        final View back = findViewById(R.id.background);
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
                    back.setVisibility(View.INVISIBLE);
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
                back.startAnimation(anim);
            }
        }
    }

    public boolean isRunning = true;
    public ArrayList<Status> replies;
    public TimelineArrayAdapter adapter;

    public void getConversation() {
        Thread getConvo = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return;
                }

                Twitter twitter = Utils.getTwitter(context, settings);
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

    public Query query;

    public void getDiscussion() {

        Thread getReplies = new Thread(new Runnable() {
            @Override
            public void run() {

                if (!isRunning) {
                    return;
                }

                ArrayList<twitter4j.Status> all = null;
                Twitter twitter = Utils.getTwitter(context, settings);
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
                                        }
                                    } catch (Exception e) {
                                        // none and it got the null object
                                        e.printStackTrace();
                                        disableConvoButton();
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
                        }
                    });
                }
            }
        });

        getReplies.setPriority(8);
        getReplies.start();

    }

    public ImageButton viewReplyButton;

    public void disableConvoButton() {
        if (viewReplyButton != null) {
            viewReplyButton.setEnabled(false);
            viewReplyButton.setAlpha(.5f);
        }
    }

    public void getRetweeters() {
        Thread getRetweeters = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    long id = tweetId;
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

                            for (int i = 0; i < (getResources().getBoolean(R.bool.isTablet) ? 3 : 2); i++) {
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

    private Status status = null;

    public void getInfo(final TextView favoriteText, final TextView favCount, final TextView retweetCount, final long tweetId, final TextView retweetText) {

        Thread getInfo = new Thread(new Runnable() {
            @Override
            public void run() {
                String location = "";
                String via = "";
                long realTime = 0;
                boolean retweetedByMe = false;
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);

                    TwitterMultipleImageHelper helper = new TwitterMultipleImageHelper();
                    status = twitter.showStatus(tweetId);

                    ArrayList<String> i = new ArrayList<String>();

                    if (picture) {
                        i = helper.getImageURLs(status, twitter);
                    }

                    final ArrayList<String> images = i;

                    GeoLocation loc = status.getGeoLocation();
                    try {
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                        if (addresses.size() > 0) {
                            Address address = addresses.get(0);
                            location += address.getLocality() + ", " + address.getCountryName();
                        } else {
                            location = "";
                        }
                    } catch (Exception x) {
                        location = "";
                    }

                    via = android.text.Html.fromHtml(status.getSource()).toString();

                    final String sfavCount;
                    if (status.isRetweet()) {
                        twitter4j.Status status2 = status.getRetweetedStatus();
                        via = android.text.Html.fromHtml(status2.getSource()).toString();
                        realTime = status2.getCreatedAt().getTime();
                        sfavCount = status2.getFavoriteCount() + "";
                    } else {
                        realTime = status.getCreatedAt().getTime();
                        sfavCount = status.getFavoriteCount() + "";
                    }

                    retweetedByMe = status.isRetweetedByMe();
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

                    getConversation();

                    final String timeDisplay;

                    if (!settings.militaryTime) {
                        timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(realTime) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(realTime);
                    } else {
                        timeDisplay = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(realTime) + " " + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(realTime);
                    }
                    final String fVia = " " + getResources().getString(R.string.via) + " " + via;
                    final String fLoc = location.equals("") ? "" : "\n" + location;

                    final boolean fRet = retweetedByMe;
                    final long fTime = realTime;
                    final Status fStatus = status;
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.textColor});
                            int textColor = a.getResourceId(0, 0);
                            a.recycle();

                            retweetCount.setText(" " + retCount);

                            if (fRet) {
                                retweetText.setTextColor(settings.themeColors.accentColor);
                            } else {
                                retweetText.setTextColor(context.getResources().getColor(textColor));
                            }

                            timetv.setText(timeDisplay + fVia);
                            timetv.append(fLoc);

                            favCount.setText(" " + sfavCount);

                            if (fStatus.isFavorited()) {
                                favoriteText.setTextColor(settings.themeColors.accentColor);
                                isFavorited = true;
                            } else {
                                favoriteText.setTextColor(context.getResources().getColor(textColor));
                                isFavorited = false;
                            }

                            for (String s : images) {
                                Log.v("talon_image", s);
                            }
                            if (images.size() > 1) {
                                Log.v("talon_images", "size: " + images.size());
                                profilePic.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent viewPics = new Intent(context, ViewPictures.class);
                                        viewPics.putExtra("images", images);
                                        getWindow().setExitTransition(null);
                                        startActivity(viewPics);
                                    }
                                });
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

    public void getRetweetCount(final TextView retweetCount, final long tweetId, final TextView retweetText) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean retweetedByMe;
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    twitter4j.Status status = twitter.showStatus(tweetId);

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
                                retweetText.setTextColor(settings.themeColors.accentColor);
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

    public void favoriteStatus(final TextView favs, final TextView favoriteText, final long tweetId) {
        if (!isFavorited) {
            Toast.makeText(context, getResources().getString(R.string.favoriting_status), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, getResources().getString(R.string.removing_favorite), Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    if (isFavorited) {
                        twitter.destroyFavorite(tweetId);
                    } else {
                        twitter.createFavorite(tweetId);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, getResources().getString(R.string.success), Toast.LENGTH_SHORT).show();
                                getFavoriteCount(favs, favoriteText, tweetId);
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

    public void retweetStatus(final TextView retweetCount, final long tweetId, final TextView retweetText) {
        Toast.makeText(context, getResources().getString(R.string.retweeting_status), Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, settings);
                    twitter.retweetStatus(tweetId);

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, getResources().getString(R.string.retweet_success), Toast.LENGTH_SHORT).show();
                                getRetweetCount(retweetCount, tweetId, retweetText);
                            } catch (Exception e) {

                            }
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public void removeKeyboard(EditText reply) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);
    }

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
