package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.utils.IOUtils;
import com.klinker.android.talon.utils.Utils;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import java.io.File;


public class ComposeActivity extends Activity {

    public AppSettings settings;
    private Context context;

    private EditText contactEntry;
    private ImageView attachImage;

    private String attachedFilePath = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new AppSettings(this);
        context = this;

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

        setUpLayout();

        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_done_discard, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean close = doneClick();
                        if (close) {
                            finish();
                        }
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

    private static final int SELECT_PHOTO = 100;

    public void setUpLayout() {
        setContentView(R.layout.compose_activity);

        contactEntry = (EditText) findViewById(R.id.contact_entry);
        attachImage = (ImageView) findViewById(R.id.attach);

        attachImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (attachedFilePath.equals("")) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                } else {
                    attachedFilePath = "";

                    TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.attachButton});
                    int resource = a.getResourceId(0, 0);
                    a.recycle();
                    attachImage.setImageDrawable(context.getResources().getDrawable(resource));
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                }
            }
        });

        final TextView charRemaining = (TextView) findViewById(R.id.char_remaining);
        final EditText reply = (EditText) findViewById(R.id.tweet_content);

        charRemaining.setText(140 - reply.getText().length() + "");
        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                charRemaining.setText(140 - reply.getText().length() + "");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
        if ((status.trim().length() > 0 || !attachedFilePath.equals("")) && editText.getText().length() < 140) {
            // update status
            sendStatus(status);
            return true;
        } else {
            if (editText.getText().length() < 140) {
                // EditText is empty
                Toast.makeText(context, context.getResources().getString(R.string.error_sending_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    private void sendStatus(String status) {
        new updateTwitterStatus().execute(status);
    }

    private class updateTwitterStatus extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext());

                if (attachedFilePath.equals("")) {
                    // Update status
                    twitter4j.Status response = twitter.updateStatus(status);

                } else {
                    Log.v("updating_with_pic", attachedFilePath);
                    StatusUpdate media = new StatusUpdate(status);
                    media.setMedia(new File(attachedFilePath));
                    twitter.updateStatus(media);
                }

            } catch (TwitterException e) {
                // Error in updating status
            }
            return null;
        }

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

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    String filePath = IOUtils.getPath(selectedImage, context);

                    Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                    attachImage.setImageBitmap(yourSelectedImage);

                    attachedFilePath = filePath;
                }
        }
    }
}