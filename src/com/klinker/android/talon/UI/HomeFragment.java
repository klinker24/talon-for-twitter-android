package com.klinker.android.talon.UI;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.klinker.android.talon.Adapters.MainDrawerArrayAdapter;
import com.klinker.android.talon.Adapters.MainDrawerClickListener;
import com.klinker.android.talon.Adapters.TimeLineCursorAdapter;
import com.klinker.android.talon.Adapters.TimeLineListLoader;
import com.klinker.android.talon.App;
import com.klinker.android.talon.R;
import com.klinker.android.talon.SQLite.DMDataSource;
import com.klinker.android.talon.SQLite.HomeDataSource;
import com.klinker.android.talon.SQLite.MentionsDataSource;
import com.klinker.android.talon.Utilities.*;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment implements PullToRefreshAttacher.OnRefreshListener {

    private static Twitter twitter;
    private ConnectionDetector cd;

    private AsyncListView listView;
    private CursorAdapter cursorAdapter;

    public AppSettings settings;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;

    private HomeDataSource dataSource;

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

        dataSource = new HomeDataSource(context);
        dataSource.open();

        listView = (AsyncListView) layout.findViewById(R.id.listView);

        mPullToRefreshAttacher = ((MainActivity) getActivity())
                .getPullToRefreshAttacher();
        mPullToRefreshAttacher.addRefreshableView(listView, this);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        TimeLineListLoader loader = new TimeLineListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        new GetCursorAdapter().execute();

        return layout;
    }

    @Override
    public void onRefreshStarted(View view) {
        new AsyncTask<Void, Void, Void>() {

            private boolean update;
            private int numberNew;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    twitter = Utils.getTwitter(context);

                    User user = twitter.verifyCredentials();
                    long lastId = sharedPrefs.getLong("last_tweet_id", 0);
                    Paging paging;
                    if (lastId != 0) {
                        paging = new Paging(1).sinceId(lastId);
                    } else {
                        paging = new Paging(1, 500);
                    }
                    List<twitter4j.Status> statuses = twitter.getHomeTimeline(paging);

                    if (statuses.size() != 0) {
                        sharedPrefs.edit().putLong("last_tweet_id", statuses.get(0).getId()).commit();
                        update = true;
                        numberNew = statuses.size();
                    } else {
                        update = false;
                        numberNew = 0;
                    }

                    Log.v("timeline_update", "Showing @" + user.getScreenName() + "'s home timeline.");
                    for (twitter4j.Status status : statuses) {
                        try {
                            dataSource.createTweet(status);
                        } catch (Exception e) {
                            break;
                        }
                    }

                } catch (TwitterException e) {
                    // Error in updating status
                    Log.d("Twitter Update Error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                if (update) {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(), false);
                    refreshCursor();
                    CharSequence text = numberNew == 1 ?  numberNew + " new tweet" :  numberNew + " new tweets";
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                    listView.smoothScrollToPosition(numberNew + 1);
                } else {
                    CharSequence text = "No new tweets";
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                }

                mPullToRefreshAttacher.setRefreshComplete();
            }
        }.execute();
    }

    class GetCursorAdapter extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... args) {

            cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(), false);

            return null;
        }

        protected void onPostExecute(String file_url) {

            attachCursor();
        }

    }

    public void swapCursors() {
        cursorAdapter.swapCursor(dataSource.getCursor());
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