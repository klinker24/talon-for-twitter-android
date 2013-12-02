package com.klinker.android.talon.ui.drawer_activities.lists;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

        setUpTheme();

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));

        setContentView(R.layout.text_list_view);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);

        setUpDrawer(6, getResources().getString(R.string.lists));

        new GetLists().execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

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
                        dialog.dismiss();
                    }
                });

                Button privateBtn = (Button) dialog.findViewById(R.id.private_btn);
                privateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new CreateList(name.getText().toString(), false, description.getText().toString()).execute();
                        dialog.dismiss();
                    }
                });

                Button publicBtn = (Button) dialog.findViewById(R.id.public_btn);
                publicBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new CreateList(name.getText().toString(), true, description.getText().toString()).execute();
                        dialog.dismiss();
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
}
