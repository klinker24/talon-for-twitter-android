package com.klinker.android.twitter.ui.compose;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
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
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.manipulations.EmojiKeyboard;
import com.klinker.android.twitter.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.api_helper.TwitLongerHelper;
import com.klinker.android.twitter.utils.Utils;
import com.klinker.android.twitter.utils.api_helper.TwitPicHelper;

import java.io.File;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public ListPopupWindow autocomplete;

    public LinearLayout selectAccounts;
    public CheckBox accountOneCheck;
    public CheckBox accountTwoCheck;
    public HoloTextView accountOneName;
    public HoloTextView accountTwoName;

    public String attachedUri = "";

    public PhotoViewAttacher mAttacher;

    public boolean isDM = false;

    public long notiId = 0;

    public int currentAccount;

    final Pattern p = Patterns.WEB_URL;

    public Handler countHandler;
    public Runnable getCount = new Runnable() {
        @Override
        public void run() {
            String text = reply.getText().toString();

            if (!Patterns.WEB_URL.matcher(text).find()) { // no links, normal tweet
                try {
                    charRemaining.setText(140 - reply.getText().length() - (attachedUri.equals("") ? 0 : 22) + "");
                } catch (Exception e) {
                    charRemaining.setText("0");
                }
            } else {
                int count = text.length();
                Matcher m = p.matcher(text);
                while(m.find()) {
                    String url = m.group();
                    count -= url.length(); // take out the length of the url
                    count += 22; // add 22 for the shortened url
                }

                if (!attachedUri.equals("")) {
                    count += 22;
                }

                charRemaining.setText(140 - count + "");
            }
        }
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_up, R.anim.activity_slide_down);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.activity_slide_up, R.anim.activity_slide_down);

        countHandler = new Handler();

        settings = AppSettings.getInstance(this);
        context = this;
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        currentAccount = sharedPrefs.getInt("current_account", 1);

        mLocationClient = new LocationClient(context, this, this);
        mLocationClient.connect();

        Utils.setUpPopupTheme(context, settings);
        setUpWindow();
        setUpLayout();
        setUpDoneDiscard();
        setUpReplyText();

        if (reply.getText().toString().contains(" RT @")) {
            reply.setSelection(0);
        }

        Utils.setActionBar(context);
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

    public void setUpDoneDiscard() {
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_send_discard, null);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Integer.parseInt(charRemaining.getText().toString()) < 0 && settings.twitlonger) {
                            new AlertDialog.Builder(context)
                                    .setTitle(context.getResources().getString(R.string.tweet_to_long))
                                    .setMessage(context.getResources().getString(R.string.select_shortening_service))
                                    .setPositiveButton(R.string.twitlonger, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            doneClick();
                                        }
                                    })
                                    .setNeutralButton(R.string.pwiccer, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            try {
                                                Intent pwiccer = new Intent("com.t3hh4xx0r.pwiccer.requestImagePost");
                                                pwiccer.putExtra("POST_CONTENT", reply.getText().toString());
                                                startActivityForResult(pwiccer, 420);
                                            } catch (Throwable e) {
                                                // open the play store here
                                                // they don't have pwiccer installed
                                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.t3hh4xx0r.pwiccer&hl=en")));
                                            }
                                        }
                                    })
                                    .setNegativeButton(R.string.edit, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    })
                                    .create()
                                    .show();
                        } else {
                            boolean close = doneClick();
                            if (close) {
                                onBackPressed();
                            }
                        }
                    }
                });
        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        discardClicked = true;
                        sharedPrefs.edit().putString("draft", "").commit();
                        if (emojiKeyboard.isShowing()) {
                            onBackPressed();
                        }
                        onBackPressed();
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

        charRemaining.setText(140 - reply.getText().length() + "");

        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                countHandler.removeCallbacks(getCount);
                countHandler.postDelayed(getCount, 300);
            }
        });

        if (settings.addonTheme) {
            try {
                Resources resourceAddon = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                int back = resourceAddon.getIdentifier("reply_entry_background", "drawable", settings.addonThemePackage);
                reply.setBackgroundDrawable(resourceAddon.getDrawable(back));
            } catch (Exception e) {
                // theme does not include a reply entry box
            }
        }
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (sharedText != null) {
            if (!isDM) {
                Log.v("username_for_noti", "shared text: " + sharedText);
                if (subject != null) {
                    reply.setText(subject + " - " + sharedText);
                } else {
                    reply.setText(sharedText);
                }
                reply.setSelection(reply.getText().toString().length());
            } else {
                contactEntry.setText(sharedText);
                reply.requestFocus();
            }
        }
    }



    void handleSendImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            //String filePath = IOUtils.getPath(imageUri, context);
            attachImage.setImageURI(imageUri);
            attachedUri = imageUri.toString();
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
    public static final int PWICCER = 420;

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        String filePath = IOUtils.getPath(selectedImage, context);

                        Log.v("talon_compose_pic", "path to image on sd card: " + filePath);

                        attachImage.setImageURI(selectedImage);
                        attachedUri = selectedImage.toString();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case CAPTURE_IMAGE:
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        Uri selectedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg"));

                        attachImage.setImageURI(selectedImage);
                        attachedUri = selectedImage.toString();

                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(this, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case PWICCER:
                if (resultCode == Activity.RESULT_OK) {
                    String path = imageReturnedIntent.getStringExtra("RESULT");
                    attachedUri = Uri.fromFile(new File(path)).toString();
                    attachImage.setImageURI(Uri.parse(attachedUri));

                    String currText = imageReturnedIntent.getStringExtra("RESULT_TEXT");
                    Log.v("pwiccer_text", currText);
                    if (currText != null) {
                        reply.setText(currText);
                    } else {
                        reply.setText(reply.getText().toString().substring(0, 114) + "...");
                    }

                    doneClick();
                    onBackPressed();
                } else {
                    Toast.makeText(context, "Pwiccer failed to generate image! Is it installed?", Toast.LENGTH_SHORT).show();
                }
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
                Twitter twitter = Utils.getTwitter(getApplicationContext(), settings);

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

            context.sendBroadcast(new Intent("com.klinker.android.twitter.UPDATE_DM"));
        }

    }

    class updateTwitterStatus extends AsyncTask<String, String, Boolean> {

        String text;
        private int remaining;
        private InputStream stream;

        public updateTwitterStatus(String text, int length) {
            this.text = text;
            this.remaining = length;
        }

        protected Boolean doInBackground(String... args) {
            String status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext(), settings);
                Twitter twitter2 = Utils.getSecondTwitter(getApplicationContext());

                if (remaining < 0) {
                    // twitlonger goes here
                    boolean isDone = false;

                    if (accountOneCheck.isChecked()) {
                        TwitLongerHelper helper = new TwitLongerHelper(text, twitter);

                        if (notiId != 0) {
                            helper.setInReplyToStatusId(notiId);
                        }

                        if (addLocation) {
                            int wait = 0;
                            while (!mLocationClient.isConnected() && wait < 4) {
                                try {
                                    Thread.sleep(1500);
                                } catch (Exception e) {
                                    return false;
                                }

                                wait++;
                            }

                            if (wait == 4) {
                                return false;
                            }

                            Location location = mLocationClient.getLastLocation();
                            GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());

                            helper.setLocation(geolocation);
                        }

                        if (helper.createPost() != 0) {
                            isDone = true;
                        }
                    }

                    if (accountTwoCheck.isChecked()) {
                        TwitLongerHelper helper = new TwitLongerHelper(text, twitter2);

                        if (notiId != 0) {
                            helper.setInReplyToStatusId(notiId);
                        }

                        if (addLocation) {
                            int wait = 0;
                            while (!mLocationClient.isConnected() && wait < 4) {
                                try {
                                    Thread.sleep(1500);
                                } catch (Exception e) {
                                    return false;
                                }

                                wait++;
                            }

                            if (wait == 4) {
                                return false;
                            }

                            Location location = mLocationClient.getLastLocation();
                            GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());

                            helper.setLocation(geolocation);
                        }

                        if (helper.createPost() != 0) {
                            isDone = true;
                        }
                    }

                    return isDone;
                } else {
                    StatusUpdate media = new StatusUpdate(status);

                    if (notiId != 0) {
                        media.setInReplyToStatusId(notiId);
                    }

                    if (attachedUri.equals("")) {
                        // Update status
                        if(addLocation) {
                            Location location = mLocationClient.getLastLocation();
                            GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());
                            media.setLocation(geolocation);
                        }

                        if (accountOneCheck.isChecked()) {
                            twitter.updateStatus(media);
                        }
                        if (accountTwoCheck.isChecked()) {
                            twitter2.updateStatus(media);
                        }

                        return true;

                    } else {
                        stream = getContentResolver().openInputStream(Uri.parse(attachedUri));

                        if (settings.twitpic) {
                            boolean isDone = false;
                            if (accountOneCheck.isChecked()) {
                                TwitPicHelper helper = new TwitPicHelper(twitter, text, stream, context);
                                if (addLocation) {
                                    int wait = 0;
                                    while (!mLocationClient.isConnected() && wait < 4) {
                                        try {
                                            Thread.sleep(1500);
                                        } catch (Exception e) {
                                            return false;
                                        }

                                        wait++;
                                    }

                                    if (wait == 4) {
                                        return false;
                                    }

                                    Location location = mLocationClient.getLastLocation();
                                    GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());

                                    helper.setLocation(geolocation);
                                }
                                if (helper.createPost() != 0) {
                                    isDone = true;
                                }
                            }
                            if (accountTwoCheck.isChecked()) {
                                TwitPicHelper helper = new TwitPicHelper(twitter2, text, stream, context);
                                if (addLocation) {
                                    int wait = 0;
                                    while (!mLocationClient.isConnected() && wait < 4) {
                                        try {
                                            Thread.sleep(1500);
                                        } catch (Exception e) {
                                            return false;
                                        }

                                        wait++;
                                    }

                                    if (wait == 4) {
                                        return false;
                                    }

                                    Location location = mLocationClient.getLastLocation();
                                    GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());

                                    helper.setLocation(geolocation);
                                }
                                if (helper.createPost() != 0) {
                                    isDone = true;
                                }
                            }
                            return isDone;
                        } else {
                            //media.setMedia(f);
                            try {
                                media.setMedia("Pic from Talon", stream);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if(addLocation) {
                                int wait = 0;
                                while (!mLocationClient.isConnected() && wait < 4) {
                                    try {
                                        Thread.sleep(1500);
                                    } catch (Exception e) {
                                        return false;
                                    }

                                    wait++;
                                }

                                if (wait == 4) {
                                    return false;
                                }

                                Location location = mLocationClient.getLastLocation();
                                GeoLocation geolocation = new GeoLocation(location.getLatitude(),location.getLongitude());
                                media.setLocation(geolocation);
                            }

                            if (accountOneCheck.isChecked()) {
                                twitter.updateStatus(media);
                            }
                            if (accountTwoCheck.isChecked()) {
                                twitter2.updateStatus(media);
                            }

                            return true;
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        protected void onPostExecute(Boolean success) {
            // dismiss the dialog after getting all products
            try {
                stream.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }

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

    public Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public abstract boolean doneClick();
    public abstract void setUpLayout();
    public abstract void setUpReplyText();

    public int toDP(int px) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
        } catch (Exception e) {
            return px;
        }
    }
}
