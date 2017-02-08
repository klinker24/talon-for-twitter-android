package com.klinker.android.twitter_l.activities.main_fragments.other_fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.main_fragments.MainFragment;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.data.sq_lite.UserTweetsDataSource;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;

public class UserTweetsFragment extends MainFragment {

    public static final int USER_TWEETS_REFRESH_ID = 1575;

    public boolean newTweets = false;

    public UserTweetsFragment() {
        this.userId = 0;
    }

    public BroadcastReceiver resetLists = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(true);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        userId = getArguments().getLong("user_id", 0l);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.RESET_USER");
        filter.addAction("com.klinker.android.twitter.USER_REFRESHED_" + userId);
        context.registerReceiver(resetLists, filter);

        if (sharedPrefs.getBoolean("refresh_me_user_" + userId, false)) { // this will restart the loader to display the new tweets
            getCursorAdapter(true);
            sharedPrefs.edit().putBoolean("refresh_me_user_" + userId, false).apply();
        }
    }

    public boolean manualRefresh = false;

    public int doRefresh() {
        int numberNew = 0;

        try {

            twitter = Utils.getTwitter(context, DrawerActivity.settings);

            long[] lastId = UserTweetsDataSource.getInstance(context).getLastIds(userId);

            final List<Status> statuses = new ArrayList<Status>();

            boolean foundStatus = false;

            Paging paging = new Paging(1, 200);

            if (lastId[0] > 0) {
                paging.setSinceId(lastId[0]);
            }

            for (int i = 0; i < DrawerActivity.settings.maxTweetsRefresh; i++) {

                try {
                    if (!foundStatus) {
                        paging.setPage(i + 1);
                        List<Status> list = twitter.getUserTimeline(userId, paging);

                        statuses.addAll(list);
                    }
                } catch (Exception e) {
                    // the page doesn't exist
                    foundStatus = true;
                } catch (OutOfMemoryError o) {
                    // don't know why...
                }
            }

            manualRefresh = false;

            UserTweetsDataSource dataSource = UserTweetsDataSource.getInstance(context);
            numberNew = dataSource.insertTweets(statuses, userId);

            return numberNew;
        } catch (Exception e) {
            // Error in updating status
            Log.d("Twitter Update Error", e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void onRefreshStarted() {
        new AsyncTask<Void, Void, Boolean>() {

            private int numberNew;

            @Override
            protected void onPreExecute() {
                try {
                    DrawerActivity.canSwitch = false;
                } catch (Exception e) { }
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

                            text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_tweet) :  numberNew + " " + getResources().getString(R.string.new_tweets);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Looper.prepare();
                                    } catch (Exception e) {
                                        // just in case
                                    }
                                    overrideSnackbarSetting = true;
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

                        refreshLayout.setRefreshing(false);
                    }

                    DrawerActivity.canSwitch = true;

                    newTweets = false;
                } catch (Exception e) {
                    DrawerActivity.canSwitch = true;

                    try {
                        refreshLayout.setRefreshing(false);
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
        context.unregisterReceiver(resetLists);
        super.onPause();
    }

    public long userId;

    public void getCursorAdapter(final boolean bSpinner) {

        markReadForLoad();

        if (bSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) { }
        }

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor;
                try {
                    cursor = UserTweetsDataSource.getInstance(context).getCursor(userId);
                } catch (Exception e) {
                    UserTweetsDataSource.dataSource = null;
                    context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_USERS"));
                    return;
                }

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (!isAdded()) {
                            return;
                        }

                        Cursor c = null;
                        if (cursorAdapter != null) {
                            c = cursorAdapter.getCursor();
                            Log.v("talon_cursor", c.getCount() + " tweets in old list");
                        }

                        try {
                            Log.v("talon_user", "number of tweets in user timeline: " + cursor.getCount());
                        } catch (Exception e) {
                            e.printStackTrace();
                            // the cursor or database is closed, so we will null out the datasource and restart the get cursor method
                            UserTweetsDataSource.dataSource = null;
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_USERS"));
                            return;
                        }

                        stopCurrentVideos();
                        if (cursorAdapter != null) {
                            TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, UserTweetsFragment.this);
                            cursorAdapter.setQuotedTweets(UserTweetsFragment.this.cursorAdapter.getQuotedTweets());
                            UserTweetsFragment.this.cursorAdapter = cursorAdapter;
                        } else {
                            cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, UserTweetsFragment.this);
                        }

                        applyAdapter();

                        int position = getPosition(cursor, sharedPrefs.getLong("current_user_tweets_" + userId + "_account_" + currentAccount, 0));

                        if (position > 0  && !settings.topDown) {
                            int size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                            try {
                                listView.setSelectionFromTop(position + listView.getHeaderViewsCount() -
                                                //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                                (settings.jumpingWorkaround ? 1 : 0),
                                        size);
                            } catch (Exception e) {
                                // not attached
                            }
                            refreshLayout.setRefreshing(false);
                        }

                        try {
                            spinner.setVisibility(View.GONE);
                        } catch (Exception e) { }

                        try {
                            listView.setVisibility(View.VISIBLE);
                        } catch (Exception e) {

                        }

                        if (c != null) {
                            try {
                                c.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        refreshLayout.setRefreshing(false);
                    }
                });
            }
        }).start();
    }

    public int getPosition(Cursor cursor, long id) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                do {
                    if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) == id) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {

        }

        return pos;
    }

    public void markReadForLoad() {

        try {
            Cursor cursor = cursorAdapter.getCursor();
            int current = listView.getFirstVisiblePosition();

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_user_tweets_" + userId + "_account_" + currentAccount, id).apply();
            } else {
                if (cursor.moveToLast()) {
                    long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_user_tweets_" + userId + "_account_" + currentAccount, id).apply();
                }
            }
        } catch (Exception e) {
            // cursor adapter is null because the loader was reset for some reason
            e.printStackTrace();
        }
    }

}