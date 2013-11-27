package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.ArrayListLoader;
import com.klinker.android.talon.adapters.TimelineArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;

/**
 * Created by luke on 11/27/13.
 */
public class ChoosenListActivity extends Activity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private ActionBar actionBar;

    public static ViewPager mViewPager;

    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawer;
    private ListView drawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private AsyncListView listView;

    private boolean logoutVisible = false;

    private int listId;
    private String listName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        if (settings.advanceWindowed) {
            setUpWindow();
        }
        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));

        setContentView(R.layout.retweets_activity);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        listName = getIntent().getStringExtra("list_name");

        listId = Integer.parseInt(getIntent().getStringExtra("list_id"));
        actionBar.setTitle(listName);

        new GetList().execute();

    }

    public void setUpWindow() {

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .75f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

    }

    public void setUpTheme() {

        switch (settings.theme) {
            case AppSettings.THEME_LIGHT:
                setTheme(R.style.Theme_TalonLight_Popup);
                break;
            case AppSettings.THEME_DARK:
                setTheme(R.style.Theme_TalonDark_Popup);
                break;
            case AppSettings.THEME_BLACK:
                setTheme(R.style.Theme_TalonBlack_Popup);
                break;
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    class GetList extends AsyncTask<String, Void, ResponseList<Status>> {

        protected ResponseList<twitter4j.Status> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                Log.v("list_id", listId + "");
                Paging paging = new Paging(1, 100);
                ResponseList<twitter4j.Status> statuses = twitter.getUserListStatuses(listId, new Paging(1, 100));

                return statuses;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {

            if (statuses != null) {
                ArrayList<twitter4j.Status> arrayList = new ArrayList<twitter4j.Status>();
                for (twitter4j.Status s : statuses) {
                    arrayList.add(s);
                }

                listView.setAdapter(new TimelineArrayAdapter(context, arrayList));
                listView.setVisibility(View.VISIBLE);

                /*LinearLayout viewHeader = new LinearLayout(context);
                viewHeader.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, toDP(0));
                viewHeader.setLayoutParams(lp);

                try {
                    listView.addHeaderView(viewHeader, null, false);
                } catch (Exception e) {

                }   */
            }

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

}
