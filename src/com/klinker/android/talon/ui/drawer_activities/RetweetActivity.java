package com.klinker.android.talon.ui.drawer_activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.ArrayListLoader;
import com.klinker.android.talon.adapters.TimelineArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.LoginActivity;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;

public class RetweetActivity extends DrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.retweets));

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

        setUpDrawer(3, getResources().getString(R.string.retweets));

        new GetRetweets().execute();

    }

    class GetRetweets extends AsyncTask<String, Void, ResponseList<twitter4j.Status>> {

        protected ResponseList<twitter4j.Status> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                Paging paging = new Paging(1, 100);

                ResponseList<twitter4j.Status> statuses = twitter.getRetweetsOfMe(paging);

                return statuses;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {

            ArrayList<twitter4j.Status> arrayList = new ArrayList<twitter4j.Status>();
            for (twitter4j.Status s : statuses) {
                arrayList.add(s);
            }

            listView.setAdapter(new TimelineArrayAdapter(context, arrayList, TimelineArrayAdapter.RETWEET));
            listView.setVisibility(View.VISIBLE);


            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

}