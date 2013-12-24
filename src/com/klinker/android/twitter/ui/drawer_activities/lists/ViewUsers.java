package com.klinker.android.twitter.ui.drawer_activities.lists;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.LinearLayout;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.UserListMembersArrayAdapter;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;

import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.User;

/**
 * Created by luke on 11/27/13.
 */
public class ViewUsers extends Activity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private ActionBar actionBar;

    private AsyncListView listView;

    private boolean canRefresh = true;

    private int listId;
    private String listName;

    private long currCursor = -1;

    private boolean bigEnough = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        listName = getIntent().getStringExtra("list_name");

        if (settings.advanceWindowed) {
            setUpWindow();
        }

        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(listName);

        setContentView(R.layout.list_view_activity);

        LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
        spinner.setVisibility(View.GONE);

        listView = (AsyncListView) findViewById(R.id.listView);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount) {
                    // Last item is fully visible.
                    if (canRefresh && bigEnough) {
                        new GetUsers().execute();
                    }

                    canRefresh = false;

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            canRefresh = true;
                        }
                    }, 4000);

                }
            }
        });

        listId = getIntent().getIntExtra("list_id", 0);

        new GetUsers().execute();

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

    class GetUsers extends AsyncTask<String, Void, ArrayList<User>> {

        protected ArrayList<User> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                PagableResponseList<User> users = twitter.getUserListMembers(listId, currCursor);

                currCursor = users.getNextCursor();

                ArrayList<User> array = new ArrayList<User>();

                for (User user : users) {
                    array.add(user);
                }

                bigEnough = array.size() > 19;

                return array;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(ArrayList<User> users) {
            final UserListMembersArrayAdapter people = new UserListMembersArrayAdapter(context, users, listId);
            final int firstVisible = listView.getFirstVisiblePosition();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listView.setAdapter(people);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            listView.setSelection(firstVisible);
                        }
                    }, 100);

                    listView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                        @Override
                        public boolean onPreDraw() {
                            if(listView.getFirstVisiblePosition() == firstVisible) {
                                listView.getViewTreeObserver().removeOnPreDrawListener(this);
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
                    });
                }
            });

            listView.setVisibility(View.VISIBLE);
        }
    }

}