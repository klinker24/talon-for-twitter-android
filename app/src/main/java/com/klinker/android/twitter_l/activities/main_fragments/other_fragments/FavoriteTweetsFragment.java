package com.klinker.android.twitter_l.activities.main_fragments.other_fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteTweetsDataSource;
import com.klinker.android.twitter_l.activities.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.activities.main_fragments.MainFragment;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import twitter4j.Paging;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

public class FavoriteTweetsFragment extends MainFragment {

    public boolean newTweets = false;

    public BroadcastReceiver resetLists = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(true);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.RESET_FAVORITES");
        context.registerReceiver(resetLists, filter);
    }

    public boolean manualRefresh = false;

    public int doRefresh() {
        int numberNew = 0;

        try {

            twitter = Utils.getTwitter(context, DrawerActivity.settings);

            long[] lastId = FavoriteTweetsDataSource.getInstance(context).getLastIds(currentAccount);

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
                        List<Status> list = twitter.getFavorites(settings.myScreenName, paging);

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

            FavoriteTweetsDataSource dataSource = FavoriteTweetsDataSource.getInstance(context);
            numberNew = dataSource.insertTweets(statuses, currentAccount, lastId);

            return numberNew;

        } catch (Exception e) {
            // Error in updating status
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

                            text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_tweet) :  numberNew + " " + getResources().getString(R.string.new_tweets);

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
        context.unregisterReceiver(resetLists);
        super.onPause();
    }

    public long listId;

    public void getCursorAdapter(final boolean bSpinner) {

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
                    cursor = FavoriteTweetsDataSource.getInstance(context).getCursor(currentAccount);
                } catch (Exception e) {
                    FavoriteTweetsDataSource.dataSource = null;
                    context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_FAVORITE"));
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

                        try {
                            Log.v("talon_list", "number of tweets in favorites: " + cursor.getCount());
                        } catch (Exception e) {
                            e.printStackTrace();
                            // the cursor or database is closed, so we will null out the datasource and restart the get cursor method
                            FavoriteTweetsDataSource.dataSource = null;
                            context.sendBroadcast(new Intent("com.klinker.android.twitter.RESET_FAVORITE"));
                            return;
                        }

                        stopCurrentVideos();
                        if (cursorAdapter != null) {
                            TimeLineCursorAdapter cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, FavoriteTweetsFragment.this);
                            cursorAdapter.setQuotedTweets(FavoriteTweetsFragment.this.cursorAdapter.getQuotedTweets());
                            FavoriteTweetsFragment.this.cursorAdapter = cursorAdapter;
                        } else {
                            cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, FavoriteTweetsFragment.this);
                        }

                        applyAdapter();

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

    public Handler handler = new Handler();
    public Runnable hideToast = new Runnable() {
        @Override
        public void run() {
            hideToastBar(mLength);
            infoBar = false;
        }
    };
    public long mLength;

    public void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        if (quit) {
            infoBar = true;
        } else {
            infoBar = false;
        }

        if (!settings.useSnackbar && !overrideSnackbarSetting) {
            return;
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

    public void hideToastBar(long length) {
        if (!isToastShowing || (!settings.useSnackbar && !overrideSnackbarSetting)) {
            return;
        }

        mLength = length;

        isToastShowing = false;

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.fade_out);
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
            infoBar = false;
            toastDescription.setText(text);
            toastButton.setText(button);
        } else if (text.equals("0 " + fromTop) || text.equals("1 " + fromTop) || text.equals("2 " + fromTop)) {
            hideToastBar(400);
        }
    }

}