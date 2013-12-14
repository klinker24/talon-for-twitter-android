package com.klinker.android.talon.ui.drawer_activities.lists;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.talon.R;
import com.klinker.android.talon.adapters.ListsArrayAdapter;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.LoginActivity;
import com.klinker.android.talon.ui.drawer_activities.DrawerActivity;
import com.klinker.android.talon.ui.widgets.HoloEditText;
import com.klinker.android.talon.utils.Utils;

import org.lucasr.smoothie.AsyncListView;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.UserList;

/**
 * Created by luke on 11/27/13.
 */
public class ListsActivity extends DrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        settings = new AppSettings(this);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));

        setContentView(R.layout.text_list_view_land);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);
        View viewHeader = getLayoutInflater().inflate(R.layout.ab_header, null);
        listView.addHeaderView(viewHeader, null, false);
        listView.setHeaderDividersEnabled(false);

        if (DrawerActivity.translucent) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        setUpDrawer(6, getResources().getString(R.string.lists));

        new GetLists().execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private boolean clicked = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_add_list:
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.create_list_dialog);
                dialog.setTitle(getResources().getString(R.string.create_new_list) + ":");

                final HoloEditText name = (HoloEditText) dialog.findViewById(R.id.name);
                final HoloEditText description = (HoloEditText) dialog.findViewById(R.id.description);

                Button cancel = (Button) dialog.findViewById(R.id.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!clicked) {
                            dialog.dismiss();
                        }
                        clicked = true;
                    }
                });

                Button privateBtn = (Button) dialog.findViewById(R.id.private_btn);
                privateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!clicked) {
                            new CreateList(name.getText().toString(), false, description.getText().toString()).execute();
                            dialog.dismiss();
                        }
                        clicked = true;
                    }
                });

                Button publicBtn = (Button) dialog.findViewById(R.id.public_btn);
                publicBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!clicked) {
                            new CreateList(name.getText().toString(), true, description.getText().toString()).execute();
                            dialog.dismiss();
                        }
                        clicked = true;
                    }
                });

                dialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class GetLists extends AsyncTask<String, Void, ResponseList<UserList>> {

        protected ResponseList<UserList> doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                ResponseList<UserList> lists = twitter.getUserLists(settings.myScreenName);

                return lists;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(ResponseList<UserList> lists) {

            listView.setAdapter(new ListsArrayAdapter(context, lists));
            listView.setVisibility(View.VISIBLE);

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

    class CreateList extends AsyncTask<String, Void, Boolean> {

        String name;
        String description;
        boolean publicList;

        public CreateList(String name, boolean publicList, String description) {
            this.name = name;
            this.publicList = publicList;
            this.description = description;
        }

        protected Boolean doInBackground(String... urls) {
            try {
                Twitter twitter =  Utils.getTwitter(context);

                twitter.createUserList(name, publicList, description);

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        protected void onPostExecute(Boolean created) {
            if (created) {
                recreate();
            } else {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
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
        Intent restart = new Intent(context, ListsActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        startActivity(restart);
    }
}
