package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
import com.klinker.android.twitter_l.manipulations.ConversationPopupLayout;
import com.klinker.android.twitter_l.manipulations.MobilizedWebPopupLayout;
import com.klinker.android.twitter_l.manipulations.RetweetersPopupLayout;
import com.klinker.android.twitter_l.manipulations.WebPopupLayout;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.compose.ComposeActivity;
import com.klinker.android.twitter_l.ui.tweet_viewer.ViewPictures;
import com.klinker.android.twitter_l.utils.api_helper.TwitterMultipleImageHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.*;
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
    View webButton;
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

    public ExpansionViewHelper(Context context, long tweetId) {
        this.context = context;
        this.id = tweetId;

        // get the base view
        expansion = ((Activity)context).getLayoutInflater().inflate(R.layout.tweet_expansion, null, false);

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

        webButton = expansion.findViewById(R.id.web_button);
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
                    }
                    convoPopup.show();
                } else {
                    Toast.makeText(context, "Loading Tweet...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        composeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent compose = new Intent(context, ComposeActivity.class);
                compose.putExtra("user", composeText);
                compose.putExtra("id", id);
                compose.putExtra("reply_to_text", tweetText);
                context.startActivity(compose);
            }
        });

        webButton.setEnabled(false);
        webButton.setAlpha(.5f);
        webButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                    }
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
                    }
                    webPopup.show();
                }
            }
        });
    }

    String webLink = null;

    public void setWebLink(String[] otherLinks) {

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

        if (webLink != null) {
            webButton.setEnabled(true);
            webButton.setAlpha(1.0f);
        }
    }

    String tweetText = null;
    String composeText = null;
    public void setReplyDetails(String t, String replyText) {
        this.tweetText = t;
        this.composeText = replyText;
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
                    Twitter twitter =  Utils.getTwitter(context, AppSettings.getInstance(context));
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
                    Twitter twitter =  Utils.getTwitter(context, AppSettings.getInstance(context));
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
                    Twitter twitter =  Utils.getTwitter(context, AppSettings.getInstance(context));

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
                                retweetText.setTextColor(context.getResources().getColor(R.color.accent));
                            } else {
                                retweetText.setTextColor(context.getResources().getColor(textColor));
                            }

                            favCount.setText(" " + sfavCount);

                            if (fStatus.isFavorited()) {
                                favText.setTextColor(context.getResources().getColor(R.color.accent));
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
                    Twitter twitter =  Utils.getTwitter(context, AppSettings.getInstance(context));
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
                                retweetText.setTextColor(context.getResources().getColor(R.color.accent));
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
                    Twitter twitter =  Utils.getTwitter(context, AppSettings.getInstance(context));
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
                                favText.setTextColor(context.getResources().getColor(R.color.accent));
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

                Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
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

                            } else {
                                disableConvoButton();
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
                Twitter twitter = Utils.getTwitter(context, AppSettings.getInstance(context));
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
                    Twitter twitter =  Utils.getTwitter(context, AppSettings.getInstance(context));

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
}
