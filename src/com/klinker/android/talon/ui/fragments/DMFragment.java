package com.klinker.android.talon.ui.fragments;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.CursorListLoader;
import com.klinker.android.talon.adapters.TimeLineCursorAdapter;
import com.klinker.android.talon.services.DirectMessageRefreshService;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.DMDataSource;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.ConnectionDetector;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.Date;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class DMFragment extends Fragment implements OnRefreshListener {

    public static final int DM_REFRESH_ID = 125;

    private static Twitter twitter;
    private ConnectionDetector cd;

    private AsyncListView listView;
    private CursorAdapter cursorAdapter;

    public AppSettings settings;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;
    private PullToRefreshLayout mPullToRefreshLayout;

    private DMDataSource dataSource;

    static Activity context;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(context);
        cd = new ConnectionDetector(context);

        View layout = inflater.inflate(R.layout.main_fragments, null);
        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            Crouton.makeText(context, "No internet connection", Style.ALERT);
        }

        sharedPrefs.edit().putInt("dm_unread", 0).commit();

        dataSource = new DMDataSource(context);
        dataSource.open();

        listView = (AsyncListView) layout.findViewById(R.id.listView);

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

        new GetCursorAdapter().execute();

        MainActivity.refreshMe = false;

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

                if (settings.dmRefresh != 0) { // user only wants manual
                    AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    long now = new Date().getTime();
                    long alarm = now + settings.dmRefresh;

                    Log.v("alarm_date", "dircet message " + new Date(alarm).toString());

                    PendingIntent pendingIntent = PendingIntent.getService(context, DM_REFRESH_ID, new Intent(context, DirectMessageRefreshService.class), 0);

                    am.setRepeating(AlarmManager.RTC_WAKEUP, alarm, settings.dmRefresh, pendingIntent);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                if (update) {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), true);
                    refreshCursor();
                    CharSequence text = numberNew == 1 ?  numberNew +  " " + getResources().getString(R.string.new_direct_message) :  numberNew + " " + getResources().getString(R.string.new_direct_messages);
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                    listView.setSelectionFromTop(numberNew + 1, toDP(5));
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), true);
                    refreshCursor();

                    CharSequence text = getResources().getString(R.string.no_new_direct_messages);
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                }

                mPullToRefreshLayout.setRefreshComplete();

                DrawerActivity.canSwitch = true;
            }
        }.execute();
    }

    class GetCursorAdapter extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... args) {

            cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(sharedPrefs.getInt("current_account", 1)), true);

            return null;
        }

        protected void onPostExecute(String file_url) {

            attachCursor();
        }

    }

    public void swapCursors() {
        cursorAdapter.swapCursor(dataSource.getCursor(sharedPrefs.getInt("current_account", 1)));
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

        LinearLayout viewHeader = new LinearLayout(context);
        viewHeader.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, toDP(0));
        viewHeader.setLayoutParams(lp);

        try {
            listView.addHeaderView(viewHeader, null, false);
        } catch (Exception e) {

        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}