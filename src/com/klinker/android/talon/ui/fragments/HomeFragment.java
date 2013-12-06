package com.klinker.android.talon.ui.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.CursorListLoader;
import com.klinker.android.talon.adapters.TimeLineCursorAdapter;
import com.klinker.android.talon.services.TimelineRefreshService;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.HomeContentProvider;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.HomeSQLiteHelper;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.ConnectionDetector;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class HomeFragment extends Fragment implements OnRefreshListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final int HOME_REFRESH_ID = 121;

    private static Twitter twitter;
    private ConnectionDetector cd;

    public static AsyncListView listView;
    private TimeLineCursorAdapter cursorAdapter;

    public AppSettings settings;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;
    private PullToRefreshLayout mPullToRefreshLayout;

    private HomeDataSource dataSource;

    private static int unread;

    static Activity context;

    private ActionBar actionBar;
    private int mActionBarSize;

    private boolean initial = true;
    private boolean shown = true;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = context.getActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(context);
        cd = new ConnectionDetector(context);

        sharedPrefs.edit().putBoolean("refresh_me", false).commit();

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
        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            //Crouton.makeText(context, "No internet connection", Style.ALERT);
        }

        dataSource = new HomeDataSource(context);
        dataSource.open();

        listView = (AsyncListView) layout.findViewById(R.id.listView);

        getLoaderManager().initLoader(0, null, this);

        // Now find the PullToRefreshLayout to setup
        mPullToRefreshLayout = (PullToRefreshLayout) layout.findViewById(R.id.ptr_layout);

        // Now setup the PullToRefreshLayout
        ActionBarPullToRefresh.from(context)
                // Mark All Children as pullable
                .allChildrenArePullable()
                        // Set the OnRefreshListener
                .listener(this)
                        // Finally commit the setup to our PullToRefreshLayout
                .setup(mPullToRefreshLayout);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        CursorListLoader loader = new CursorListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());
        listView.setOverScrollMode(ListView.OVER_SCROLL_NEVER);

        View viewHeader = context.getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);
        listView.setHeaderDividersEnabled(false);

        if (DrawerActivity.translucent) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);

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

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                // used to mark read
                // TODO: CHANGE THIS, it is completely unnessisary and doesn't work well for large amounts
                // to onDestroy or pause and check the current item,
                // making sure everything below it is marked as read.
                final int currentAccount = sharedPrefs.getInt("current_account", 1);
                if (firstVisibleItem < unread) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            dataSource.markRead(currentAccount, firstVisibleItem);

                            unread = dataSource.getUnreadCount(currentAccount);
                        }
                    }).start();
                }

                if (firstVisibleItem != 0) {
                    if (MainActivity.canSwitch) {
                        // used to show and hide the action bar
                        if (firstVisibleItem < 3) {

                        } else if (firstVisibleItem < mLastFirstVisibleItem) {
                            actionBar.hide();
                        } else if (firstVisibleItem > mLastFirstVisibleItem) {
                            actionBar.show();
                        }

                        mLastFirstVisibleItem = firstVisibleItem;
                    }
                } else {
                    actionBar.show();
                }

                if (MainActivity.translucent && actionBar.isShowing()) {
                    showStatusBar();
                } else if (MainActivity.translucent) {
                    hideStatusBar();
                }

            }

            private void hideActionBar() {
                if (shown) {
                    actionBar.hide();
                    shown = false;
                }
            }

            private void showActionBar() {
                if (!shown) {
                    actionBar.show();
                    shown = true;
                }
            }
        });

        if(settings.refreshOnStart && listView.getFirstVisiblePosition() == 0) {
            
            final View view = layout;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullToRefreshLayout.setRefreshing(true);
                    onRefreshStarted(view);
                }
            }, 400);

        }

        return layout;
    }

    @Override
    public void onRefreshStarted(final View view) {
        new AsyncTask<Void, Void, Void>() {

            private int numberNew;

            public List<twitter4j.Status> getList(int page, Twitter twitter) {
                try {
                    return twitter.getHomeTimeline(new Paging(page, 200));
                } catch (Exception e) {
                    return new ArrayList<twitter4j.Status>();
                }
            }

            @Override
            protected void onPreExecute() {
                DrawerActivity.canSwitch = false;
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    twitter = Utils.getTwitter(context);

                    int currentAccount = sharedPrefs.getInt("current_account", 1);

                    User user = twitter.verifyCredentials();
                    long lastId = sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0);
                    long secondToLastId = sharedPrefs.getLong("second_last_tweet_id_" + currentAccount, 0);

                    List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

                    boolean foundStatus = false;
                    int lastJ = 0;

                    for (int i = 0; i < settings.maxTweetsRefresh; i++) {
                        if (foundStatus) {
                            break;
                        } else {
                            statuses.addAll(getList(i + 1, twitter));
                        }

                        try {
                            for (int j = lastJ; j < statuses.size(); j++) {
                                long id = statuses.get(j).getId();
                                if (id == lastId || id == secondToLastId) {
                                    statuses = statuses.subList(0, j);
                                    foundStatus = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            foundStatus = true;
                        }

                        lastJ = statuses.size();
                    }

                    if (statuses.size() != 0) {
                        try {
                            sharedPrefs.edit().putLong("second_last_tweet_id_" + currentAccount, statuses.get(1).getId()).commit();
                        } catch (Exception e) {
                            sharedPrefs.edit().putLong("second_last_tweet_id_" + currentAccount, sharedPrefs.getLong("last_tweet_id_" + currentAccount, 0)).commit();
                        }
                        sharedPrefs.edit().putLong("last_tweet_id_" + currentAccount, statuses.get(0).getId()).commit();

                    }

                    for (twitter4j.Status status : statuses) {
                        try {
                            insertTweet(status, currentAccount);
                            //dataSource.createTweet(status, currentAccount);
                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }
                    }

                    numberNew = dataSource.getUnreadCount(currentAccount);
                    unread = numberNew;

                } catch (TwitterException e) {
                    // Error in updating status
                    Log.d("Twitter Update Error", e.getMessage());
                }

                if (settings.timelineRefresh != 0) { // user only wants manual
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    long now = new Date().getTime();
                    long alarm = now + settings.timelineRefresh;

                    PendingIntent pendingIntent = PendingIntent.getService(context, HOME_REFRESH_ID, new Intent(context, TimelineRefreshService.class), 0);

                    am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.timelineRefresh, pendingIntent);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                try {
                    super.onPostExecute(result);

                    getLoaderManager().restartLoader(0, null, HomeFragment.this);

                    if (unread > 0) {
                        cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), false);
                        CharSequence text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_tweet) :  numberNew + " " + getResources().getString(R.string.new_tweets);
                        //Crouton.makeText((Activity) context, text, Style.INFO).show();
                    } else {
                        cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), false);

                        CharSequence text = context.getResources().getString(R.string.no_new_tweets);
                        //Crouton.makeText((Activity) context, text, Style.INFO).show();
                    }

                    DrawerActivity.canSwitch = true;

                    mPullToRefreshLayout.setRefreshComplete();

                    new RefreshMentions().execute();
                } catch (Exception e) {
                    DrawerActivity.canSwitch = true;

                    try {
                        mPullToRefreshLayout.setRefreshComplete();
                    } catch (Exception x) {
                        // not attached to the activity i guess, don't know how or why that would be though
                    }
                }
            }
        }.execute();
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
                twitter = Utils.getTwitter(context);

                int currentAccount = sharedPrefs.getInt("current_account", 1);

                User user = twitter.verifyCredentials();
                long lastId = sharedPrefs.getLong("last_mention_id_" + currentAccount, 0);
                Paging paging;
                paging = new Paging(1, 50);

                List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

                boolean broken = false;

                // first try to get the top 50 tweets
                for (int i = 0; i < statuses.size(); i++) {
                    if (statuses.get(i).getId() == lastId) {
                        statuses = statuses.subList(0, i);
                        broken = true;
                        break;
                    }
                }

                // if that doesn't work, then go for the top 150
                if (!broken) {
                    Log.v("updating_timeline", "not broken");
                    Paging paging2 = new Paging(1, 150);
                    List<twitter4j.Status> statuses2 = twitter.getHomeTimeline(paging2);

                    for (int i = 0; i < statuses2.size(); i++) {
                        if (statuses2.get(i).getId() == lastId) {
                            statuses2 = statuses2.subList(0, i);
                            break;
                        }
                    }

                    statuses = statuses2;
                }

                if (statuses.size() != 0) {
                    sharedPrefs.edit().putLong("last_mention_id_" + currentAccount, statuses.get(0).getId()).commit();
                    update = true;
                    numberNew = statuses.size();
                } else {
                    update = false;
                    numberNew = 0;
                }

                MentionsDataSource dataSource = new MentionsDataSource(context);
                dataSource.open();

                for (twitter4j.Status status : statuses) {
                    try {
                        dataSource.createTweet(status, currentAccount);
                    } catch (Exception e) {
                        break;
                    }
                }

                dataSource.close();

            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            }

            return update;
        }

        protected void onPostExecute(Boolean updated) {

            if (updated) {
                CharSequence text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_mention) :  numberNew + " " + getResources().getString(R.string.new_mentions);

                MentionsFragment.refreshCursor();
            } else {

            }

            DrawerActivity.canSwitch = true;
        }

    }

    @Override
    public void onPause() {
        sharedPrefs.edit().putInt("timeline_unread", listView.getFirstVisiblePosition()).commit();

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.v("on_resumed", "resuming home fragment");

        if (sharedPrefs.getBoolean("refresh_me", false)) {
            getLoaderManager().restartLoader(0, null, HomeFragment.this);
        }

        sharedPrefs.edit().putBoolean("refresh_me", false).commit();
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


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = HomeDataSource.allColumns;
        CursorLoader cursorLoader = new CursorLoader(
                context,
                HomeContentProvider.CONTENT_URI,
                projection,
                null,
                new String[] {sharedPrefs.getInt("current_account", 1) + ""},
                null );
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        //if (initial) {
            cursorAdapter = new TimeLineCursorAdapter(context, cursor, false);
            //cursorAdapter.swapCursor(cursor);
            listView.setAdapter(cursorAdapter);
            initial = false;
        //} else {
            //cursorAdapter.swapCursor(cursor);
        //}

        int currentAccount = sharedPrefs.getInt("current_account", 1);
        int newTweets = dataSource.getUnreadCount(currentAccount);

        Toast.makeText(context, newTweets + "", Toast.LENGTH_SHORT).show();

        if (newTweets > 0) {
            unread = newTweets;
            int size = toDP(5) + mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
            listView.setSelectionFromTop(newTweets + 2, size);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // data is not available anymore, delete reference
        cursorAdapter.swapCursor(null);
    }

    public void insertTweet(Status status, int currentAccount) {
        ContentValues values = new ContentValues();
        String originalName = "";
        long id = status.getId();
        long time = status.getCreatedAt().getTime();

        if(status.isRetweet()) {
            originalName = status.getUser().getScreenName();
            status = status.getRetweetedStatus();
        }

        values.put(HomeSQLiteHelper.COLUMN_ACCOUNT, currentAccount);
        values.put(HomeSQLiteHelper.COLUMN_TEXT, status.getText());
        values.put(HomeSQLiteHelper.COLUMN_TWEET_ID, id);
        values.put(HomeSQLiteHelper.COLUMN_NAME, status.getUser().getName());
        values.put(HomeSQLiteHelper.COLUMN_PRO_PIC, status.getUser().getBiggerProfileImageURL());
        values.put(HomeSQLiteHelper.COLUMN_SCREEN_NAME, status.getUser().getScreenName());
        values.put(HomeSQLiteHelper.COLUMN_TIME, time);
        values.put(HomeSQLiteHelper.COLUMN_RETWEETER, originalName);
        values.put(HomeSQLiteHelper.COLUMN_UNREAD, 1);

        MediaEntity[] entities = status.getMediaEntities();

        if (entities.length > 0) {
            values.put(HomeSQLiteHelper.COLUMN_PIC_URL, entities[0].getMediaURL());
        }

        context.getContentResolver().insert(HomeContentProvider.CONTENT_URI, values);
    }

}