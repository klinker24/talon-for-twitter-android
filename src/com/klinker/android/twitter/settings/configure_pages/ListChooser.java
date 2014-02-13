package com.klinker.android.twitter.settings.configure_pages;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ListsArrayAdapter;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.LoginActivity;
import com.klinker.android.twitter.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter.ui.widgets.ActionBarDrawerToggle;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import java.util.Collections;
import java.util.Comparator;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.UserList;


public class ListChooser extends Activity {

    private Context context;
    private SharedPreferences sharedPrefs;
    private AppSettings settings;
    private ActionBar actionBar;

    private AsyncListView listView;
    private ListChooserArrayAdapter arrayAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        Utils.setUpTheme(context, settings);
        setContentView(R.layout.list_chooser);

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));


        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                UserList list = arrayAdapter.getItem(i);
                Intent returnIntent = new Intent();
                returnIntent.putExtra("listId",list.getId());
                returnIntent.putExtra("listName", list.getName());
                setResult(RESULT_OK,returnIntent);
                finish();
            }
        });

        new GetLists().execute();
    }

    class GetLists extends AsyncTask<String, Void, ResponseList<UserList>> {

        protected ResponseList<UserList> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context, settings);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myScreenName);

                Collections.sort(lists, new Comparator<UserList>() {
                    public int compare(UserList result1, UserList result2) {
                        return result1.getName().compareTo(result2.getName());
                    }
                });

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(ResponseList<UserList> lists) {

            if (lists != null) {
                arrayAdapter = new ListChooserArrayAdapter(context, lists);
                listView.setAdapter(arrayAdapter);
                listView.setVisibility(View.VISIBLE);
            }

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

}
