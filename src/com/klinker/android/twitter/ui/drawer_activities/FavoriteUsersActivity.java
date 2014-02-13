package com.klinker.android.twitter.ui.drawer_activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.FavoriteUsersCursorAdapter;
import com.klinker.android.twitter.adapters.PeopleCursorAdapter;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersDataSource;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.LoginActivity;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

public class FavoriteUsersActivity extends DrawerActivity {

    private static FavoriteUsersDataSource dataSource;
    private boolean landscape;
    private static AsyncListView list;
    private static Context sContext;
    private static SharedPreferences sSharedPrefs;
    private static LinearLayout spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        context = this;
        sContext = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        sSharedPrefs = sharedPrefs;
        settings = new AppSettings(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setUpTheme();
        setContentView(R.layout.retweets_activity);
        setUpDrawer(5, getResources().getString(R.string.favorite_users));

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.favorite_users));


        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        spinner = (LinearLayout) findViewById(R.id.list_progress);

        listView = (AsyncListView) findViewById(R.id.listView);
        list = listView;

        View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);

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
                listView.setFooterDividersEnabled(false);
            }
        }

        final boolean isTablet = getResources().getBoolean(R.bool.isTablet);

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
                        if (firstVisibleItem > mLastFirstVisibleItem) {
                            if (!landscape && !isTablet) {
                                actionBar.hide();
                            }
                        } else if (firstVisibleItem < mLastFirstVisibleItem) {
                            if(!landscape && !isTablet) {
                                actionBar.show();
                            }
                            if (translucent) {
                                statusBar.setVisibility(View.VISIBLE);
                            }
                        }

                        mLastFirstVisibleItem = firstVisibleItem;
                    }
                } else {
                    if(!landscape && !isTablet) {
                        actionBar.show();
                    }
                }

                if (MainActivity.translucent && actionBar.isShowing()) {
                    showStatusBar();
                } else if (MainActivity.translucent) {
                    hideStatusBar();
                }
            }
        });

        LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        new GetFavUsers().execute();

    }

    @Override
    public void onResume() {
        super.onResume();

        new GetFavUsers().execute();
    }

    private static FavoriteUsersCursorAdapter people;

    public static void refreshFavs() {
        new GetFavUsers().execute();
    }

    static class GetFavUsers extends AsyncTask<String, Void, Cursor> {

        protected Cursor doInBackground(String... urls) {
            try {
                dataSource = new FavoriteUsersDataSource(sContext);
                dataSource.open();

                return dataSource.getCursor(sSharedPrefs.getInt("current_account", 1));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Cursor cursor) {

            Log.v("fav_users", cursor.getCount() + "");

            people = new FavoriteUsersCursorAdapter(sContext, cursor);
            list.setAdapter(people);
            list.setVisibility(View.VISIBLE);

            spinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            mDrawerToggle.onConfigurationChanged(newConfig);
        } catch (Exception e) { }

        overridePendingTransition(0,0);
        finish();
        Intent restart = new Intent(context, FavoriteUsersActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }

}