package com.klinker.android.twitter.ui.fragments.lists;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.CursorListLoader;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.data.sq_lite.ListDataSource;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;
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

public class List2Fragment extends Fragment implements OnRefreshListener {

    private static Twitter twitter;

    public AsyncListView listView;
    private TimeLineCursorAdapter cursorAdapter;

    private SharedPreferences sharedPrefs;

    private PullToRefreshLayout mPullToRefreshLayout;
    public DefaultHeaderTransformer transformer;
    private LinearLayout spinner;

    static Activity context;

    private ActionBar actionBar;
    private int mActionBarSize;

    private boolean landscape;
    public boolean newTweets = false;

    private String jumpToTop;
    private String fromTop;
    private String allRead;

    private View.OnClickListener toTopListener;
    private View.OnClickListener over50Refresh;

    public View view;

    public BroadcastReceiver jumpTopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toTop();
        }
    };

    public List2Fragment() {
        this.listId = 0;
    }

    public List2Fragment(int listId) {
        this.listId = listId;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = context.getActionBar();
    }

    @Override
    public void onDestroy() {
        try {
            cursorAdapter.getCursor().close();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Log.v("talon_lists", "loaded list page");

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        currentAccount = sharedPrefs.getInt("current_account", 1);

        jumpToTop = getResources().getString(R.string.jump_to_top);
        fromTop = getResources().getString(R.string.from_top);
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

        layout = inflater.inflate(R.layout.list_fragment, null);

        loadAll = (LinearLayout) layout.findViewById(R.id.load_tweets);
        loadButton = (Button) layout.findViewById(R.id.load_tweets_button);

        listView = (AsyncListView) layout.findViewById(R.id.listView);
        listView.setVisibility(View.VISIBLE);

        spinner = (LinearLayout) layout.findViewById(R.id.spinner);
        spinner.setVisibility(View.VISIBLE);

        getCursorAdapter(true);

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

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                if (i == SCROLL_STATE_IDLE) {
                    MainActivity.sendHandler.removeCallbacks(MainActivity.hideSend);
                    MainActivity.sendHandler.postDelayed(MainActivity.showSend, 600);
                } else {
                    MainActivity.sendHandler.removeCallbacks(MainActivity.showSend);
                    MainActivity.sendHandler.postDelayed(MainActivity.hideSend, 300);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                if (over50Unread > 0 && firstVisibleItem == 0) {
                    showToastBar(over50Unread + " " + (over50Unread == 1 ? getResources().getString(R.string.new_tweet) : getResources().getString(R.string.new_tweets)),
                            getResources().getString(R.string.view),
                            400,
                            true,
                            over50Refresh);
                }

                if (DrawerActivity.settings.uiExtras) {
                    // show and hide the action bar
                    if (firstVisibleItem != 0) {
                        if (MainActivity.canSwitch) {
                            // used to show and hide the action bar
                            if (firstVisibleItem < 3) {

                            } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                if (!landscape && !isTablet) {
                                    actionBar.hide();
                                }
                                if (!isToastShowing && DrawerActivity.settings.useToast) {
                                    showToastBar(firstVisibleItem + " " + fromTop, jumpToTop, 400, false, toTopListener);
                                }
                            } else if (firstVisibleItem > mLastFirstVisibleItem) {
                                if (!landscape && !isTablet) {
                                    actionBar.show();
                                }
                                if (isToastShowing && !infoBar && DrawerActivity.settings.useToast) {
                                    hideToastBar(400);
                                }
                            }

                            mLastFirstVisibleItem = firstVisibleItem;
                        }
                    } else {
                        if (!landscape && !isTablet) {
                            actionBar.show();
                        }
                        if (!infoBar && DrawerActivity.settings.useToast) {
                            hideToastBar(400);
                        }
                    }

                    if (isToastShowing && !infoBar && DrawerActivity.settings.useToast) {
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

        view = layout;

        setUpToastBar(layout);

        toTopListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toTop();
            }
        };

        over50Refresh = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                over50Unread = 0;
                getCursorAdapter(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideToastBar(400);
                    }
                }, 300);
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

            twitter = Utils.getTwitter(context, DrawerActivity.settings);

            User user = twitter.verifyCredentials();
            long[] lastId;
            lastId = ListDataSource.getInstance(context).getLastIds(listId);

            final List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

            boolean foundStatus = false;

            Paging paging = new Paging(1, 200);

            if (lastId[0] != 0) {
                paging.setSinceId(lastId[0]);
            }

            for (int i = 0; i < DrawerActivity.settings.maxTweetsRefresh; i++) {

                try {
                    if (!foundStatus) {
                        paging.setPage(i + 1);
                        List<Status> list = twitter.getUserListStatuses(listId, paging);

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

            only50 = false;
            manualRefresh = false;

            ListDataSource dataSource = ListDataSource.getInstance(context);
            for (twitter4j.Status status : statuses) {
                dataSource.createTweet(status, listId);
            }

            numberNew = statuses.size();

            return numberNew;

        } catch (TwitterException e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onRefreshStarted(final View view) {
        new AsyncTask<Void, Void, Boolean>() {

            private int numberNew;

            @Override
            protected void onPreExecute() {
                try {
                    transformer.setRefreshingText(getResources().getString(R.string.loading) + "...");
                    DrawerActivity.canSwitch = false;
                } catch (Exception e) {

                }

            }

            @Override
            protected Boolean doInBackground(Void... params) {

                numberNew = doRefresh();

                return numberNew > 0;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                try {
                    super.onPostExecute(result);

                    if (result) {
                        getCursorAdapter(false);

                        if (numberNew > 0) {
                            final CharSequence text;

                            // append a plus on the end if it is 50
                            if (numberNew != 50) {
                                text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_tweet) :  numberNew + " " + getResources().getString(R.string.new_tweets);
                            } else {
                                text = numberNew + "+ " + getResources().getString(R.string.new_tweets);
                            }

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
                    } else {
                        final CharSequence text = context.getResources().getString(R.string.no_new_tweets);
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

                        mPullToRefreshLayout.setRefreshComplete();
                    }

                    if (!only50) {
                        DrawerActivity.canSwitch = true;
                    }

                    newTweets = false;
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

    @Override
    public void onPause() {
        markReadForLoad();

        context.unregisterReceiver(jumpTopReceiver);

        super.onPause();
    }

    public int currentAccount;
    public int listId;


    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.TOP_TIMELINE");
        context.registerReceiver(jumpTopReceiver, filter);
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
    public LinearLayout loadAll;
    public Button loadButton;
    public View layout;

    public void getCursorAdapter(final boolean bSpinner) {

        markReadForLoad();

        if (bSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) { }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor = ListDataSource.getInstance(context).getCursor(listId);

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Cursor c = null;
                        try {
                            c = cursorAdapter.getCursor();
                        } catch (Exception e) {

                        }
                        cursorAdapter = new TimeLineCursorAdapter(context, cursor, false);

                        final int position = getPosition(cursor, sharedPrefs.getLong("current_list_" + listId + "_account_" + currentAccount, 0));

                        if (bSpinner) {
                            try {
                                spinner.setVisibility(View.GONE);
                                listView.setVisibility(View.VISIBLE);
                            } catch (Exception e) { }
                        }

                        Log.v("talon_lists", "getting list: " + cursorAdapter.getCount());

                        listView.setAdapter(cursorAdapter);
                        int size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                        listView.setSelectionFromTop(position + (MainActivity.isPopup || landscape || MainActivity.settings.jumpingWorkaround ? 1 : 2), size);
                        mPullToRefreshLayout.setRefreshComplete();

                        try {
                            c.close();
                        } catch (Exception e) {

                        }
                    }
                });
            }
        }).start();
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

    private boolean isToastShowing = false;
    private boolean infoBar = false;

    private View toastBar;
    private TextView toastDescription;
    private TextView toastButton;

    private void setUpToastBar(View view) {
        toastBar = view.findViewById(R.id.toastBar);
        toastDescription = (TextView) view.findViewById(R.id.toastDescription);
        toastButton = (TextView) view.findViewById(R.id.toastButton);

        if (DrawerActivity.settings.addonTheme) {
            LinearLayout toastBackground = (LinearLayout) view.findViewById(R.id.toast_background);
            toastBackground.setBackgroundColor(Color.parseColor("#DD" + DrawerActivity.settings.accentColor));
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

        try {
            Cursor cursor = cursorAdapter.getCursor();
            int current = listView.getFirstVisiblePosition();

            if (cursor.moveToPosition(cursor.getCount() - current)) {

                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_list_" + listId + "_account_" + currentAccount, id).commit();
            } else {
                if (cursor.moveToLast()) {
                    long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_list_" + listId + "_account_" + currentAccount, id).commit();
                }
            }
        } catch (Exception e) {
            // cursor adapter is null because the loader was reset for some reason
            e.printStackTrace();
        }
    }

}