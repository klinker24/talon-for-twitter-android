package com.klinker.android.twitter_l.activities.main_fragments.home_fragments;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.StaleDataException;
import android.database.sqlite.SQLiteDiskIOException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.adapters.TimelinePagerAdapter;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.services.PreCacheService;
import com.klinker.android.twitter_l.services.background_refresh.TimelineRefreshService;
import com.klinker.android.twitter_l.services.background_refresh.WidgetRefreshService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.main_fragments.MainFragment;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TweetMarkerHelper;
import com.klinker.android.twitter_l.widget.WidgetProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.TwitterException;

public class HomeFragment extends MainFragment {

    public static final int HOME_REFRESH_ID = 121;
    public static final String RATE_LIMIT_URL = "https://plus.google.com/117432358268488452276/posts/J5yY3K3fyME";

    public int unread;

    public boolean initial = true;
    public boolean newTweets = false;

    @Override
    public void setHome() {
        isHome = true;
        setStrings();
    }

    public void resetTimeline(boolean spinner) {
        getCursorAdapter(spinner);
    }

    private View.OnClickListener toMentionsListener = new View.OnClickListener() {
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

            MainActivity.mViewPager.setCurrentItem(
                    ((TimelinePagerAdapter)MainActivity.mViewPager.getAdapter()).mentionIndex, true);
            hideToastBar(400);
        }
    };

    protected View.OnClickListener liveStreamRefresh = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            newTweets = false;
            viewPressed = true;
            trueLive = true;
            manualRefresh = false;
            resetTimeline(false);
            listView.setSelectionFromTop(0, 0);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    infoBar = false;
                }
            }, 500);

            context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));
        }
    };

    protected View.OnClickListener infoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(RATE_LIMIT_URL));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    };

    public int liveUnread = 0;
    public boolean loadToTop = false;

    public BroadcastReceiver pullReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (!isLauncher()) {
                if (listView.getFirstVisiblePosition() == 0) {
                    // we want to automatically show the new one if the user is at the top of the list
                    // so we set the current position to the id of the top tweet

                    context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

                    sharedPrefs.edit().putBoolean("refresh_me", false).apply();
                    final long id = sharedPrefs.getLong("account_" + currentAccount + "_lastid", 0l);
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).apply();

                    new TimeoutThread(new Runnable() {
                        @Override
                        public void run() {
                            // sleep so that everyting loads correctly
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {

                            }
                            try {
                                HomeDataSource.getInstance(context).markPosition(currentAccount, id);
                            } catch (Exception e) {

                            }
                            //HomeContentProvider.updateCurrent(currentAccount, context, id);

                            trueLive = true;
                            loadToTop = true;

                            resetTimeline(false);
                        }
                    }).start();

                } else {
                    liveUnread++;
                    sharedPrefs.edit().putBoolean("refresh_me", false).apply();

                    newTweets = true;
                }
            }
        }
    };

    public BroadcastReceiver tweetmarkerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // if it is live streaming, then we will not have to refresh the timeline
            // otherwise, we do have to refresh the timeline.
            refreshOnStart();
        }
    };

    public BroadcastReceiver markRead = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            markReadForLoad();
            if (settings.tweetmarker) {
                new TimeoutThread(() -> {

                    TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                            sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                            Utils.getTwitter(context, new AppSettings(context)),
                            sharedPrefs,
                            getActivity());

                    long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0l);
                    helper.sendCurrentId("timeline", currentId);

                }).start();
            }
        }
    };

    public BroadcastReceiver homeClosed = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Log.v("talon_home_frag", "home closed broadcast received on home fragment");
            if (!dontGetCursor) {
                resetTimeline(true);
            }
            dontGetCursor = false;
        }
    };

    public TimeLineCursorAdapter returnAdapter(Cursor c) {
        TimeLineCursorAdapter adapter = new TimeLineCursorAdapter(context, c, false, true, this);
        if (this.cursorAdapter != null)
            adapter.setQuotedTweets(this.cursorAdapter.getQuotedTweets());

        return adapter;
    }

    public boolean isLauncher() {
        return false;
    }

    @Override
    public void getCursorAdapter(boolean showSpinner) {

        TimeoutThread getCursor = new TimeoutThread(new Runnable() {
            @Override
            public void run() {

                if (!trueLive && !initial) {
                    Log.v("talon_tweetmarker", "true live");
                    markReadForLoad();
                }

                final Cursor cursor;
                try {
                    cursor = HomeDataSource.getInstance(context).getCursor(currentAccount);
                } catch (Exception e) {
                    Log.v("talon_home_frag", "caught getting the cursor on the home timeline, sending reset home");
                    HomeDataSource.dataSource = null;
                    context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
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
                        }

                        stopCurrentVideos();
                        cursorAdapter = returnAdapter(cursor);

                        try {
                            Log.v("talon_databases", "size of adapter cursor on home fragment: " + cursor.getCount());
                        } catch (Exception e) {
                            e.printStackTrace();
                            HomeDataSource.dataSource = null;
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
                            return;
                        }

                        initial = false;

                        long id = sharedPrefs.getLong("current_position_" + currentAccount, 0l);
                        boolean update = true;
                        int numTweets;
                        if (id == 0 || loadToTop) {
                            numTweets = 0;
                            loadToTop = false;
                        } else {
                            numTweets = getPosition(cursor);

                            // if it would set it to the end, then we will get the position by the id instead
                            if (numTweets > cursor.getCount() - 5) {
                                numTweets = getPosition(cursor, id);
                                if (numTweets == -1) {
                                    return;
                                }
                            }

                            sharedPrefs.edit().putBoolean("just_muted", false).apply();
                        }

                        final int tweets = numTweets;

                        if (spinner.getVisibility() == View.VISIBLE) {
                            spinner.setVisibility(View.GONE);
                        }

                        if (listView.getVisibility() != View.VISIBLE) {
                            listView.setVisibility(View.VISIBLE);
                        }

                        try {
                            applyAdapter();
                        } catch (Exception e) {
                            // happens when coming from the launcher sometimes because database has been closed
                            HomeDataSource.dataSource = null;
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
                            return;
                        }

                        if (viewPressed && !settings.topDown) {
                            int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
                            try {
                                listView.setSelectionFromTop(liveUnread + listView.getHeaderViewsCount() -
                                                //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                                (settings.jumpingWorkaround ? 1 : 0),
                                        size);
                            } catch (Exception e) {
                                // not attached
                            }
                        } else if (tweets != 0 ) {
                            unread = tweets;
                            int size = mActionBarSize + (DrawerActivity.translucent && !MainActivity.isPopup ? Utils.getStatusBarHeight(context) : 0);
                            try {
                                if (!settings.topDown) {
                                    listView.setSelectionFromTop(tweets + listView.getHeaderViewsCount() -
                                                    //(getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                                    (settings.jumpingWorkaround ? 1 : 0),
                                            size);
                                }
                            } catch (Exception e) {
                                // not attached
                            }
                        } else {
                            try {
                                listView.setSelectionFromTop(0, 0);
                            } catch (Exception e) {
                                // not attached
                            }
                        }

                        try {
                            c.close();
                        } catch (Exception e) {

                        }

                        liveUnread = 0;
                        viewPressed = false;

                        refreshLayout.setRefreshing(false);

                        isRefreshing = false;

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
                });
            }
        });

        getCursor.setPriority(8);
        getCursor.start();


    }

    public void toTop() {
        // used so the content observer doesn't change the shared pref we just put in
        trueLive = true;
        super.toTop();
    }

    public boolean manualRefresh = false;
    public boolean dontGetCursor = false;
    public boolean rateLimited = false;

    public int insertTweets(List<Status> statuses, long[] lastId) {
        return HomeDataSource.getInstance(context).insertTweets(statuses, currentAccount, lastId);
    }

    public int doRefresh() {
        TimelineRefreshService.scheduleRefresh(context);

        int numberNew = 0;

        if (TimelineRefreshService.isRunning || WidgetRefreshService.isRunning) {
            // quit if it is running in the background
            return 0;
        }

        try {
            Cursor cursor = cursorAdapter.getCursor();
            if (cursor.moveToLast()) {
                long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).apply();
                HomeDataSource.getInstance(context).markPosition(currentAccount, id);
                //HomeContentProvider.updateCurrent(currentAccount, context, id);
            }
        } catch (Exception e) {

        }

        boolean needClose = false;

        context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

        twitter = Utils.getTwitter(context, settings);

        final List<twitter4j.Status> statuses = new ArrayList<twitter4j.Status>();

        boolean foundStatus = false;

        Paging paging = new Paging(1, 200);

        long[] lastId = null;
        long id;
        try {
            lastId = HomeDataSource.getInstance(context).getLastIds(currentAccount);
            id = lastId[1];
        } catch (Exception e) {
            id = sharedPrefs.getLong("account_" + currentAccount + "_lastid", 1l);
        }
        Log.v("talon_inserting", "since_id=" + id);
        try {
            paging.setSinceId(id);
        } catch (Exception e) {
            // 0 for some reason, so dont set one and let the database sort which should show and which shouldn't
        }

        long beforeDownload = Calendar.getInstance().getTimeInMillis();

        for (int i = 0; i < settings.maxTweetsRefresh; i++) {

            try {
                if (!foundStatus) {
                    paging.setPage(i + 1);
                    List<Status> list = twitter.getHomeTimeline(paging);
                    statuses.addAll(list);

                    if (statuses.size() <= 1 || statuses.get(statuses.size() - 1).getId() == lastId[0]) {
                        Log.v("talon_inserting", "found status");
                        foundStatus = true;
                    } else {
                        Log.v("talon_inserting", "haven't found status");
                        foundStatus = false;
                    }
                }
            } catch (TwitterException e) {
                Log.v("talon_error", "code: " + e.getErrorCode());
                if (e.getErrorCode() == 88) {
                    // rate limit reached
                    rateLimited = true;
                    foundStatus = true;

                    return 0;
                }
            } catch (Exception e) {
                // the page doesn't exist
                e.printStackTrace();
                Log.v("talon_error", "error with refresh");
                foundStatus = true;
            } catch (OutOfMemoryError o) {
                // don't know why...
            }
        }

        long afterDownload = Calendar.getInstance().getTimeInMillis();
        Log.v("talon_inserting", "downloaded " + statuses.size() + " tweets in " + (afterDownload - beforeDownload));

        if (statuses.size() > 0) {
            statuses.remove(statuses.size() - 1);
        }

        HashSet<Status> hs = new HashSet<>();
        hs.addAll(statuses);
        statuses.clear();
        statuses.addAll(hs);

        Log.v("talon_inserting", "tweets after hashset: " + statuses.size());

        manualRefresh = false;

        if (needClose) {
            HomeDataSource.dataSource = null;
            Log.v("talon_home_frag", "sending the reset home broadcase in needclose section");
            dontGetCursor = true;
            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
        }

        if (lastId == null) {
            try {
                lastId = HomeDataSource.getInstance(context).getLastIds(currentAccount);
            } catch (Exception e) {
                // let the
                lastId = new long[] {0,0,0,0,0};
            }
        }

        try {
            numberNew = insertTweets(statuses, lastId);
        } catch (NullPointerException e) {
            return 0;
        }

        if (numberNew > statuses.size()) {
            numberNew = statuses.size();
        }

        if (numberNew > 0 && statuses.size() > 0) {
            sharedPrefs.edit().putLong("account_" + currentAccount + "_lastid", statuses.get(0).getId()).apply();
        }

        Log.v("talon_inserting", "inserted " + numberNew + " tweets in " + (Calendar.getInstance().getTimeInMillis() - afterDownload));

        //numberNew = statuses.size();
        unread = numberNew;

        statuses.clear();

        int unreadCount;
        try {
            unreadCount = HomeDataSource.getInstance(context).getUnreadCount(currentAccount);
        } catch (Exception e) {
            unreadCount = numberNew;
        }

        /*if (settings.preCacheImages) {
            try {
                getActivity().startService(new Intent(getActivity(), PreCacheService.class));
            } catch (Throwable t) {

            }
        }*/

        return unreadCount;
    }

    public boolean getTweet() {

        TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                Utils.getTwitter(context, new AppSettings(context)),
                sharedPrefs,
                getActivity());

        boolean updated = helper.getLastStatus("timeline", context);

        // update settings just in case it was invalidated
        settings = AppSettings.getInstance(getActivity());

        Log.v("talon_tweetmarker", "tweetmarker status: " + updated);

        if (updated) {
            //HomeContentProvider.updateCurrent(currentAccount, context, sharedPrefs.getLong("current_position_" + currentAccount, 0l));
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
                if (!isLauncher() && actionBar != null && !actionBar.isShowing()) {
                    showStatusBar();
                    actionBar.show();
                }

                try {
                    refreshLayout.setRefreshing(true);
                } catch (Exception e) {
                    // same thing
                }
                MainActivity.canSwitch = false;
                isRefreshing = true;
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

                MainActivity.canSwitch = true;

                if (result) {
                    resetTimeline(false);
                } else {
                    refreshLayout.setRefreshing(false);
                    isRefreshing = false;
                }

            }
        }.execute();
    }

    public String sNewTweet;
    public String sNewTweets;
    public String sNoNewTweets;
    public String sNewMention;
    public String sNewMentions;

    public void setStrings() {
        sNewTweet = getResources().getString(R.string.new_tweet);
        sNewTweets = getResources().getString(R.string.new_tweets);
        sNoNewTweets = getResources().getString(R.string.no_new_tweets);
        sNewMention = getResources().getString(R.string.new_mention);
        sNewMentions = getResources().getString(R.string.new_mentions);
    }

    public int numberNew;
    public boolean tweetMarkerUpdate;

    public boolean isRefreshing = false;

    @Override
    public void onRefreshStarted() {
        if (isRefreshing) {
            return;
        } else {
            isRefreshing = true;
        }

        DrawerActivity.canSwitch = false;

        TimeoutThread refresh = new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                if (!onStartRefresh) {
                    numberNew = doRefresh();
                } else {
                    onStartRefresh = false;
                }

                tweetMarkerUpdate = false;

                if (settings.tweetmarker && refreshTweetmarker) {
                    tweetMarkerUpdate = getTweet();
                }

                HomeFragment.starting = false;

                refreshTweetmarker = false;

                final boolean result = numberNew > 0 || tweetMarkerUpdate;

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setStrings();

                            if (result) {
                                Log.v("talon_home_frag", "getting cursor adapter in onrefreshstarted");
                                resetTimeline(false);

                                if (unread > 0) {
                                    final CharSequence text;

                                    numberNew = HomeDataSource.getInstance(context).getUnreadCount(currentAccount);

                                    text = numberNew == 1 ? numberNew + " " + sNewTweet : numberNew + " " + sNewTweets;

                                    if (!tweetMarkerUpdate || (!tweetMarkerUpdate && settings.tweetmarkerManualOnly)) {
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    Looper.prepare();
                                                } catch (Exception e) {
                                                    // just in case
                                                }
                                                isToastShowing = false;
                                                overrideSnackbarSetting = true;
                                                showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                                            }
                                        }, 500);
                                    }
                                }
                            } else if (rateLimited) {

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                                rateLimited = false;

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Looper.prepare();
                                            isToastShowing = false;
                                            overrideSnackbarSetting = true;
                                            showToastBar(getString(R.string.rate_limit_reached),
                                                    getString(R.string.info),
                                                    400,
                                                    true,
                                                    infoClickListener);
                                        } catch (Exception e) {
                                            // just in case
                                        }
                                    }
                                }, 500);

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                            } else {
                                final CharSequence text = sNoNewTweets;
                                if (!settings.tweetmarker) {
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                Looper.prepare();
                                            } catch (Exception e) {
                                                // just in case
                                            }
                                            isToastShowing = false;
                                            showToastBar(text + "", allRead, 400, true, toTopListener);
                                        }
                                    }, 500);
                                }

                                refreshLayout.setRefreshing(false);
                                isRefreshing = false;
                            }

                            DrawerActivity.canSwitch = true;

                            newTweets = false;

                            if (!isLauncher()) {
                                new RefreshMentions().execute();
                            }
                        } catch (Exception e) {
                            DrawerActivity.canSwitch = true;

                            try {
                                refreshLayout.setRefreshing(false);
                            } catch (Exception x) {
                                // not attached to the activity i guess, don't know how or why that would be though
                            }
                            isRefreshing = false;
                        }
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
                twitter = Utils.getTwitter(context, settings);

                MentionsDataSource mentions = MentionsDataSource.getInstance(context);
                try {
                    mentions.markAllRead(settings.currentAccount);
                } catch (Throwable e) {

                }
                long[] lastId = mentions.getLastIds(currentAccount);
                Paging paging;
                paging = new Paging(1, 200);
                try {
                    if (lastId[0] != 0) {
                        try {
                            paging.setSinceId(lastId[0]);
                        } catch (Exception e) {
                            return false;
                        }
                    }
                } catch (NullPointerException e) {
                    return false;
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

                sharedPrefs.edit().putBoolean("refresh_me_mentions", true).apply();

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
                    setStrings();
                    context.sendBroadcast(new Intent("com.klinker.android.twitter.REFRESH_MENTIONS"));
                    sharedPrefs.edit().putBoolean("refresh_me_mentions", true).apply();
                    final CharSequence text = numberNew == 1 ?  numberNew + " " + sNewMention :  numberNew + " " + sNewMentions;
                    isToastShowing = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showToastBar(text + "", toMentions, 400, true, toMentionsListener);
                        }
                    },1500);
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
            sharedPrefs.edit().putBoolean("should_refresh", true).apply();
        }
    };

    @Override
    public void onPause() {

        markReadForLoad();

        context.unregisterReceiver(pullReceiver);
        context.unregisterReceiver(markRead);
        context.unregisterReceiver(homeClosed);
        context.unregisterReceiver(tweetmarkerReceiver);

        super.onPause();
    }

    @Override
    public void onStop() {

        context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));

        if (settings.tweetmarker && !isLauncher()) {
            new TimeoutThread(new Runnable() {
                @Override
                public void run() {

                    TweetMarkerHelper helper = new TweetMarkerHelper(currentAccount,
                            sharedPrefs.getString("twitter_screen_name_" + currentAccount, ""),
                            Utils.getTwitter(context, new AppSettings(context)), sharedPrefs,
                            getActivity());

                    long currentId = sharedPrefs.getLong("current_position_" + currentAccount, 0);
                    helper.sendCurrentId("timeline", currentId);

                }
            }).start();
        }

        WidgetProvider.updateWidget(getActivity());
        //context.getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.NEW_TWEET");
        context.registerReceiver(pullReceiver, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.RESET_HOME");
        context.registerReceiver(homeClosed, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.MARK_POSITION");
        context.registerReceiver(markRead, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.TWEETMARKER");
        context.registerReceiver(tweetmarkerReceiver, filter);

        if (isLauncher()) {
            return;
        }

        if (sharedPrefs.getBoolean("refresh_me", false)) { // this will restart the loader to display the new tweets
            //getLoaderManager().restartLoader(0, null, HomeFragment.this);
            Log.v("talon_home_frag", "getting cursor adapter in on resume");
            resetTimeline(true);
            sharedPrefs.edit().putBoolean("refresh_me", false).apply();
        }
    }

    public boolean refreshTweetmarker = false;
    public boolean onStartRefresh = false;
    public static Handler refreshHandler;

    @Override
    public void onStart() {
        super.onStart();

        if (HomeFragment.refreshHandler == null) {
            HomeFragment.refreshHandler = new Handler();
        }

        if (MainActivity.caughtstarting) {
            MainActivity.caughtstarting = false;
            return;
        }

        initial = true;
        justStarted = true;

        if (sharedPrefs.getBoolean("refresh_me", false)) { // this will restart the loader to display the new tweets
            Log.v("talon_home_frag", "getting cursor adapter in on start");
            resetTimeline(false);
            sharedPrefs.edit().putBoolean("refresh_me", false).apply();
        } else if (!sharedPrefs.getBoolean("dont_refresh", false)) { // otherwise, if there are no new ones, it should start the refresh
            HomeFragment.refreshHandler.removeCallbacksAndMessages(null);
            HomeFragment.refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ((settings.refreshOnStart) &&
                            (listView.getFirstVisiblePosition() == 0) &&
                            !MainActivity.isPopup &&
                            sharedPrefs.getBoolean("should_refresh", true) &&
                            (!settings.tweetmarker || settings.tweetmarkerManualOnly)) {
                        if (actionBar != null && !actionBar.isShowing() && !isLauncher()) {
                            showStatusBar();
                            actionBar.show();
                        }

                        refreshOnStart();
                    }

                    waitOnRefresh.removeCallbacks(applyRefresh);
                    waitOnRefresh.postDelayed(applyRefresh, 30000);
                    sharedPrefs.edit().putBoolean("dont_refresh", false).apply();

                }
            }, 600);
        }


        if (settings.tweetmarker && !settings.tweetmarkerManualOnly) {
            HomeFragment.refreshHandler.removeCallbacksAndMessages(null);
            HomeFragment.refreshHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (actionBar != null && !actionBar.isShowing() && !isLauncher()) {
                        showStatusBar();
                        actionBar.show();
                    }

                    if (!sharedPrefs.getBoolean("from_activity", false)) {
                        refreshOnStart();
                    } else {
                        sharedPrefs.edit().putBoolean("from_activity", false).apply();
                    }
                }
            }, 600);
        }

        context.sendBroadcast(new Intent("com.klinker.android.twitter.CLEAR_PULL_UNREAD"));
    }

    public static boolean starting = false;
    private void refreshOnStart() {
        if (HomeFragment.starting) {
            return;
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    HomeFragment.starting = false;
                }
            }, 10000);
        }

        refreshLayout.setRefreshing(true);
        refreshTweetmarker = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.TIMELINE_REFRESHED");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.v("talon_startup", "here");
                numberNew = intent.getIntExtra("number_new", 0);
                unread = numberNew;
                onStartRefresh = true;
                onRefreshStarted();
                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                    // not registered
                }
            }
        }, filter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                TimelineRefreshService.refresh(context, true);
                PreCacheService.cache(context);
            }
        }).start();
    }

    public boolean trueLive = false;

    /*@Override
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
                new String[] {currentAccount + ""},
                null );

        return cursorLoader;
    }*/

    public boolean viewPressed = false;

    /*@Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
Log.v("talon_remake", "load finished, " + cursor.getCount() + " tweets");

                        currCursor = cursor;

                        Cursor c = null;
                        if (cursorAdapter != null) {
                            c = cursorAdapter.getCursor();
                        }

                        cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, true);

                        initial = false;

                        long id = sharedPrefs.getLong("current_position_" + currentAccount, 0);
                        boolean update = true;
                        int numTweets;
                        if (id == 0) {
                            numTweets = 0;
                        } else {
                            numTweets = getPosition(cursor, id);
                            if (numTweets == -1) {
                                return;
                            }

                            // tweetmarker was sending me the id of the wrong one sometimes, minus one from what it showed on the web and what i was sending it
                            // so this is to error trap that
                            if (numTweets < settings.timelineSize + 10 && numTweets > settings.timelineSize - 10) {

                                // go with id + 1 first because tweetmarker seems to go 1 id less than I need
                                numTweets = getPosition(cursor, id + 1);
                                if (numTweets == -1) {
                                    return;
                                }

                                if (numTweets < settings.timelineSize + 10 && numTweets > settings.timelineSize - 10) {
                                    numTweets = getPosition(cursor, id + 2);
                                    if (numTweets == -1) {
                                        return;
                                    }

                                    if (numTweets < settings.timelineSize + 10 && numTweets > settings.timelineSize - 10) {
                                        numTweets = getPosition(cursor, id - 1);
                                        if (numTweets == -1) {
                                            return;
                                        }

                                        if (numTweets < settings.timelineSize + 10 && numTweets > settings.timelineSize - 10) {
                                            numTweets = 0;
                                            update = sharedPrefs.getBoolean("just_muted", false);
                                        }
                                    }
                                }
                            }

                            sharedPrefs.edit().putBoolean("just_muted", false).apply();

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

                        if (spinner.getVisibility() == View.VISIBLE) {
                            spinner.setVisibility(View.GONE);
                        }

                        if (listView.getVisibility() != View.VISIBLE) {
                            update = true; // we want to do this to ensure there just isn't a blank list shown...
                            listView.setVisibility(View.VISIBLE);
                        }

                        if (update) {
                            applyAdapter();

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
                        }

                        liveUnread = 0;
                        viewPressed = false;

                        refreshLayout.setRefreshing(false);

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

                        if (update) {
                            try {
                                c.close();
                            } catch (Exception e) {

                            }
                        }
    }*/

    // use the cursor to find which one has "1" in current position column
    public int getPosition(Cursor cursor) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                String s;
                do {
                    s = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_CURRENT_POS));
                    if (s != null && !s.isEmpty()) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            Log.v("talon_home_frag", "caught getting position on home timeline, getting the cursor adapter again");
            e.printStackTrace();
            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
            return -1;
        }

        return pos;
    }

    // find the id from the cursor to get the position
    public int getPosition(Cursor cursor, long id) {
        int pos = 0;

        try {
            if (cursor.moveToLast()) {
                do {
                    if (cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) < id) {
                        break;
                    } else {
                        pos++;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            Log.v("talon_home_frag", "caught getting position on home timeline, getting the cursor adapter again");
            e.printStackTrace();
            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_HOME"));
            return -1;
        }

        return pos;
    }

    /*@Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // data is not available anymore, delete reference
        Log.v("talon_timeline", "had to restart the loader for some reason, it was reset");

        resetTimeline(false);
    }*/

    public Handler handler = new Handler();
    public Runnable hideToast = new Runnable() {
        @Override
        public void run() {
            infoBar = false;
            hideToastBar(mLength);
        }
    };
    public long mLength;

    /*public void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        showToastBar(description, buttonText, length, quit, listener, false);
    }*/

    public boolean topViewToastShowing = false;

    /*public void showToastBar(final String description, final String buttonText, final long length, final boolean quit, final View.OnClickListener listener, boolean isLive) {
        try {
            if (!isToastShowing || isLive) {
                if (isToastShowing) {
                    if (topViewToastShowing) {
                        return;
                    }
                    infoBar = false;
                    hideToastBar(300);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            topViewToastShowing = true;
                            showToastBar(description, buttonText, length, quit, listener, false);
                        }
                    }, 350);
                } else {
                    infoBar = quit;

                    mLength = length;

                    toastDescription.setText(description);
                    toastButton.setText(buttonText);
                    toastButton.setOnClickListener(listener);

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
            } else if (!infoBar) {
                // this will change the # from top
                toastDescription.setText(description);
            }
        } catch (Exception e) {
            // fragment not attached
        }
    }*/

    public boolean isHiding = false;

    public void markReadForLoad() {
        try {
            final Cursor cursor = cursorAdapter.getCursor();
            final int current = listView.getFirstVisiblePosition();

            if (cursor.isClosed()) {
                return;
            }

            HomeDataSource.getInstance(context).markAllRead(currentAccount);

            if (cursor.moveToPosition(cursor.getCount() - current)) {
                Log.v("talon_marking_read", cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)) + "");
                final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                sharedPrefs.edit().putLong("current_position_" + currentAccount, id).apply();

                new TimeoutThread(new Runnable() {
                    @Override
                    public void run() {
                        markRead(currentAccount, context, id);
                    }
                }).start();
            } else {
                if (cursor.moveToLast()) {
                    final long id = cursor.getLong(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));
                    sharedPrefs.edit().putLong("current_position_" + currentAccount, id).apply();

                    new TimeoutThread(new Runnable() {
                        @Override
                        public void run() {
                            markRead(currentAccount, context, id);
                        }
                    }).start();
                }
            }
        } catch (IllegalStateException e) {
            // Home datasource is not open, so we manually close it to null out values and reset it
            e.printStackTrace();
            try {
                HomeDataSource.dataSource = null;
            } catch (Exception x) {

            }
        } catch (NullPointerException | StaleDataException | SQLiteDiskIOException e) {
            e.printStackTrace();
            // the cursoradapter is null
        }
    }

    public void markRead(int currentAccount, Context context, long id) {
        try {
            HomeDataSource.getInstance(context).markPosition(currentAccount, id);
        } catch (Throwable t) {

        }
    }

}