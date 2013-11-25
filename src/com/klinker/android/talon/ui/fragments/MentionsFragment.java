package com.klinker.android.talon.ui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.klinker.android.talon.adapters.CursorListLoader;
import com.klinker.android.talon.adapters.TimeLineCursorAdapter;
import com.klinker.android.talon.ui.MainActivity;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.utils.*;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;
import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.util.List;

public class MentionsFragment extends Fragment implements OnRefreshListener {

    private static Twitter twitter;
    private ConnectionDetector cd;

    private AsyncListView listView;
    private CursorAdapter cursorAdapter;

    public AppSettings settings;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;
    private PullToRefreshLayout mPullToRefreshLayout;

    private MentionsDataSource dataSource;

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

        new GetCursorAdapter().execute();

        MainActivity.startUp = false;

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
                    long lastId = sharedPrefs.getLong("last_mention_id", 0);
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
                        sharedPrefs.edit().putLong("last_mention_id", statuses.get(0).getId()).commit();
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
                    CharSequence text = numberNew == 1 ?  numberNew + " " + getResources().getString(R.string.new_mention) :  numberNew + " " + getResources().getString(R.string.new_mentions);
                    Crouton.makeText(context, text, Style.INFO).show();
                    listView.setSelectionFromTop(numberNew + 1, toDP(5));
                } else {
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor(), false);
                    refreshCursor();

                    CharSequence text = getResources().getString(R.string.no_new_mentions);
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                }

                mPullToRefreshLayout.setRefreshComplete();
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