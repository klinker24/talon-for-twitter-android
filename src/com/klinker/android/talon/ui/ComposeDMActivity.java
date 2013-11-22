package com.klinker.android.talon.ui;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.klinker.android.talon.R;
import com.klinker.android.talon.utilities.Utils;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class ComposeDMActivity extends ComposeActivity {

    private EditText contactEntry;


    public void setUpLayout() {
        setContentView(R.layout.compose_activity);

        LinearLayout buttons = (LinearLayout) findViewById(R.id.buttons);
        buttons.setVisibility(View.INVISIBLE);

        contactEntry = (EditText) findViewById(R.id.contact_entry);
        contactEntry.setVisibility(View.VISIBLE);

        String screenname = getIntent().getStringExtra("screenname");

        if (screenname != null) {
            contactEntry.setText(screenname);
            contactEntry.setSelection(contactEntry.getText().toString().length());
        }
    }

    public boolean doneClick() {
        EditText editText = (EditText) findViewById(R.id.tweet_content);
        String status = editText.getText().toString();

        // Check for blank text
        if (status.trim().length() > 0) {
            // update status
            sendStatus(status);
        } else {
            // EditText is empty
            Toast.makeText(getApplicationContext(),
                    "Please enter status message", Toast.LENGTH_SHORT)
                    .show();
        }
        return true;
    }

    private void sendStatus(String status) {
        new SendDirectMessage().execute(status);
    }

    private class SendDirectMessage extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected Boolean doInBackground(String... args) {
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext());

                Log.v("screen_name", contactEntry.getText().toString());

                twitter.sendDirectMessage(contactEntry.getText().toString(), status);

                return true;

            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return false;
        }

        protected void onPostExecute(Boolean sent) {
            // dismiss the dialog after getting all products

            if (sent) {
            // updating ui from Background Thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(),
                                "Direct Message Sent", Toast.LENGTH_SHORT)
                                .show();
                        // Clearing EditText field
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(),
                                getResources().getString(R.string.error), Toast.LENGTH_SHORT)
                                .show();
                        // Clearing EditText field
                    }
                });
            }
        }

    }
}
