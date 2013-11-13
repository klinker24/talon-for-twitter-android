package com.klinker.android.roar.UI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.klinker.android.roar.Adapters.TimeLineCursorAdapter;
import com.klinker.android.roar.Adapters.TimeLineListLoader;
import com.klinker.android.roar.App;
import com.klinker.android.roar.R;
import com.klinker.android.roar.SQLite.HomeDataSource;
import com.klinker.android.roar.Utilities.AppSettings;
import com.klinker.android.roar.Utilities.CircleTransform;
import com.klinker.android.roar.Utilities.ConnectionDetector;
import com.klinker.android.roar.Utilities.Utils;
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

import java.util.List;

public class MainActivity extends Activity implements PullToRefreshAttacher.OnRefreshListener {

    private static Twitter twitter;
    private ConnectionDetector cd;

    private AsyncListView listView;
    private CursorAdapter cursorAdapter;

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private PullToRefreshAttacher mPullToRefreshAttacher;

    private HomeDataSource dataSource;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);
        //cd = new ConnectionDetector(getApplicationContext());

        setUpTheme();
        setContentView(R.layout.main_activity);
        // Check if Internet present
        //if (!cd.isConnectingToInternet()) {
            //Crouton.makeText(this, "No internet connection", Style.ALERT);
        //}
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
        TimeLineListLoader loader = new TimeLineListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        new GetCursorAdapter().execute();

        // Retrieve the PullToRefreshLayout from the content view
        PullToRefreshLayout ptrLayout = (PullToRefreshLayout) findViewById(R.id.ptr_layout);
        ptrLayout.setPullToRefreshAttacher(mPullToRefreshAttacher, this);

        setUpDrawer();
    }

    public void setUpDrawer() {

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.drawerIcon});
        int resource = a.getResourceId(0, 0);
        a.recycle();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawer = (LinearLayout) findViewById(R.id.left_drawer);

        TextView name = (TextView) mDrawer.findViewById(R.id.name);
        TextView screenName = (TextView) mDrawer.findViewById(R.id.screen_name);
        ImageView backgroundPic = (ImageView) mDrawer.findViewById(R.id.background_image);
        ImageView profilePic = (ImageView) mDrawer.findViewById(R.id.profile_pic);
        ImageButton showMoreDrawer = (ImageButton) mDrawer.findViewById(R.id.options);
        final Button logoutDrawer = (Button) mDrawer.findViewById(R.id.logout);
        final ListView drawerList = (ListView) mDrawer.findViewById(R.id.drawer_list);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                resource,  /* nav drawer icon to replace 'Up' caret */
                R.string.app_name,  /* "open drawer" description */
                R.string.app_name  /* "close drawer" description */
        ) {

            public void onDrawerClosed(View view) {
                logoutDrawer.setVisibility(View.GONE);
                drawerList.setVisibility(View.VISIBLE);

            }

            public void onDrawerOpened(View drawerView) {

            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        showMoreDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(logoutDrawer.getVisibility() == View.GONE) {
                    logoutDrawer.setVisibility(View.VISIBLE);
                    drawerList.setVisibility(View.GONE);
                } else {
                    logoutDrawer.setVisibility(View.GONE);
                    drawerList.setVisibility(View.VISIBLE);
                }
            }
        });

        logoutDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutFromTwitter();
            }
        });

        String sName = sharedPrefs.getString("twitter_users_name", "");
        String sScreenName = sharedPrefs.getString("twitter_screen_name", "");
        String backgroundUrl = sharedPrefs.getString("twitter_background_url", "");
        String profilePicUrl = sharedPrefs.getString("profile_pic_url", "");

        Log.v("twitter_drawer", profilePicUrl);

        name.setText(sName);
        screenName.setText("@" + sScreenName);

        try {
            Picasso.with(context)
                    .load(backgroundUrl)
                    .into(backgroundPic);
        } catch (Exception e) {
            // empty path for some reason
        }

        try {
            Picasso.with(context)
                    .load(profilePicUrl)
                    .transform(new CircleTransform())
                    .into(profilePic);
        } catch (Exception e) {
            // empty path again
        }

    }

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack);
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
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

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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