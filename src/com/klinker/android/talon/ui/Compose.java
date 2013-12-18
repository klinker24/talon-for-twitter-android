package com.klinker.android.talon.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.klinker.android.talon.R;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.widgets.EmojiKeyboard;
import com.klinker.android.talon.ui.widgets.HoloEditText;
import com.klinker.android.talon.utils.IOUtils;
import com.klinker.android.talon.utils.Utils;

import java.io.File;
import java.io.IOException;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.photoview.PhotoViewAttacher;

public abstract class Compose extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    public LocationClient mLocationClient;
    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

    public EditText contactEntry;
    public EditText reply;
    public ImageView attachImage;
    public ImageButton attachButton;
    public ImageButton emojiButton;
    public EmojiKeyboard emojiKeyboard;
    public ImageButton overflow;
    public TextView charRemaining;

    public String attachedFilePath = "";

    public PhotoViewAttacher mAttacher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = new AppSettings(this);
        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mLocationClient = new LocationClient(context, this, this);
        mLocationClient.connect();

        setUpTheme();
        setUpWindow();
        setUpLayout();
        setUpDoneDiscard();

        String to = getIntent().getStringExtra("user") + " ";

        if (!to.equals("null ")) {
            EditText editText = (EditText) findViewById(R.id.tweet_content);
            editText.setText(to);
            editText.setSelection(editText.getText().toString().length());
        }

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }
        }

    }

    public void setUpWindow() {
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
            getWindow().setLayout((int) (width * .9), (int) (height * .8));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
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

    public void setUpDoneDiscard() {
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
                        discardClicked = true;
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
    }

    public void setUpSimilar() {
        attachImage = (ImageView) findViewById(R.id.picture);
        attachButton = (ImageButton) findViewById(R.id.attach);
        emojiButton = (ImageButton) findViewById(R.id.emoji);
        emojiKeyboard = (EmojiKeyboard) findViewById(R.id.emojiKeyboard);
        reply = (EditText) findViewById(R.id.tweet_content);
        charRemaining = (TextView) findViewById(R.id.char_remaining);

        if (!settings.useEmoji) {
            emojiButton.setVisibility(View.GONE);
        } else {
            emojiKeyboard.setAttached((HoloEditText) reply);

            reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    }
                }
            });

            emojiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager imm = (InputMethodManager)getSystemService(
                                        Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(reply, 0);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    } else {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                emojiKeyboard.setVisibility(true);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.keyboardButton});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    }
                }
            });
        }

        charRemaining.setText(140 - reply.getText().length() + "");
        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                charRemaining.setText(140 - reply.getText().length() - (attachedFilePath.equals("") ? 0 : 22) + "");
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            reply.setText(sharedText);
            reply.setSelection(reply.getText().toString().length());
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            String filePath = IOUtils.getPath(imageUri, context);

            Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

            attachImage.setImageBitmap(yourSelectedImage);

            attachedFilePath = filePath;
        }
    }

    public boolean addLocation = false;

    public void makeFailedNotification(String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.timeline_dark)
                        .setContentTitle(getResources().getString(R.string.tweet_failed))
                        .setContentText(getResources().getString(R.string.tap_to_retry));

        Intent resultIntent = new Intent(this, RetryCompose.class);
        resultIntent.setAction(Intent.ACTION_SEND);
        resultIntent.setType("text/plain");
        resultIntent.putExtra(Intent.EXTRA_TEXT, text);
        resultIntent.putExtra("failed_notification", true);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        0
                );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(5, mBuilder.build());
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("location", "connected");
    }

    @Override
    public void onDisconnected() {
        //Toast.makeText(context, getResources().getString(R.string.location_disconnected), Toast.LENGTH_SHORT).show();
        Log.v("location", "disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
        Log.v("location", "failed");
    }

    @Override
    public void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    public static final int SELECT_PHOTO = 100;
    public static final int CAPTURE_IMAGE = 101;

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
                break;
            case CAPTURE_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg"));
                        String filePath = selectedImage.getPath();
                        //String filePath = IOUtils.getPath(selectedImage, context);
                        Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);

                        attachImage.setImageBitmap(yourSelectedImage);

                        attachedFilePath = filePath;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(this, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (emojiKeyboard.isShowing()) {
            emojiKeyboard.setVisibility(false);

            TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            emojiButton.setImageResource(resource);
            return;
        }

        super.onBackPressed();
    }

    public boolean doneClicked = false;
    public boolean discardClicked = false;

    class SendDirectMessage extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected Boolean doInBackground(String... args) {
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext());

                String sendTo = contactEntry.getText().toString().replace("@", "").replace(" ", "");

                twitter.sendDirectMessage(sendTo, status);

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
        }

    }

    class updateTwitterStatus extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        String text;

        public updateTwitterStatus(String text) {
            this.text = text;
        }

        protected Boolean doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext());

                StatusUpdate media = new StatusUpdate(status);

                if (attachedFilePath.equals("")) {
                    // Update status
                    if(addLocation) {
                        Location location = mLocationClient.getLastLocation();
                        GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());
                        media.setLocation(geolocation);
                    }

                    twitter.updateStatus(media);

                } else {
                    media.setMedia(new File(attachedFilePath));

                    if(addLocation) {
                        Location location = mLocationClient.getLastLocation();
                        GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());
                        media.setLocation(geolocation);
                    }

                    twitter.updateStatus(media);

                    return true;
                }

            } catch (TwitterException e) {
                // Error in updating status
            }
            return false;
        }

        protected void onPostExecute(Boolean success) {
            // dismiss the dialog after getting all products

            if (success) {
                Toast.makeText(getBaseContext(),
                        getResources().getString(R.string.tweet_success),
                        Toast.LENGTH_SHORT)
                        .show();
            } else {
                makeFailedNotification(text);
            }
        }

    }

    public abstract boolean doneClick();
    public abstract void setUpLayout();
}
