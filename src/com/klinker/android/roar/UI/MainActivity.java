package com.klinker.android.roar.UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import com.klinker.android.roar.Adapters.TimeLineCursorAdapter;
import com.klinker.android.roar.Adapters.TimeLineListLoader;
import com.klinker.android.roar.App;
import com.klinker.android.roar.R;
import com.klinker.android.roar.SQLite.HomeDataSource;
import com.klinker.android.roar.Utilities.AppSettings;
import com.klinker.android.roar.Utilities.ConnectionDetector;
import com.klinker.android.roar.Utilities.Utils;
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

import java.util.List;

public class MainActivity extends Activity implements PullToRefreshAttacher.OnRefreshListener {

    private static Twitter twitter;
    private ConnectionDetector cd;

    private AsyncListView listView;
    private CursorAdapter cursorAdapter;

    public static AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;

    private HomeDataSource dataSource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        cd = new ConnectionDetector(getApplicationContext());

        // Check if Internet present
        if (!cd.isConnectingToInternet()) {
            Crouton.makeText(this, "No internet connection", Style.ALERT);
        }

        settings = new AppSettings(this);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        dataSource = new HomeDataSource(this);
        dataSource.open();

        listView = (AsyncListView) findViewById(R.id.listView);

        mPullToRefreshAttacher = PullToRefreshAttacher.get(this);

        BitmapLruCache cache = App.getInstance(this).getBitmapCache();
        TimeLineListLoader loader = new TimeLineListLoader(cache);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        new GetCursorAdapter().execute();

        // Retrieve the PullToRefreshLayout from the content view
        PullToRefreshLayout ptrLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
        ptrLayout.setPullToRefreshAttacher(mPullToRefreshAttacher, this);
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
                    cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor());
                    refreshCursor();
                    CharSequence text = numberNew + " new tweets";
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                    listView.smoothScrollToPosition(numberNew);
                } else {
                    CharSequence text = "No new tweets";
                    Crouton.makeText((Activity) context, text, Style.INFO).show();
                }

                mPullToRefreshAttacher.setRefreshComplete();
            }
        }.execute();
    }

    class GetName extends AsyncTask<String, Void, User> {

        protected User doInBackground(String... urls) {
            try {
                AccessToken accessToken = new AccessToken(settings.authenticationToken, settings.authenticationTokenSecret);
                long userID = accessToken.getUserId();

                Twitter twitter = Utils.getTwitter(context);

                return twitter.showUser(userID);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(User user) {
            try {
                String username = user.getName();
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * Function to logout from twitter It will just clear the application shared
     * preferences
     */
    private void logoutFromTwitter() {
        // Clear the shared preferences
        Editor e = sharedPrefs.edit();
        e.remove("authentication_token");
        e.remove("authentication_token_secret");
        e.remove("is_logged_in");
        e.commit();

        dataSource.deleteAllTweets();

        Intent login = new Intent(context, LoginActivity.class);
        startActivity(login);
    }


    class GetCursorAdapter extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... args) {

            cursorAdapter = new TimeLineCursorAdapter(context, dataSource.getCursor());

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

        listView.addHeaderView(viewHeader, null, false);
    }

    @Override
    protected void onResume() {
        dataSource.open();
        super.onResume();
    }

    @Override
    protected void onPause() {
        dataSource.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_compose:
                Intent compose = new Intent(context, ComposeActivity.class);
                startActivity(compose);
                return true;

            case R.id.menu_logout:
                logoutFromTwitter();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}