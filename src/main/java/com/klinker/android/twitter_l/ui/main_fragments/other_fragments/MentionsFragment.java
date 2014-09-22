package com.klinker.android.twitter_l.ui.main_fragments.other_fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.SecondMentionsRefreshService;
import com.klinker.android.twitter_l.ui.MainActivity;
import com.klinker.android.twitter_l.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.ui.main_fragments.MainFragment;
import com.klinker.android.twitter_l.utils.Utils;

import java.util.Date;
import java.util.List;

import twitter4j.Paging;
import twitter4j.TwitterException;
import twitter4j.User;

public class MentionsFragment extends MainFragment {

    public static final int MENTIONS_REFRESH_ID = 127;

    public int unread = 0;

    public BroadcastReceiver refrehshMentions = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getCursorAdapter(false);
        }
    };

    @Override
    public void onRefreshStarted() {
        new AsyncTask<Void, Void, Cursor>() {

            private boolean update;
            private int numberNew;

            @Override
            protected void onPreExecute() {
                DrawerActivity.canSwitch = false;
            }

            @Override
            protected Cursor doInBackground(Void... params) {
                try {
                    twitter = Utils.getTwitter(context, DrawerActivity.settings);

                    User user = twitter.verifyCredentials();
                    long[] lastId = MentionsDataSource.getInstance(context).getLastIds(currentAccount);

                    Paging paging;
                    paging = new Paging(1, 200);
                    if (lastId[0] > 0) {
                        paging.setSinceId(lastId[0]);
                    }

                    List<twitter4j.Status> statuses = twitter.getMentionsTimeline(paging);

                    if (statuses.size() != 0) {
                        update = true;
                        numberNew = statuses.size();
                    } else {
                        update = false;
                        numberNew = 0;
                    }

                    MentionsDataSource dataSource = MentionsDataSource.getInstance(context);

                    try {
                        dataSource.markAllRead(settings.currentAccount);
                    } catch (Throwable e) {

                    }

                    numberNew = dataSource.insertTweets(statuses, currentAccount);
                    unread = numberNew;

                } catch (TwitterException e) {
                    // Error in updating status
                    Log.d("Twitter Update Error", e.getMessage());
                }

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                long now = new Date().getTime();
                long alarm = now + DrawerActivity.settings.mentionsRefresh;

                PendingIntent pendingIntent = PendingIntent.getService(context, MENTIONS_REFRESH_ID, new Intent(context, MentionsRefreshService.class), 0);

                if (DrawerActivity.settings.mentionsRefresh != 0)
                    am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, DrawerActivity.settings.mentionsRefresh, pendingIntent);
                else
                    am.cancel(pendingIntent);

                if (DrawerActivity.settings.syncSecondMentions) {
                    // refresh the second account
                    context.startService(new Intent(context, SecondMentionsRefreshService.class));
                }

                return MentionsDataSource.getInstance(context).getCursor(sharedPrefs.getInt("current_account", 1));
            }

            @Override
            protected void onPostExecute(Cursor cursor) {

                Cursor c = null;
                try {
                    c = cursorAdapter.getCursor();
                } catch (Exception e) {

                }

                cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, MentionsFragment.this);
                attachCursor();

                try {
                    if (update) {
                        CharSequence text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_mention) :  numberNew + " " + getResources().getString(R.string.new_mentions);
                        showToastBar(text + "", jumpToTop, 400, true, toTopListener);
                        int size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                        try {
                            listView.setSelectionFromTop(numberNew + listView.getHeaderViewsCount() -
                                            (getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                            (settings.jumpingWorkaround ? 1 : 0),
                                    size);
                        } catch (Exception e) {
                            // not attached
                        }
                    } else {
                        CharSequence text = getResources().getString(R.string.no_new_mentions);
                        showToastBar(text + "", allRead, 400, true, toTopListener);
                    }
                } catch (Exception e) {
                    // user closed the app before it was done
                }

                refreshLayout.setRefreshing(false);

                DrawerActivity.canSwitch = true;

                try {
                    c.close();
                } catch (Exception e) {

                }
            }
        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean("refresh_me_mentions", false)) {
            getCursorAdapter(false);
            sharedPrefs.edit().putBoolean("refresh_me_mentions", false).commit();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.REFRESH_MENTIONS");
        context.registerReceiver(refrehshMentions, filter);

        filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.NEW_MENTION");
        context.registerReceiver(refrehshMentions, filter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        try {
            MentionsDataSource.getInstance(context).markAllRead(sharedPrefs.getInt("current_account", 1));
        } catch (Exception e) {

        }
        super.onStop();
    }

    public void getCursorAdapter(boolean showSpinner) {
        if (showSpinner) {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) { }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor;
                try {
                    cursor = MentionsDataSource.getInstance(context).getCursor(sharedPrefs.getInt("current_account", 1));
                } catch (Exception e) {
                    MentionsDataSource.dataSource = null;
                    getCursorAdapter(true);
                    return;
                }

                try {
                    Log.v("talon_databases", "mentions cursor size: " + cursor.getCount());
                } catch (Exception e) {
                    MentionsDataSource.dataSource = null;
                    getCursorAdapter(true);
                    return;
                }

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Cursor c = null;
                        if (cursorAdapter != null) {
                            c = cursorAdapter.getCursor();
                        }

                        cursorAdapter = new TimeLineCursorAdapter(context, cursor, false, MentionsFragment.this);

                        try {
                            spinner.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        } catch (Exception e) { }

                        attachCursor();

                        if (c != null) {
                            try {
                                c.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onPause() {

        int mUnread = listView.getFirstVisiblePosition();

        if (unread > 0) {
            MentionsDataSource.getInstance(context).markMultipleRead(mUnread, currentAccount);

            unread = mUnread;
        }

        context.unregisterReceiver(refrehshMentions);

        super.onPause();
    }


    public void attachCursor() {
        try {
            listView.setAdapter(cursorAdapter);
        } catch (Exception e) {

        }

        int newTweets;

        try {
            newTweets = MentionsDataSource.getInstance(context).getUnreadCount(currentAccount);
        } catch (Exception e) {
            newTweets = 0;
        }

        if (newTweets > 0) {
            unread = newTweets;
            int size = mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
            try {
                listView.setSelectionFromTop(newTweets + listView.getHeaderViewsCount() -
                                (getResources().getBoolean(R.bool.isTablet) ? 1 : 0) -
                                (settings.jumpingWorkaround ? 1 : 0),
                        size);
            } catch (Exception e) {
                // not attached
            }
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

    public void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
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

    public void hideToastBar(long length) {
        mLength = length;

        if (!isToastShowing) {
            return;
        }

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
}