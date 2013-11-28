package com.klinker.android.talon.ui.drawer_activities.trends;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.TrendsArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.LoginActivity;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.ArrayList;

import twitter4j.Trend;
import twitter4j.Twitter;

/**
 * Created by luke on 11/27/13.
 */
public class Trends extends DrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.trends));

        setContentView(R.layout.retweets_activity);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);
        listView.setDividerHeight(toDP(5));

        setUpDrawer(7, getResources().getString(R.string.trends));

        new GetTrends().execute();

    }

    @Override
    public void onResume() {
        super.onResume();
        setUpDrawer(7, getResources().getString(R.string.trends));
    }

    class GetTrends extends AsyncTask<String, Void, ArrayList<String>> {

        protected ArrayList<String> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                twitter4j.Trends trends = twitter.getPlaceTrends(1);
                ArrayList<String> currentTrends = new ArrayList<String>();

                for(Trend t: trends.getTrends()){
                    String name = t.getName();
                    currentTrends.add(name);
                }

                return currentTrends;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(ArrayList<String> trends) {

            listView.setAdapter(new TrendsArrayAdapter(context, trends));
            listView.setVisibility(View.VISIBLE);

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

}