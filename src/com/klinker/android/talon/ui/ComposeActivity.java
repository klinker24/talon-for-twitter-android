package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.Toast;
import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.utilities.Utils;
import twitter4j.Twitter;
import twitter4j.TwitterException;


public class ComposeActivity extends Activity {

    public AppSettings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new AppSettings(this);

        setUpTheme();

        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .6f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .7));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

        setContentView(R.layout.compose_activity);

        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_done_discard, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doneClick();
                        finish();
                    }
                });
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                        | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        String to = getIntent().getStringExtra("user") + " ";

        if (!to.equals("null ")) {
            EditText editText = (EditText) findViewById(R.id.tweet_content);
            editText.setText(to);
            editText.setSelection(editText.getText().toString().length());
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

    public boolean doneClick() {
        EditText editText = (EditText) findViewById(R.id.tweet_content);
        String status = editText.getText().toString();

        // Check for blank text
        if (status.trim().length() > 0) {
            // update status
            new updateTwitterStatus().execute(status);
        } else {
            // EditText is empty
            Toast.makeText(getApplicationContext(),
                    "Please enter status message", Toast.LENGTH_SHORT)
                    .show();
        }
        return true;
    }

    /**
     * Function to update status
     */
    class updateTwitterStatus extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        /**
         * getting Places JSON
         */
        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext());

                // Update status
                twitter4j.Status response = twitter.updateStatus(status);

                Log.d("Status", "> " + response.getText());
            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in ui Always use runOnUiThread(new Runnable()) to update ui
         * from background thread, otherwise you will get error
         * *
         */
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products

            // updating ui from Background Thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(),
                            "Status tweeted successfully", Toast.LENGTH_SHORT)
                            .show();
                    // Clearing EditText field
                }
            });
        }

    }
}