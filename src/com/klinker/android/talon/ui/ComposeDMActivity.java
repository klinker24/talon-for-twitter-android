package com.klinker.android.talon.ui;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.talon.R;
import com.klinker.android.talon.ui.widgets.QustomDialogBuilder;
import com.klinker.android.talon.utils.Utils;

import twitter4j.Twitter;
import twitter4j.TwitterException;

public class ComposeDMActivity extends Compose {

    public void setUpLayout() {
        setContentView(R.layout.compose_dm_activity);

        setUpSimilar();

        contactEntry = (EditText) findViewById(R.id.contact_entry);
        contactEntry.setVisibility(View.VISIBLE);

        String screenname = getIntent().getStringExtra("screenname");

        if (screenname != null) {
            contactEntry.setText("@" + screenname);
            contactEntry.setSelection(contactEntry.getText().toString().length());
        }

        Button at = (Button) findViewById(R.id.at_button);
        at.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final QustomDialogBuilder qustomDialogBuilder = new QustomDialogBuilder(context, sharedPrefs.getInt("current_account", 1)).
                        setTitle(getResources().getString(R.string.type_user)).
                        setTitleColor(getResources().getColor(R.color.app_color)).
                        setDividerColor(getResources().getColor(R.color.app_color));

                qustomDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                qustomDialogBuilder.setPositiveButton(getResources().getString(R.string.add_user), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        contactEntry.append(qustomDialogBuilder.text.getText().toString());
                    }
                });

                qustomDialogBuilder.show();

                overflow.performClick();
            }
        });

    }

    public boolean doneClick() {
        EditText editText = (EditText) findViewById(R.id.tweet_content);
        String status = editText.getText().toString();

        // Check for blank text
        if (status.trim().length() > 0 && status.length() < 140) {
            // update status
            sendStatus(status);
            return true;
        } else {
            if (editText.getText().length() < 140) {
                // EditText is empty
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_sending_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private void sendStatus(String status) {
        new SendDirectMessage().execute(status);
    }

}
