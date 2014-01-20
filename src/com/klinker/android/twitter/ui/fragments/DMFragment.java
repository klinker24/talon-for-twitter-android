package com.klinker.android.twitter.ui.fragments;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
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
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.CursorListLoader;
import com.klinker.android.twitter.adapters.DirectMessageListArrayAdapter;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.data.sq_lite.DMSQLiteHelper;
import com.klinker.android.twitter.services.DirectMessageRefreshService;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class DMFragment extends Fragment implements OnRefreshListener {

    public static final int DM_REFRESH_ID = 125;

    private static Twitter twitter;

    private AsyncListView listView;
    private ArrayAdapter cursorAdapter;

    private SharedPreferences sharedPrefs;

    private PullToRefreshLayout mPullToRefreshLayout;
    private LinearLayout spinner;

    private DMDataSource dataSource;

    static Activity context;

    private boolean landscape;

    private ActionBar actionBar;
    private int mActionBarSize;

    private String jumpToTop;
    private String fromTop;
    private String allRead;

    private View.OnClickListener toTopListener;

    public BroadcastReceiver jumpTopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            toTop();
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

        Log.v("setting_fragments", "dm fragment");

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

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

        sharedPrefs.edit().putInt("dm_unread_" + sharedPrefs.getInt("current_account", 1), 0).commit();

        dataSource = new DMDataSource(context);
        dataSource.open();

        listView = (AsyncListView) layout.findViewById(R.id.listView);
        spinner = (LinearLayout) layout.findViewById(R.id.spinner);

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

        /*BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        CursorListLoader loader = new CursorListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());*/

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

        new GetCursorAdapter().execute();
        final boolean isTablet = getResources().getBoolean(R.bool.isTablet);

        if (DrawerActivity.settings.uiExtras) {
            listView.setOnScrollListener(new AbsListView.OnScrollListener() {

                int mLastFirstVisibleItem = 0;

                @Override
                public void onScrollStateChanged(AbsListView absListView, int i) {

                }

                @Override
                public void onScroll(AbsListView absListView, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                    // show and hide the action bar
                    if (firstVisibleItem != 0) {
                        if (MainActivity.canSwitch) {
                            // used to show and hide the action bar
                            if (firstVisibleItem < 3) {

                            } else if (firstVisibleItem < mLastFirstVisibleItem) {
                                if(!landscape && !isTablet) {
                                    actionBar.hide();
                                }
                                if (!isToastShowing && DrawerActivity.settings.useToast) {
                                    showToastBar(firstVisibleItem + " " + fromTop, jumpToTop, 400, false, toTopListener);
                                }
                            } else if (firstVisibleItem > mLastFirstVisibleItem) {
                                if(!landscape && !isTablet) {
                                    actionBar.show();
                                }
                                if (isToastShowing && !infoBar && DrawerActivity.settings.useToast) {
                                    hideToastBar(400);
                                }
                            }

                            mLastFirstVisibleItem = firstVisibleItem;
                        }
                    } else {
                        if(!landscape && !isTablet) {
                            actionBar.show();
                        }
                        if (!infoBar && DrawerActivity.settings.useToast) {
                            hideToastBar(400);
                        }
                    }

                    if (isToastShowing && !infoBar && DrawerActivity.settings.useToast) {
                        updateToastText(firstVisibleItem + " " + fromTop);
                    }

                    if (MainActivity.translucent && actionBar.isShowing()) {
                        showStatusBar();
                    } else if (MainActivity.translucent) {
                        hideStatusBar();
                    }
                }

            });
        }

        setUpToastBar(layout);

        toTopListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toTop();
            }
        };

        return layout;
    }

    public void toTop() {
        try {
            if (Integer.parseInt(toastDescription.getText().toString().split(" ")[0]) > 100) {
                listView.setSelection(0);
            } else {
                listView.smoothScrollToPosition(0);
            }
        } catch (Exception e) {
            listView.smoothScrollToPosition(0);
        }
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
                    twitter = Utils.getTwitter(context, DrawerActivity.settings);

                    User user = twitter.verifyCredentials();
                    long lastId = sharedPrefs.getLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), 0);
                    Paging paging;
                    if (lastId != 0) {
                        paging = new Paging(1).sinceId(lastId);
                    } else {
                        paging = new Paging(1, 500);
                    }

                    List<DirectMessage> dm = twitter.getDirectMessages(paging);
                    List<DirectMessage> sent = twitter.getSentDirectMessages(paging);

                    if (dm.size() != 0) {
                        sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), dm.get(0).getId()).commit();
                        update = true;
                        numberNew = dm.size();
                    } else {
                        update = false;
                        numberNew = 0;
                    }

                    for (DirectMessage directMessage : dm) {
                        try {
                            dataSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        } catch (Exception e) {
                            break;
                        }
                    }

                    for (DirectMessage directMessage : sent) {
                        try {
                            dataSource.createDirectMessage(directMessage, sharedPrefs.getInt("current_account", 1));
                        } catch (Exception e) {
                            break;
                        }
                    }

                } catch (TwitterException e) {
                    // Error in updating status
                    Log.d("Twitter Update Error", e.getMessage());
                }


                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                long now = new Date().getTime();
                long alarm = now + DrawerActivity.settings.dmRefresh;

                Log.v("alarm_date", "direct message " + new Date(alarm).toString());

                PendingIntent pendingIntent = PendingIntent.getService(context, DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);

                if (DrawerActivity.settings.dmRefresh != 0)
                    am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, DrawerActivity.settings.dmRefresh, pendingIntent);
                else
                    am.cancel(pendingIntent);


                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                /*if (update) {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), true);
                    refreshCursor();

                    CharSequence text = numberNew == 1 ?  numberNew +  " " + getResources().getString(R.string.new_direct_message) :  numberNew + " " + getResources().getString(R.string.new_direct_messages);
                    showToastBar(text + "", jumpToTop, 400, true, toTopListener);

                    int size = toDP(5) + mActionBarSize + (DrawerActivity.translucent ? DrawerActivity.statusBarHeight : 0);
                    listView.setSelectionFromTop(numberNew + (MainActivity.isPopup || landscape ? 1 : 2), size);
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), true);
                    refreshCursor();

                    CharSequence text = getResources().getString(R.string.no_new_direct_messages);
                    showToastBar(text + "", allRead, 400, true, toTopListener);
                }*/

                mPullToRefreshLayout.setRefreshComplete();

                DrawerActivity.canSwitch = true;
            }
        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sharedPrefs.getBoolean("refresh_me_dm", false)) {
            new GetCursorAdapter().execute();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.TOP_TIMELINE");
        context.registerReceiver(jumpTopReceiver, filter);

        sharedPrefs.edit().putBoolean("refresh_me_dm", false).commit();
    }

    @Override
    public void onPause() {
        try {
            context.unregisterReceiver(jumpTopReceiver);
        } catch (Exception e) {
            // not registered
        }

        super.onPause();
    }

    class GetCursorAdapter extends AsyncTask<Void, Void, String> {

        protected void onPreExecute() {
            try {
                spinner.setVisibility(View.VISIBLE);
                listView.setVisibility(View.GONE);
            } catch (Exception e) { }
        }

        protected String doInBackground(Void... args) {

            Cursor cursor = dataSource.getCursor(sharedPrefs.getInt("current_account", 1));

            ArrayList<com.klinker.android.twitter.data.DirectMessage> messageList = new ArrayList<com.klinker.android.twitter.data.DirectMessage>();
            ArrayList<String> names = new ArrayList<String>();

            if (cursor.moveToLast()) {
                do {
                    String screenname = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_SCREEN_NAME));
                    String otherName = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_RETWEETER));

                    if (!names.contains(screenname) && !screenname.equals(DrawerActivity.settings.myScreenName)) {
                        Log.v("direct_message", "adding screenname: " + screenname);
                        names.add(screenname);

                        String name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_NAME));
                        String message = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TEXT));
                        String profilePic = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_PRO_PIC));

                        messageList.add(new com.klinker.android.twitter.data.DirectMessage(name, screenname, message, profilePic));
                    } else if (screenname.equals(DrawerActivity.settings.myScreenName) && !names.contains(otherName)) {

                        names.add(otherName);

                        String name = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_EXTRA_TWO));
                        String message = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_TEXT));
                        String profilePic = cursor.getString(cursor.getColumnIndex(DMSQLiteHelper.COLUMN_EXTRA_ONE));

                        messageList.add(new com.klinker.android.twitter.data.DirectMessage(name, screenname, message, profilePic));
                    }
                } while (cursor.moveToPrevious());
            }

            cursorAdapter = new DirectMessageListArrayAdapter(context, messageList);
            //cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), true);

            return null;
        }

        protected void onPostExecute(String file_url) {

            try {
                spinner.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            } catch (Exception e) { }

            listView.setAdapter(cursorAdapter);
            //attachCursor();
        }

    }

    public void swapCursors() {
        //cursorAdapter.swapCursor(dataSource.getCursor(sharedPrefs.getInt("current_account", 1)));
        cursorAdapter.notifyDataSetChanged();
    }

    public void refreshCursor() {
        listView.setAdapter(cursorAdapter);

        swapCursors();
    }

    @SuppressWarnings("deprecation")
    public void attachCursor() {
        listView.setAdapter(cursorAdapter);

        swapCursors();
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