package com.klinker.android.twitter.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ArrayListLoader;
import com.klinker.android.twitter.adapters.CursorListLoader;
import com.klinker.android.twitter.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.DirectMessage;
import com.klinker.android.twitter.data.sq_lite.DMDataSource;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.HoloTextView;
import com.klinker.android.twitter.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.bitmapcache.BitmapLruCache;


public class DirectMessageConversation extends Activity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private ActionBar actionBar;

    private AsyncListView listView;
    private HoloEditText composeBar;
    private ImageButton sendButton;
    private HoloTextView charRemaining;

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

        Utils.setUpPopupTheme(this, settings);

        actionBar = getActionBar();
        actionBar.setTitle(getResources().getString(R.string.lists));

        setContentView(R.layout.dm_conversation);

        if (!settings.isTwitterLoggedIn) {
            Intent login = new Intent(context, LoginActivity.class);
            startActivity(login);
            finish();
        }

        listView = (AsyncListView) findViewById(R.id.listView);
        sendButton = (ImageButton) findViewById(R.id.send_button);
        composeBar = (HoloEditText) findViewById(R.id.tweet_content);
        charRemaining = (HoloTextView) findViewById(R.id.char_remaining);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        CursorListLoader loader = new CursorListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(50);
        builder.setThreadPoolSize(4);

        listView.setItemManager(builder.build());

        listName = getIntent().getStringExtra("screenname");

        actionBar.setTitle(getIntent().getStringExtra("name"));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        new GetList().execute();

        charRemaining.setText(140 - composeBar.getText().length() + "");
        composeBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                try {
                    charRemaining.setText(140 - composeBar.getText().length() + "");
                } catch (Exception e) {
                    charRemaining.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = composeBar.getText().toString();
                if (status.trim().length() > 0 && status.length() <= 140) {
                    new SendDirectMessage().execute(status);
                    composeBar.setText("");
                }
            }
        });
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

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    class GetList extends AsyncTask<String, Void, Cursor> {

        protected Cursor doInBackground(String... urls) {
            try {
                DMDataSource data = new DMDataSource(context);
                data.open();
                Cursor cursor = data.getConvCursor(listName, settings.currentAccount);
                return cursor;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Cursor cursor) {

            if (cursor != null) {
                listView.setAdapter(new TimeLineCursorAdapter(context, cursor, true));
                listView.setVisibility(View.VISIBLE);
                listView.setStackFromBottom(true);
            }

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

    class SendDirectMessage extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected Boolean doInBackground(String... args) {
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext(), settings);

                String sendTo = listName;

                twitter4j.DirectMessage message = twitter.sendDirectMessage(sendTo, status);

                DMDataSource data = new DMDataSource(context);
                data.open();
                data.createDirectMessage(message, settings.currentAccount);
                data.close();

                sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), message.getId()).commit();
                sharedPrefs.edit().putBoolean("refresh_me_dm", true).commit();

                return true;

            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return false;
        }

        protected void onPostExecute(Boolean sent) {
            // dismiss the dialog after getting all products

            if (sent) {
                Toast.makeText(getBaseContext(),
                        getApplicationContext().getResources().getString(R.string.direct_message_sent),
                        Toast.LENGTH_SHORT)
                        .show();
            } else {
                Toast.makeText(getBaseContext(),
                        getResources().getString(R.string.error),
                        Toast.LENGTH_SHORT)
                        .show();
            }

            context.sendBroadcast(new Intent("com.klinker.android.twitter.UPDATE_DM"));
        }

    }

    public BroadcastReceiver updateConv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new GetList().execute();
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.klinker.android.twitter.UPDATE_DM");
        context.registerReceiver(updateConv, filter);
    }

    @Override
    public void onPause() {
        try {
            context.unregisterReceiver(updateConv);
        } catch (Exception e) {

        }

        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

}