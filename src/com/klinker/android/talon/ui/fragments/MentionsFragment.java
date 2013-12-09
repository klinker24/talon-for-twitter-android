package com.klinker.android.talon.ui.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.CursorListLoader;
import com.klinker.android.talon.adapters.TimeLineCursorAdapter;
import com.klinker.android.talon.services.MentionsRefreshService;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.ConnectionDetector;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.Date;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class MentionsFragment extends Fragment implements OnRefreshListener {

    public static final int MENTIONS_REFRESH_ID = 127;

    private static Twitter twitter;
    private ConnectionDetector cd;

    private static AsyncListView listView;
    private static CursorAdapter cursorAdapter;

    public AppSettings settings;
    private static SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;
    private PullToRefreshLayout mPullToRefreshLayout;

    private static MentionsDataSource dataSource;

    private static int unread = 0;

    private boolean landscape;

    static Activity context;

    private ActionBar actionBar;
    private int mActionBarSize;

    private String fromTop;
    private String jumpToTop;
    private String allRead;

    private View.OnClickListener toTopListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        actionBar = context.getActionBar();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(context);
        cd = new ConnectionDetector(context);

        fromTop = getResources().getString(R.string.from_top);
        jumpToTop = getResources().getString(R.string.jump_to_top);
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
        // Check if Internet present
        if (!cd.isConnectingToInternet()) {

        }

        sharedPrefs.edit().putInt("mentions_unread_" + sharedPrefs.getInt("current_account", 1), 0).commit();

        dataSource = new MentionsDataSource(context);
        dataSource.open();

        listView = (AsyncListView) layout.findViewById(R.id.listView);

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
                ListView.LayoutParams params2 = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getStatusBarHeight(context) - toDP(5));
                view.setLayoutParams(params2);
                listView.addHeaderView(view);
                listView.setHeaderDividersEnabled(false);
            }
        }

        new GetCursorAdapter().execute();

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                // marks as read
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

                if (settings.uiExtras) {
                    // show and hide the action bar
                    if (firstVisibleItem != 0) {
                        if (MainActivity.canSwitch) {
                            // used to show and hide the action bar
                            if (firstVisibleItem < 3) {

                            } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                if (!landscape) {
                                    actionBar.hide();
                                }
                                if (!isToastShowing) {
                                    showToastBar(firstVisibleItem + " " + fromTop, jumpToTop, 400, false, toTopListener);
                                }
                            } else if (firstVisibleItem > mLastFirstVisibleItem) {
                                if (!landscape) {
                                    actionBar.show();
                                }
                                if (isToastShowing && !infoBar) {
                                    hideToastBar(400);
                                }
                            }

                            mLastFirstVisibleItem = firstVisibleItem;
                        }
                    } else {
                        if (!landscape) {
                            actionBar.show();
                        }
                        if (!infoBar) {
                            hideToastBar(400);
                        }
                    }

                    if (isToastShowing && !infoBar) {
                        updateToastText(firstVisibleItem + " " + fromTop);
                    }

                    if (MainActivity.translucent && actionBar.isShowing()) {
                        showStatusBar();
                    } else if (MainActivity.translucent) {
                        hideStatusBar();
                    }
                }
            }
        });

        setUpToastBar(layout);

        toTopListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listView.smoothScrollToPosition(0);
            }
        };

        return layout;
    }

    @Override
    public void onRefreshStarted(View view) {
        new AsyncTask<Void, Void, Void>() {

            private boolean update;
            private int numberNew;

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
                        Paging paging2 = new Paging(1, 150);
                        List<twitter4j.Status> statuses2 = twitter.getMentionsTimeline(paging2);

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

                    for (twitter4j.Status status : statuses) {
                        try {
                            dataSource.createTweet(status, currentAccount);
                        } catch (Exception e) {
                            break;
                        }
                    }

                    numberNew = dataSource.getUnreadCount(currentAccount);
                    unread = numberNew;

                } catch (TwitterException e) {
                    // Error in updating status
                    Log.d("Twitter Update Error", e.getMessage());
                }

                if (settings.mentionsRefresh != 0) {
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    long now = new Date().getTime();
                    long alarm = now + settings.mentionsRefresh;

                    PendingIntent pendingIntent = PendingIntent.getService(context, MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);

                    am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.mentionsRefresh, pendingIntent);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                if (update) {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), false);
                    refreshCursor();
                    CharSequence text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_mention) :  numberNew + " " + getResources().getString(R.string.new_mentions);
                    showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                    int size = toDP(5) + mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                    listView.setSelectionFromTop(numberNew + 2, size);
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), false);
                    refreshCursor();

                    CharSequence text = getResources().getString(R.string.no_new_mentions);
                    showToastBar(text + "", allRead, 400, true, toTopListener);
                }

                mPullToRefreshLayout.setRefreshComplete();

                DrawerActivity.canSwitch = true;
            }
        }.execute();
    }

    class GetCursorAdapter extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... args) {

            cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), false);

            return null;
        }

        protected void onPostExecute(String file_url) {

            attachCursor();
        }

    }

    public static void swapCursors() {
        cursorAdapter.swapCursor(dataSource.getCursor(sharedPrefs.getInt("current_account", 1)));
        cursorAdapter.notifyDataSetChanged();
    }

    public static void refreshCursor() {
        listView.setAdapter(cursorAdapter);

        swapCursors();
    }

    @SuppressWarnings("deprecation")
    public void attachCursor() {
        listView.setAdapter(cursorAdapter);

        swapCursors();

        int currentAccount = sharedPrefs.getInt("current_account", 1);
        int newTweets = dataSource.getUnreadCount(currentAccount);

        if (newTweets > 0) {
            unread = newTweets;
            int size = toDP(5) + mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
            listView.setSelectionFromTop(newTweets, size);
        }
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
        }, 000);
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
    }

    private void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        toastDescription.setText(description);
        toastButton.setText(buttonText);
        toastButton.setOnClickListener(listener);

        toastBar.setVisibility(View.VISIBLE);

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isToastShowing = true;

                if (quit) {
                    infoBar = true;
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (quit) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hideToastBar(length);
                            infoBar = false;
                        }
                    }, 3000);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(length);
        toastBar.startAnimation(anim);
    }

    private void hideToastBar(long length) {
        if (!isToastShowing) {
            return;
        }

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isToastShowing = false;
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

    public void updateToastText(String text) {
        toastDescription.setText(text);
    }


}