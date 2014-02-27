package com.klinker.android.twitter.ui.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.CursorListLoader;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter.services.TalonPullNotificationService;
import com.klinker.android.twitter.services.TimelineRefreshService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.Utils;
import com.klinker.android.twitter.utils.api_helper.TweetMarkerHelper;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.DefaultHeaderTransformer;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class HomeFragment extends Fragment implements OnRefreshListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final int HOME_REFRESH_ID = 121;

    private static Twitter twitter;

    public static AsyncListView listView;
    private TimeLineCursorAdapter cursorAdapter;

    private SharedPreferences sharedPrefs;

    private PullToRefreshLayout mPullToRefreshLayout;
    public DefaultHeaderTransformer transformer;
    private LinearLayout spinner;

    private static int unread;

    static Activity context;

    private ActionBar actionBar;
    private int mActionBarSize;

    private boolean initial = true;
    private boolean shown = true;
    private boolean landscape;
    public boolean newTweets = false;

    private String jumpToTop;
    private String fromTop;
    private String toMentions;
    private String allRead;

    private View.OnClickListener toTopListener;
    private View.OnClickListener toMentionsListener;
    private View.OnClickListener liveStreamRefresh;
    private View.OnClickListener over50Refresh;

    public View view;

    public int liveUnread = 0;

    public BroadcastReceiver pullReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (listView.getFirstVisiblePosition() == 0) {
                // we want to automatically show the new one if the user is at the top of the list
                // so we set the current position to the id of the top tweet

                context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

                sharedPrefs.edit().putBoolean("refresh_me", false).commit();
                int currentAccount = sharedPrefs.getInt("current_account", 1);

                HomeDataSource dataSource = HomeDataSource.getInstance(context);
                sharedPrefs.edit().putLong("current_position_" + currentAccount, dataSource.getLastIds(currentAccount)[0]).commit();

                trueLive = true;

                try {
                    getLoaderManager().restartLoader(0, null, HomeFragment.this);
                } catch (Exception e) {
                    // fragment not attached to activity?
                }
            } else {
                liveUnread++;
                sharedPrefs.edit().putBoolean("refresh_me", false).commit();
                if (liveUnread != 0) {
                    try {
                        showToastBar(liveUnread + " " + (liveUnread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                                getResources().getString(R.string.view),
                                400,
                                !DrawerActivity.settings.useToast,
                                liveStreamRefresh);
                    } catch (Exception e) {
                        // fragment not attached to activity
                    }
                }

                newTweets = true;
            }
        }
    };

    public BroadcastReceiver jumpTopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toTop();
        }
    };

    public BroadcastReceiver markRead = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            int account = intent.getIntExtra("current_account", 0);
            markReadForLoad(account);
            if (DrawerActivity.settings.tweetmarker) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                                sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                                Utils.getTwitter(context, new AppSettings(context)));

                        long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0);

                        boolean success = helper.sendCurrentId("timeline", currentId);

                        if (success) {
                            // then want to write the new version into shared prefs
                            int currentVersion = sharedPrefs.getInt("last_version_account_" + currentAccount, 0);
                            sharedPrefs.edit().putInt("last_version_account_" + currentAccount, currentVersion + 1).commit();
                        }
                    }
                }).start();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = context.getActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.v("setting_fragments", "home fragment");

        if(DrawerActivity.settings.pushNotifications) {
            if (!TalonPullNotificationService.isRunning) {
                context.startService(new Intent(context, TalonPullNotificationService.class));
            }
        } else {
            context.sendBroadcast(new Intent("com.klinker.android.twitter.STOP_PUSH_SERVICE"));
        }

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        currentAccount = sharedPrefs.getInt("current_account", 1);

        sharedPrefs.edit().putBoolean("refresh_me", false).commit();

        jumpToTop = getResources().getString(R.string.jump_to_top);
        fromTop = getResources().getString(R.string.from_top);
        toMentions = getResources().getString(R.string.mentions);
        allRead = getResources().getString(R.string.all_read);

        try{
            final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                    new int[] { android.R.attr.actionBarSize });
            mActionBarSize = (int) styledAttributes.getDimension(0, 0);
            styledAttributes.recycle();
        } catch (Exception e) {
            // a default just in case i guess...
            mActionBarSize = toDP(48);
        }

        View layout = inflater.inflate(R.layout.main_fragments, null);

        listView = (AsyncListView) layout.findViewById(R.id.listView);
        listView.setVisibility(View.VISIBLE);

        spinner = (LinearLayout) layout.findViewById(R.id.spinner);
        spinner.setVisibility(View.VISIBLE);

        getLoaderManager().initLoader(0, null, this);

        // Now find the PullToRefreshLayout to setup
        mPullToRefreshLayout = (PullToRefreshLayout) layout.findViewById(R.id.ptr_layout);

        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(context)
                // set up the scroll distance
                .options(Options.create().scrollDistance(.3f).build())
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set the OnRefreshListener
                .listener(this)
                        // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);

        transformer = ((DefaultHeaderTransformer)mPullToRefreshLayout.getHeaderTransformer());

        if (DrawerActivity.settings.addonTheme) {
            transformer.setProgressBarColor(DrawerActivity.settings.accentInt);
        }

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        CursorListLoader loader = new CursorListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        listView.setItemManager(builder.build());
        listView.setOverScrollMode(ListView.OVER_SCROLL_NEVER);

        View viewHeader = context.getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);
        listView.setHeaderDividersEnabled(false);

        if (DrawerActivity.translucent) {
            if (Utils.hasNavBar(context)) {
                View footer = new View(context);
                footer.setOnClickListener(null);
                footer.setOnLongClickListener(null);
                ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
                footer.setLayoutParams(params);
                listView.addFooterView(footer);
                listView.setFooterDividersEnabled(false);
            }

            if (!MainActivity.isPopup) {
                View view = new View(context);
                view.setOnClickListener(null);
                view.setOnLongClickListener(null);
                ListView.LayoutParams params2 = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getStatusBarHeight(context));
                view.setLayoutParams(params2);
                listView.addHeaderView(view);
                listView.setHeaderDividersEnabled(false);
            }
        }

        final boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if (DrawerActivity.settings.useToast) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                int mLastFirstVisibleItem = 0;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (newTweets && firstVisibleItem == 0 && DrawerActivity.settings.liveStreaming) {
                        if (liveUnread > 0) {
                            showToastBar(liveUnread + " " + (liveUnread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                                    getResources().getString(R.string.view),
                                    400,
                                    false,
                                    liveStreamRefresh);
                        }
                    }

                    if (over50Unread > 0 && firstVisibleItem == 0) {
                        showToastBar(over50Unread + " " + (over50Unread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                                getResources().getString(R.string.view),
                                400,
                                true,
                                over50Refresh);
                    }

                    if (DrawerActivity.settings.uiExtras) {
                        if (firstVisibleItem != 0) {
                            if (MainActivity.canSwitch) {
                                // used to show and hide the action bar
                                if (firstVisibleItem < 3) {

                                } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                    if (!landscape && !isTablet) {
                                        actionBar.hide();
                                    }
                                    if (!isToastShowing) {
                                        showToastBar(firstVisibleItem + " " + fromTop, jumpToTop, 400, false, toTopListener);
                                    }
                                } else if (firstVisibleItem > mLastFirstVisibleItem) {
                                    if (!landscape && !isTablet) {
                                        actionBar.show();
                                    }
                                    if (isToastShowing && !infoBar) {
                                        hideToastBar(400);
                                    }
                                }

                                mLastFirstVisibleItem = firstVisibleItem;
                            }
                        } else {
                            if (!landscape && !isTablet) {
                                actionBar.show();
                            }
                            if (!infoBar && unread == 0 && liveUnread == 0) {
                                hideToastBar(400);
                            }
                        }

                        if (isToastShowing && !infoBar && firstVisibleItem != 0) {
                            updateToastText(firstVisibleItem + " " + fromTop, jumpToTop);
                        }

                        if (MainActivity.translucent && actionBar.isShowing()) {
                            showStatusBar();
                        } else if (MainActivity.translucent) {
                            hideStatusBar();
                        }
                    }

                }
            });
        } else {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                int mLastFirstVisibleItem = 0;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    if (newTweets && firstVisibleItem == 0 && (DrawerActivity.settings.liveStreaming)) {
                        if (liveUnread > 0) {
                            showToastBar(liveUnread + " " + (liveUnread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                                    getResources().getString(R.string.view),
                                    400,
                                    true,
                                    liveStreamRefresh);
                        }
                    }

                    if (over50Unread > 0 && firstVisibleItem == 0) {
                        showToastBar(over50Unread + " " + (over50Unread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                                getResources().getString(R.string.view),
                                400,
                                true,
                                over50Refresh);
                    }

                    if (DrawerActivity.settings.uiExtras) {
                        if (firstVisibleItem != 0) {
                            if (MainActivity.canSwitch) {
                                // used to show and hide the action bar
                                if (firstVisibleItem < 3) {

                                } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                    if (!landscape && !isTablet) {
                                        actionBar.hide();
                                    }

                                } else if (firstVisibleItem > mLastFirstVisibleItem) {
                                    if (!landscape && !isTablet) {
                                        actionBar.show();
                                    }
                                }

                                mLastFirstVisibleItem = firstVisibleItem;
                            }
                        } else {
                            if (!landscape && !isTablet) {
                                actionBar.show();
                            }
                        }

                        if (MainActivity.translucent && actionBar.isShowing()) {
                            showStatusBar();
                        } else if (MainActivity.translucent) {
                            hideStatusBar();
                        }
                    }

                }
            });
        }

        view = layout;

        setUpToastBar(layout);

        toTopListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toTop();
            }
        };

        toMentionsListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int page1Type = sharedPrefs.getInt("account_" + currentAccount + "_page_1", AppSettings.PAGE_TYPE_NONE);
                int page2Type = sharedPrefs.getInt("account_" + currentAccount + "_page_2", AppSettings.PAGE_TYPE_NONE);

                int extraPages = 0;
                if (page1Type != AppSettings.PAGE_TYPE_NONE) {
                    extraPages++;
                }

                if (page2Type != AppSettings.PAGE_TYPE_NONE) {
                    extraPages++;
                }

                MainActivity.mViewPager.setCurrentItem(1 + extraPages, true);
                hideToastBar(400);
            }
        };

        liveStreamRefresh = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newTweets = false;
                viewPressed = true;
                trueLive = true;
                manualRefresh = false;
                getLoaderManager().restartLoader(0, null, HomeFragment.this);
                listView.setSelectionFromTop(0, 0);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideToastBar(400);
                    }
                }, 300);

                context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
                context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));
            }
        };

        over50Refresh = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                over50Unread = 0;
                getLoaderManager().restartLoader(0, null, HomeFragment.this);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideToastBar(400);
                    }
                }, 300);

                context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
                context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));
            }
        };

        return layout;
    }

    public void toTop() {

        // used so the content observer doesn't change the shared pref we just put in
        trueLive = true;

        int pos = listView.getFirstVisiblePosition();
        if (pos < 200) {
            try {
                if (pos > 50) {
                    listView.setSelectionFromTop(0, 0);
                    hideToastBar(400);
                } else {
                    listView.smoothScrollToPosition(0);
                }
            } catch (Exception e) {
                listView.setSelectionFromTop(0, 0);
            }
        } else {
            listView.setSelectionFromTop(0,0);
            hideToastBar(400);
        }
    }

    public boolean only50 = false;
    public boolean manualRefresh = false;
    public int over50Unread = 0;

    public int doRefresh() {
        int numberNew = 0;

        final int currentAccount = sharedPrefs.getInt("current_account", 1);

        try {
            Cursor cursor = cursorAdapter.getCursor();
            if (cursor.moveToLast()) {
                long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();
            }
        } catch (Exception e) {

        }

        try {

            if (!sharedPrefs.getBoolean("refresh_me", false)) {
                HomeDataSource.getInstance(context).markAllRead(currentAccount);
            }
            context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

            twitter = Utils.getTwitter(context, DrawerActivity.settings);

            User user = twitter.verifyCredentials();
            long[] lastId = HomeDataSource.getInstance(context).getLastIds(currentAccount);

            final List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

            boolean foundStatus = false;

            Paging paging = new Paging(1, 200);

            try {
                if (lastId[0] != 0) {
                    paging.setSinceId(lastId[0]);
                } else if (cursorAdapter != null) {
                    Cursor cursor = cursorAdapter.getCursor();
                    if (cursor.moveToLast()) {
                        long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                        paging.setSinceId(id);
                    }
                } else {
                    paging.setSinceId(sharedPrefs.getLong("account_" + currentAccount + "_lastid", 0));
                }
            } catch (IllegalArgumentException e) {
                // they hit this before actually getting the shared pref set... It would happen if the list isn't loaded and they refresh on startup
                return 0;
            }


            long beforeDownload = Calendar.getInstance().getTimeInMillis();

            for (int i = 0; i < DrawerActivity.settings.maxTweetsRefresh; i++) {

                try {
                    if (!foundStatus) {
                        paging.setPage(i + 1);
                        List<Status> list = twitter.getHomeTimeline(paging);

                        if (list.size() > 185) {
                            foundStatus = false;
                        } else {
                            foundStatus = true;
                        }

                        statuses.addAll(list);
                    }
                } catch (Exception e) {
                    // the page doesn't exist
                    foundStatus = true;
                } catch (OutOfMemoryError o) {
                    // don't know why...
                }
            }

            long afterDownload = Calendar.getInstance().getTimeInMillis();
            Log.v("talon_inserting", "downloaded " + statuses.size() + " tweets in " + (afterDownload - beforeDownload));

            only50 = false;
            manualRefresh = false;

            if (statuses.size() > 0) {
                sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).commit();
            }

            HomeContentProvider.insertTweets(statuses, currentAccount, context, lastId);

            Log.v("talon_inserting", "inserted tweets in " + (Calendar.getInstance().getTimeInMillis() - afterDownload));

            numberNew = statuses.size();
            unread = numberNew;

            statuses.clear();

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            long now = new Date().getTime();
            long alarm = now + DrawerActivity.settings.timelineRefresh;

            PendingIntent pendingIntent = PendingIntent.getService(context, HOME_REFRESH_ID, new Intent(context, TimelineRefreshService.class), 0);

            if (DrawerActivity.settings.timelineRefresh != 0)
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, DrawerActivity.settings.timelineRefresh, pendingIntent);
            else
                am.cancel(pendingIntent);

            return numberNew;

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
        }

        return 0;
    }

    public boolean getTweet() {
        int currentAccount = DrawerActivity.settings.currentAccount;
        int lastVersion = sharedPrefs.getInt("last_version_account_" + currentAccount, 0);
        TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                Utils.getTwitter(context, new AppSettings(context)));

        long tweetmarkerStatus = helper.getLastStatus("timeline", lastVersion, sharedPrefs);

        Log.v("talon_tweetmarker", "tweetmarker status: " + tweetmarkerStatus);

        if (tweetmarkerStatus != 0) {
            sharedPrefs.edit().putLong("current_position_" + DrawerActivity.settings.currentAccount, tweetmarkerStatus).commit();
            Log.v("talon_tweetmarker", "updating with tweetmarker");
            trueLive = true;
            return true;
        } else {
            return false;
        }
    }

    public void fetchTweetMarker() {
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                if (!actionBar.isShowing()) {
                    showStatusBar();
                    actionBar.show();
                }
                transformer.setRefreshingText(getResources().getString(R.string.finding_tweetmarker) + "...");
                mPullToRefreshLayout.setRefreshing(true);
                MainActivity.canSwitch = false;
            }

            @Override
            protected Boolean doInBackground(Void... params) {

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {

                }

                return getTweet();
            }

            @Override
            protected void onPostExecute(Boolean result) {
                hideToastBar(400);
                if (!only50) {
                    MainActivity.canSwitch = true;
                }
                if (result) {
                    try {
                        getLoaderManager().restartLoader(0, null, HomeFragment.this);
                    } catch (IllegalStateException e) {
                        // fragment not attached?
                        try {
                            mPullToRefreshLayout.setRefreshComplete();
                        } catch (Exception x) {
                            // something went very wrong,but they closed the app i think
                        }
                    }
                } else {
                    mPullToRefreshLayout.setRefreshComplete();
                }

            }
        }.execute();
    }


    int numberNew;
    boolean tweetMarkerUpdate;

    private boolean isRefreshing = false;

    @Override
    public void onRefreshStarted(final View view) {
        if (isRefreshing) {
            return;
        } else {
            isRefreshing = true;
        }

        try {
            transformer.setRefreshingText(getResources().getString(R.string.loading) + "...");
            DrawerActivity.canSwitch = false;
        } catch (Exception e) {

        }

        Thread refresh = new Thread(new Runnable() {
            @Override
            public void run() {
                numberNew = doRefresh();

                tweetMarkerUpdate = false;

                if (DrawerActivity.settings.tweetmarker) {
                    tweetMarkerUpdate = getTweet();
                    Log.v("talon_tweetmarker", "tweet marker update" + tweetMarkerUpdate);
                }

                final boolean result = numberNew > 0 || tweetMarkerUpdate;

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (result) {
                                getLoaderManager().restartLoader(0, null, HomeFragment.this);

                                if (unread > 0) {
                                    final CharSequence text;

                                    // append a plus on the end if it is 50
                                    if (unread != 50) {
                                        text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_tweet) :  numberNew + " " + getResources().getString(R.string.new_tweets);
                                    } else {
                                        text = numberNew + "+ " + getResources().getString(R.string.new_tweets);
                                    }

                                    if (!tweetMarkerUpdate) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Looper.prepare();
                                                } catch (Exception e) {
                                                    // just in case
                                                }
                                                showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                                            }
                                        }, 500);
                                    }
                                }
                            } else {
                                final CharSequence text = context.getResources().getString(R.string.no_new_tweets);
                                if (!DrawerActivity.settings.tweetmarker) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Looper.prepare();
                                            } catch (Exception e) {
                                                // just in case
                                            }
                                            showToastBar(text + "", allRead, 400, true, toTopListener);
                                        }
                                    }, 500);
                                }

                                mPullToRefreshLayout.setRefreshComplete();
                            }

                            if (!only50) {
                                DrawerActivity.canSwitch = true;
                            }

                            newTweets = false;

                            new RefreshMentions().execute();
                        } catch (Exception e) {
                            DrawerActivity.canSwitch = true;

                            try {
                                mPullToRefreshLayout.setRefreshComplete();
                            } catch (Exception x) {
                                // not attached to the activity i guess, don't know how or why that would be though
                            }
                        }

                        isRefreshing = false;

                    context.sendBroadcast(new Intent("com.klinker.android.talon.UPDATE_WIDGET"));
                    }
                });
            }
        });

        refresh.setPriority(7);
        refresh.start();
    }

    class RefreshMentions extends AsyncTask<Void, Void, Boolean> {

        private boolean update = false;
        private int numberNew = 0;

        @Override
        protected void onPreExecute() {
            DrawerActivity.canSwitch = false;
        }

        protected Boolean doInBackground(Void... args) {

            try {
                twitter = Utils.getTwitter(context, DrawerActivity.settings);

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                twitter.verifyCredentials();
                MentionsDataSource mentions = new MentionsDataSource(context);
                mentions.open();
                long[] lastId = mentions.getLastIds(currentAccount);
                Paging paging;
                paging = new Paging(1, 200);
                if (lastId[0] != 0) {
                    paging.sinceId(lastId[0]);
                }

                List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

                if (statuses.size() != 0) {
                    update = true;
                    numberNew = statuses.size();
                } else {
                    update = false;
                    numberNew = 0;
                }

                for (twitter4j.Status status : statuses) {
                    try {
                        mentions.createTweet(status, currentAccount);
                    } catch (Exception e) {
                        break;
                    }
                }

                sharedPrefs.edit().putBoolean("refresh_me_mentions", true).commit();

                mentions.close();

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            } catch (OutOfMemoryError e) {
                // why do you do this?!?!
                update = false;
            }

            return update;
        }

        protected void onPostExecute(Boolean updated) {

            try {
                if (updated) {
                    context.sendBroadcast(new Intent("com.klinker.android.twitter.REFRESH_MENTIONS"));
                    sharedPrefs.edit().putBoolean("refresh_me_mentions", true).commit();
                    CharSequence text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_mention) :  numberNew + " " + getResources().getString(R.string.new_mentions);
                    showToastBar(text + "", toMentions, 400, true, toMentionsListener);
                } else {

                }
            } catch (Exception e) {
                // might happen when switching accounts from the notification for second accounts mentions
            }

            DrawerActivity.canSwitch = true;
        }

    }

    public boolean justStarted = false;
    public Handler waitOnRefresh = new Handler();
    public Runnable applyRefresh = new Runnable() {
        @Override
        public void run() {
            sharedPrefs.edit().putBoolean("should_refresh", true).commit();
        }
    };

    @Override
    public void onPause() {
        markReadForLoad();

        super.onPause();
    }

    int currentAccount;

    @Override
    public void onStop() {
        Log.v("talon_stopping", "stopping here");

        try {
            context.unregisterReceiver(pullReceiver);
        } catch (Exception e) { }
        try {
            context.unregisterReceiver(jumpTopReceiver);
        } catch (Exception e) { }
        try {
            context.unregisterReceiver(markRead);
        } catch (Exception e) { }

        context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

        if (DrawerActivity.settings.tweetmarker) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                            sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                            Utils.getTwitter(context, new AppSettings(context)));

                    long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0);

                    boolean success = helper.sendCurrentId("timeline", currentId);

                    if (success) {
                        // then want to write the new version into shared prefs
                        int currentVersion = sharedPrefs.getInt("last_version_account_" + currentAccount, 0);
                        sharedPrefs.edit().putInt("last_version_account_" + currentAccount, currentVersion + 1).commit();
                    }
                }
            }).start();
        }

        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();

        initial = true;

        justStarted = true;

        if (sharedPrefs.getBoolean("refresh_me", false)) { // this will restart the loader to display the new tweets
            getLoaderManager().restartLoader(0, null, HomeFragment.this);
            sharedPrefs.edit().putBoolean("refresh_me", false).commit();
        } else { // otherwise, if there are no new ones, it should start the refresh (this is what was causing the jumping before)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if((DrawerActivity.settings.refreshOnStart) &&
                            (listView.getFirstVisiblePosition() == 0) &&
                            !MainActivity.isPopup &&
                            sharedPrefs.getBoolean("should_refresh", true) &&
                            !DrawerActivity.settings.tweetmarker) {

                        mPullToRefreshLayout.setRefreshing(true);
                        onRefreshStarted(view);
                    }

                    waitOnRefresh.removeCallbacks(applyRefresh);
                    waitOnRefresh.postDelayed(applyRefresh, 30000);
                }
            }, 400);
        }

        if (DrawerActivity.settings.liveStreaming && DrawerActivity.settings.tweetmarker) {
            fetchTweetMarker();
        } else if (!DrawerActivity.settings.liveStreaming && DrawerActivity.settings.tweetmarker) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullToRefreshLayout.setRefreshing(true);
                    onRefreshStarted(view);
                }
            }, 400);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.NEW_TWEET");
        context.registerReceiver(pullReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.TOP_TIMELINE");
        context.registerReceiver(jumpTopReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.MARK_POSITION");
        context.registerReceiver(markRead, filter);

        context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));
    }


    public int toDP(int px) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
        } catch (Exception e) {
            return px;
        }
    }

    public void showStatusBar() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DrawerActivity.statusBar.setVisibility(View.VISIBLE);
            }
        }, 000);
    }

    public void hideStatusBar() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DrawerActivity.statusBar.setVisibility(View.GONE);
            }
        }, 000); // 200 would be better
    }

    public boolean trueLive = false;

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (!trueLive && !initial) {
            Log.v("talon_tweetmarker", "true live");
            markReadForLoad();
        }

        try {
            Looper.prepare();
        } catch (Exception e) {

        }

        String[] projection = HomeDataSource.allColumns;
        CursorLoader cursorLoader = new CursorLoader(
                context,
                HomeContentProvider.CONTENT_URI,
                projection,
                null,
                new String[] { currentAccount + "" },
                null );
        return cursorLoader;
    }

    public boolean viewPressed = false;
    public Cursor currCursor;

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        currCursor = cursor;

        if (cursor.getCount() == 0) {
            // restart loader i guess?
            getLoaderManager().restartLoader(0, null, HomeFragment.this);
            return;
        }

        cursorAdapter = new TimeLineCursorAdapter(context, cursor, false);

        initial = false;

        long id = sharedPrefs.getLong("current_position_" + currentAccount, 0);
        int numTweets;
        if (id == 0) {
            numTweets = 0;
        } else {
            numTweets = getPosition(cursor, id);

            // tweetmarker was sending me the id of the wrong one sometimes, minus one from what it showed on the web and what i was sending it
            // so this is to error trap that
            if (numTweets < DrawerActivity.settings.timelineSize + 10 && numTweets > DrawerActivity.settings.timelineSize - 10) {

                // go with id + 1 first because tweetmarker seems to go 1 id less than I need
                numTweets = getPosition(cursor, id + 1);

                if (numTweets < DrawerActivity.settings.timelineSize + 10 && numTweets > DrawerActivity.settings.timelineSize - 10) {
                    numTweets = getPosition(cursor, id + 2);

                    if (numTweets < DrawerActivity.settings.timelineSize + 10 && numTweets > DrawerActivity.settings.timelineSize - 10) {
                        numTweets = getPosition(cursor, id - 1);

                        if (numTweets < DrawerActivity.settings.timelineSize + 10 && numTweets > DrawerActivity.settings.timelineSize - 10) {
                            numTweets = 0;
                        }
                    }
                }
            }

            Log.v("talon_tweetmarker", "finishing loader, id = " + id + " for account " + currentAccount);

            switch (currentAccount) {
                case 1:
                    Log.v("talon_tweetmarker", "finishing loader, id = " + sharedPrefs.getLong("current_position_" + 2, 0) + " for account " + 2);
                    break;
                case 2:
                    Log.v("talon_tweetmarker", "finishing loader, id = " + sharedPrefs.getLong("current_position_" + 1, 0) + " for account " + 1);
                    break;
            }
        }

        final int tweets = numTweets;

        listView.setAdapter(cursorAdapter);
        if (spinner.getVisibility() == View.VISIBLE) {
            spinner.setVisibility(View.GONE);
        }

        if (viewPressed) {
            int size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
            listView.setSelectionFromTop(liveUnread + (MainActivity.isPopup || landscape || MainActivity.settings.jumpingWorkaround ? 1 : 2), size);
        } else if (tweets != 0) {
            unread = tweets;
            int size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
            listView.setSelectionFromTop(tweets + (MainActivity.isPopup || landscape || MainActivity.settings.jumpingWorkaround ? 1 : 2), size);
        } else {
            listView.setSelectionFromTop(0, 0);
        }

        liveUnread = 0;
        viewPressed = false;

        if (!only50) {
            mPullToRefreshLayout.setRefreshComplete();
        }

        try {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    newTweets = false;
                }
            }, 500);
        } catch (Exception e) {
            newTweets = false;
        }
    }

    public int getPosition(Cursor cursor, long id) {
        int pos = 0;

        if (cursor.moveToLast()) {
            do {
                if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) == id) {
                    break;
                } else {
                    pos++;
                }
            } while (cursor.moveToPrevious());
        }

        return pos;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // data is not available anymore, delete reference
        Log.v("talon_timeline", "had to restart the loader for some reason, it was reset");
        try {
            cursorAdapter.swapCursor(null);
        } catch (Exception e) {

        }
        getLoaderManager().restartLoader(0, null, HomeFragment.this);
    }

    private boolean isToastShowing = false;
    private boolean infoBar = false;

    private View toastBar;
    private TextView toastDescription;
    private TextView toastButton;

    private void setUpToastBar(View view) {
        toastBar = view.findViewById(R.id.toastBar);
        toastDescription = (TextView) view.findViewById(R.id.toastDescription);
        toastButton = (TextView) view.findViewById(R.id.toastButton);

        try {
            if (DrawerActivity.settings.addonTheme) {
                LinearLayout toastBackground = (LinearLayout) view.findViewById(R.id.toast_background);
                toastBackground.setBackgroundColor(Color.parseColor("#DD" + DrawerActivity.settings.accentColor));
            }
        } catch (Exception e) {

        }
    }

    public Handler handler = new Handler();
    public Runnable hideToast = new Runnable() {
        @Override
        public void run() {
            hideToastBar(mLength);
            infoBar = false;
        }
    };
    public long mLength;

    private void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        if (quit) {
            infoBar = true;
        } else {
            infoBar = false;
        }

        mLength = length;

        toastDescription.setText(description);
        toastButton.setText(buttonText);
        toastButton.setOnClickListener(listener);

        if(!isToastShowing) {
            handler.removeCallbacks(hideToast);
            isToastShowing = true;
            toastBar.setVisibility(View.VISIBLE);

            Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (quit) {
                        handler.postDelayed(hideToast, 3000);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            anim.setDuration(length);
            toastBar.startAnimation(anim);
        }
    }

    private void hideToastBar(long length) {
        mLength = length;

        if (!isToastShowing) {
            return;
        }

        isToastShowing = false;

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                toastBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(length);
        toastBar.startAnimation(anim);
    }

    public void updateToastText(String text, String button) {
        if(isToastShowing && !(text.equals("0 " + fromTop) || text.equals("1 " + fromTop) || text.equals("2 " + fromTop))) {
            toastDescription.setText(text);
            toastButton.setText(button);
        } else if (text.equals("0 " + fromTop) || text.equals("1 " + fromTop) || text.equals("2 " + fromTop)) {
            hideToastBar(400);
        }
    }

    public void markReadForLoad() {
        Log.v("talon_tweetmarker", "marking read for account " + currentAccount);

        try {
            Cursor cursor = currCursor;
            int current = listView.getFirstVisiblePosition();

            HomeDataSource.getInstance(context).markAllRead(currentAccount);

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                Log.v("talon_marking_read", cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT)));
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();
            } else {
                if (cursor.moveToLast()) {
                    long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();
                }
            }
        } catch (Exception e) {
            // cursor adapter is null because the loader was reset for some reason
            e.printStackTrace();
        }
    }
    public void markReadForLoad(int currentAccount) {

        Log.v("talon_tweetmarker", "marking read for account " + currentAccount + " from the intent");

        if (currentAccount == 0) {
            markReadForLoad();
            return;
        }

        try {
            Cursor cursor = currCursor;
            int current = listView.getFirstVisiblePosition();

            HomeDataSource.getInstance(context).markAllRead(currentAccount);

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                Log.v("talon_marking_read", cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT)));
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();
            } else {
                if (cursor.moveToLast()) {
                    long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).commit();
                }
            }
        } catch (Exception e) {
            // cursor adapter is null because the loader was reset for some reason
            e.printStackTrace();
        }
    }

}