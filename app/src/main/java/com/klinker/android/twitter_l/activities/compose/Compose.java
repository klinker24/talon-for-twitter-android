package com.klinker.android.twitter_l.activities.compose;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.views.widgets.FontPrefTextView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.views.widgets.EmojiKeyboard;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.api_helper.GiphyHelper;
import com.klinker.android.twitter_l.utils.api_helper.TwitLongerHelper;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitPicHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.yalantis.ucrop.UCrop;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.*;
import uk.co.senab.photoview.PhotoViewAttacher;

public abstract class Compose extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final boolean DEBUG = false;

    public GoogleApiClient mGoogleApiClient;
    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

    public EditText contactEntry;
    public EditText reply;
    public ImageView[] attachImage = new ImageView[4];
    public ImageButton[] cancelButton = new ImageButton[4];
    public FrameLayout[] holders = new FrameLayout[4];
    public ImageButton gifButton;
    public ImageButton attachButton;
    public ImageButton emojiButton;
    public EmojiKeyboard emojiKeyboard;
    public ImageButton overflow;
    public TextView charRemaining;
    public ListPopupWindow userAutoComplete;
    public ListPopupWindow hashtagAutoComplete;
    public FontPrefTextView numberAttached;

    protected boolean useAccOne = true;
    protected boolean useAccTwo = false;

    protected boolean sharingSomething = false;
    protected String attachmentUrl = null; // quoted tweet

    // attach up to four images
    public String[] attachedUri = new String[] {"","","",""};
    public int imagesAttached = 0;

    public PhotoViewAttacher mAttacher;

    public boolean isDM = false;

    public String to = null;
    public long notiId = 0;
    public String replyText = "";

    public int currentAccount;

    final Pattern p = Patterns.WEB_URL;

    public Handler countHandler;
    public Runnable getCount = new Runnable() {
        @Override
        public void run() {
            String text = reply.getText().toString();

            if (shouldReplaceTo(text)) {
                String replaceable = to.replaceAll("#[a-zA-Z]+ ", "");
                text = text.replaceAll(replaceable, "");
            }

            if (!Patterns.WEB_URL.matcher(text).find()) { // no links, normal tweet
                try {
                    charRemaining.setText(140 - text.length() + "");
                } catch (Exception e) {
                    charRemaining.setText("0");
                }
            } else {
                int count = text.length();
                Matcher m = p.matcher(text);
                while (m.find()) {
                    String url = m.group();
                    count -= url.length(); // take out the length of the url
                    count += 23; // add 23 for the shortened url
                }

                charRemaining.setText(140 - count + "");
            }

            changeTextColor();
        }

        private int originalTextColor = -1;
        private void changeTextColor() {
            if (originalTextColor == -1) {
                originalTextColor = charRemaining.getCurrentTextColor();
            }

            try {
                if (Integer.parseInt(charRemaining.getText().toString()) <= 10) {
                    charRemaining.setTextColor(getResources().getColor(R.color.red_primary_color_light));
                } else {
                    charRemaining.setTextColor(originalTextColor);
                }
            } catch (Exception e) {
                charRemaining.setTextColor(originalTextColor);
            }
        }
    };

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setTaskDescription(this);

        if (!getIntent().getBooleanExtra("already_animated", false)) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        countHandler = new Handler();

        settings = AppSettings.getInstance(this);
        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(context);


        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

        currentAccount = sharedPrefs.getInt("current_account", 1);

        buildGoogleApiClient();

        Utils.setUpTheme(context, settings);
        setUpWindow();
        setUpLayout();
        setUpActionBar();
        setUpReplyText();

        reply.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.isCtrlPressed()) {
                    
                    findViewById(R.id.send_button).performClick();
                    return true;
                } else {
                    return false;
                }
            }
        });

        if (reply.getText().toString().contains(" RT @")) {
            reply.setSelection(0);
        }

        if (getIntent().getBooleanExtra("start_attach", false)) {
            attachButton.performClick();
            //overflow.performClick();
        }

        if (notiId != 0) {
            FontPrefTextView replyTo = (FontPrefTextView) findViewById(R.id.reply_to);
            if (reply.getText().toString().contains("/status/")) {
                //reply.setText("");
                replyTo.setText(replyText + "\n\n" + getString(R.string.quote_disclaimer));
            } else {
                replyTo.setText(replyText);
            }
            TextUtils.linkifyText(context, replyTo, null, true, "", true);
            replyTo.setVisibility(View.VISIBLE);

            replyTo.setTextSize(settings.textSize);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String text = reply.getText().toString();

                try {
                    if (!android.text.TextUtils.isEmpty(text) && !(text.startsWith(" RT @") || text.contains("/status/"))) {
                        text = text.replaceAll("  ", " ");
                        reply.setText(text);
                        reply.setSelection(text.length());

                        if (!text.endsWith(" ")) {
                            reply.append(" ");
                        }
                    }
                } catch (Exception e) {

                }

                replyText = reply.getText().toString();
            }
        }, 250);

        if (contactEntry != null) {
            contactEntry.setTextSize(settings.textSize);
        }

        if (reply != null) {
            reply.setTextSize(settings.textSize);
        }

        if (this instanceof ComposeDMActivity) {
            attachButton.setVisibility(View.GONE);
        }

        // change the background color for the cursor
        if (settings.darkTheme && settings.theme == AppSettings.THEME_BLACK) {
            try {
                // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
                Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
                f.setAccessible(true);
                f.set(reply, R.drawable.black_cursor);
            } catch (Exception ignored) {
            }

            try {
                // https://github.com/android/platform_frameworks_base/blob/kitkat-release/core/java/android/widget/TextView.java#L562-564
                Field f = TextView.class.getDeclaredField("mHighlightColor");
                f.setAccessible(true);
                f.set(reply, context.getResources().getColor(R.color.pressed_white));
            } catch (Exception ignored) {
            }
        }
    }

    public void setUpWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = .6f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);
        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .9));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }
    }

    public void setUpActionBar() {
        findViewById(R.id.send_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (Compose.this instanceof ComposeDMActivity) {
                            boolean close = doneClick();
                            if (close) {
                                onBackPressed();
                            }

                            return;
                        }
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
                                    /*.setNegativeButton(R.string.edit, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    })*/
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
        View discard = findViewById(R.id.discard_button);
        discard.setVisibility(View.GONE);
        discard.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        discardClicked = true;
                        sharedPrefs.edit().putString("draft", "").apply();
                        if (emojiKeyboard.isShowing()) {
                            onBackPressed();
                        }
                        onBackPressed();
                    }
                });

    }

    public void setUpSimilar() {
        attachImage[0] = (ImageView) findViewById(R.id.picture1);
        attachImage[1] = (ImageView) findViewById(R.id.picture2);
        attachImage[2] = (ImageView) findViewById(R.id.picture3);
        attachImage[3] = (ImageView) findViewById(R.id.picture4);

        attachButton = (ImageButton) findViewById(R.id.attach);
        gifButton = (ImageButton) findViewById(R.id.gif);
        emojiButton = (ImageButton) findViewById(R.id.emoji);
        emojiKeyboard = (EmojiKeyboard) findViewById(R.id.emojiKeyboard);
        reply = (EditText) findViewById(R.id.tweet_content);
        charRemaining = (TextView) findViewById(R.id.char_remaining);

        findViewById(R.id.prompt_pos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("talon_input", "clicked the view");
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(reply, InputMethodManager.SHOW_FORCED);
            }
        });

        ImageView pic = (ImageView) findViewById(R.id.profile_pic);
        FontPrefTextView currentName = (FontPrefTextView) findViewById(R.id.current_name);

        if (!(this instanceof ComposeSecAccActivity))
            Glide.with(this).load(settings.myProfilePicUrl).into(pic);

        currentName.setText("@" + settings.myScreenName);

        //numberAttached.setText("0 " + getString(R.string.attached_images));

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
    }

    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (sharedText != null) {
            if (!isDM) {
                if (subject != null && !subject.equals(sharedText) && !sharedText.contains(subject)) {
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

    private Bitmap getThumbnail(Uri uri) throws FileNotFoundException, IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        int reqWidth = 150;
        int reqHeight = 150;

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = input.read(buffer)) > -1) {
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

            Bitmap b = BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            if (!isAndroidN()) {
                ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                input.close();

                b = ImageUtils.cropSquare(b);
                return rotateBitmap(b, orientation);
            } else {
                input.close();
                b = ImageUtils.cropSquare(b);
                return b;
            }

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public int getOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
                null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    public Bitmap getBitmapToSend(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        int reqWidth = 1500;
        int reqHeight = 1500;

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = input.read(buffer)) > -1) {
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

            Bitmap b = BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            if (!isAndroidN()) {
                ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                input.close();

                return rotateBitmap(b, orientation);
            } else {
                input.close();
                return b;
            }

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Log.v("talon_composing_image", "rotation: " + orientation);

        /*if (Build.MANUFACTURER.toLowerCase().contains("samsung") && Build.MODEL.toLowerCase().contains("s6")) {
            Log.v("talon_composing_image", "S6 varient");
            return bitmap;
        }*/

        try{
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return bitmap;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.setScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setRotate(180);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.setRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setRotate(270);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setRotate(270);
                    break;
                default:
                    return bitmap;
            }
            try {
                Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return bmRotated;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            //String filePath = IOUtils.getPath(imageUri, context);
            try {
                attachImage[imagesAttached].setImageURI(imageUri);
                attachedUri[imagesAttached] = imageUri.toString();
                holders[imagesAttached].setVisibility(View.VISIBLE);
                imagesAttached++;
                //numberAttached.setText(imagesAttached + " " + getResources().getString(R.string.attached_images));
                //numberAttached.setVisibility(View.VISIBLE);
            } catch (Throwable e) {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                //numberAttached.setText("");
                //numberAttached.setVisibility(View.GONE);
            }
        }
    }

    public boolean addLocation = false;

    public void displayErrorNotification(final Exception e) {

        if (!DEBUG) {
            return;
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.tweet_failed))
                        .setContentText(e.getMessage());

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(221, mBuilder.build());
    }

    public void makeFailedNotification(String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.tweet_failed))
                        .setContentText(notiId != 0 ? getResources().getString(R.string.original_probably_deleted) : getResources().getString(R.string.tap_to_retry));

        Intent resultIntent = new Intent(this, RetryCompose.class);
        QueuedDataSource.getInstance(this).createDraft(text, settings.currentAccount);
        resultIntent.setAction(Intent.ACTION_SEND);
        resultIntent.putExtra("failed_notification_text", text);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        NotificationUtils.generateRandomId(),
                        resultIntent,
                        0
                );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(5, mBuilder.build());
    }

    public void makeTweetingNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setOngoing(true)
                        .setProgress(100, 0, true);

        if (Compose.this instanceof ComposeDMActivity) {
            mBuilder.setContentTitle(getResources().getString(R.string.sending_direct_message));
        } else {
            mBuilder.setContentTitle(getResources().getString(R.string.sending_tweet));
        }

        Intent resultIntent = new Intent(this, MainActivity.class);

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
        mNotificationManager.notify(6, mBuilder.build());
    }

    public void finishedTweetingNotification() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setContentTitle(getResources().getString(R.string.tweet_success))
                                    .setOngoing(false)
                                    .setTicker(getResources().getString(R.string.tweet_success));

                    if (settings.vibrate) {
                        Log.v("talon_vibrate", "vibrate on compose");
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        long[] pattern = { 0, 50, 500 };
                        v.vibrate(pattern, -1);
                    }

                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());
                    // cancel it immediately, the ticker will just go off
                    mNotificationManager.cancel(6);
                } catch (Exception e) {
                    // not attached?
                }
            }
        }, 500);

    }

    Location mLastLocation;

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("location", "connected");
        try {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        } catch (Exception e) {

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
        Log.v("location", "failed");
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void startUcrop(Uri sourceUri) {
        try {
            UCrop.Options options = new UCrop.Options();
            options.setToolbarColor(settings.themeColors.primaryColor);
            options.setStatusBarColor(settings.themeColors.primaryColorDark);
            options.setActiveWidgetColor(settings.themeColors.accentColor);
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(100);

            File destination = File.createTempFile("ucrop", "jpg", getCacheDir());
            UCrop.of(sourceUri, Uri.fromFile(destination))
                    .withOptions(options)
                    .start(Compose.this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final int SELECT_PHOTO = 100;
    public static final int CAPTURE_IMAGE = 101;
    public static final int SELECT_GIF = 102;
    public static final int SELECT_VIDEO = 103;
    public static final int FIND_GIF = 104;
    public static final int PWICCER = 420;

    public boolean pwiccer = false;

    public String attachmentType = "";

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        Log.v("talon_image_attach", "got the result, code: " + requestCode);
        switch(requestCode) {
            case UCrop.REQUEST_CROP:
                if(resultCode == RESULT_OK){
                    try {
                        Uri selectedImage = UCrop.getOutput(imageReturnedIntent);

                        String filePath = IOUtils.getPath(selectedImage, context);
                        Log.v("talon_compose_pic", "path to gif on sd card: " + filePath);

                        try {
                            attachImage[imagesAttached].setImageBitmap(getThumbnail(selectedImage));
                            holders[imagesAttached].setVisibility(View.VISIBLE);
                            attachedUri[imagesAttached] = selectedImage.toString();
                            imagesAttached++;
                        } catch (Throwable e) {
                            Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                } else if (resultCode == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(imageReturnedIntent);
                    cropError.printStackTrace();
                }
                countHandler.post(getCount);
                break;
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK) {
                    startUcrop(imageReturnedIntent.getData());
                }

                break;
            case CAPTURE_IMAGE:
                if(resultCode == RESULT_OK) {
                    Uri selectedImage = Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg"));
                    startUcrop(selectedImage);
                }

                break;
            case PWICCER:
                if (resultCode == Activity.RESULT_OK) {
                    String path = imageReturnedIntent.getStringExtra("RESULT");
                    attachedUri[imagesAttached] = Uri.fromFile(new File(path)).toString();

                    try {
                        attachImage[imagesAttached].setImageURI(Uri.parse(attachedUri[imagesAttached]));
                        holders[imagesAttached].setVisibility(View.VISIBLE);
                        imagesAttached++;
                        //numberAttached.setText(imagesAttached + " " + getResources().getString(R.string.attached_images));
                        //numberAttached.setVisibility(View.VISIBLE);
                    } catch (Throwable e) {
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
                        //numberAttached.setText("");
                        //numberAttached.setVisibility(View.GONE);
                    }


                    String currText = imageReturnedIntent.getStringExtra("RESULT_TEXT");
                    Log.v("pwiccer_text", currText);
                    Log.v("pwiccer_text", "length: " + currText.length());
                    if (currText != null) {
                        reply.setText(currText);
                    } else {
                        reply.setText(reply.getText().toString().substring(0, 114) + "...");
                    }

                    pwiccer = true;

                    doneClick();
                    onBackPressed();
                } else {
                    Toast.makeText(context, "Pwiccer failed to generate image! Is it installed?", Toast.LENGTH_SHORT).show();
                }
                countHandler.post(getCount);
                break;
            case FIND_GIF:
            case SELECT_GIF:
                if(resultCode == RESULT_OK){
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        String filePath = IOUtils.getPath(selectedImage, context);

                        Log.v("talon_compose_pic", "path to gif on sd card: " + filePath);

                        attachImage[0].setImageBitmap(getThumbnail(selectedImage));
                        holders[0].setVisibility(View.VISIBLE);
                        attachedUri[0] = selectedImage.toString();
                        imagesAttached = 1;

                        attachmentType = "animated_gif";

                        attachButton.setEnabled(false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
                countHandler.post(getCount);
                break;
            case SELECT_VIDEO:
                if(resultCode == RESULT_OK){
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        // todo: on N, the file path doesn't work
                        if (!isAndroidN()) {
                            String filePath = IOUtils.getPath(selectedImage, context);

                            Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(filePath,
                                    MediaStore.Images.Thumbnails.MINI_KIND);

                            Log.v("talon_compose_pic", "path to surfaceView on sd card: " + filePath);

                            attachImage[0].setImageBitmap(thumbnail);
                        }

                        holders[0].setVisibility(View.VISIBLE);
                        attachedUri[0] = selectedImage.toString();
                        imagesAttached = 1;

                        attachmentType = "surfaceView/mp4";

                        attachButton.setEnabled(false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
                countHandler.post(getCount);
                break;
        }

        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
    }

    @Override
    public void onBackPressed() {
        if (emojiKeyboard.isShowing()) {
            emojiKeyboard.setVisibility(false);

            TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button_changing});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            emojiButton.setImageResource(resource);
            return;
        }

        super.onBackPressed();
    }

    private boolean shouldReplaceTo(String tweetText) {
        return tweetText != null && to != null && !to.contains("/status/") &&
                notiId != 0 && !sharingSomething &&
                tweetText.contains(to) &&  tweetText.indexOf(".") != 0 &&
                    !tweetText.contains("@" + AppSettings.getInstance(this).myScreenName) &&
                !replyText.contains("@" + AppSettings.getInstance(this).myScreenName + ": ");
    }

    public boolean doneClicked = false;
    public boolean discardClicked = false;

    class SendDirectMessage extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    makeTweetingNotification();
                }
            }, 200);
            super.onPreExecute();

        }

        protected Boolean doInBackground(String... args) {
            String status = args[0];

            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext(), settings);

                if (!attachedUri.equals("")) {
                    try {
                        for (int i = 0; i < imagesAttached; i++) {
                            File outputDir = context.getCacheDir();
                            File f = File.createTempFile("compose", "picture_" + i, outputDir);

                            Bitmap bitmap = getBitmapToSend(Uri.parse(attachedUri[i]));
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            byte[] bitmapdata = bos.toByteArray();

                            FileOutputStream fos = new FileOutputStream(f);
                            fos.write(bitmapdata);
                            fos.flush();
                            fos.close();

                            // we wont attach any text to this image at least, since it is a direct message
                            TwitPicHelper helper = new TwitPicHelper(twitter, " ", f, context);
                            String url = helper.uploadForUrl();

                            status += " " + url;
                        }
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, getString(R.string.error_attaching_image), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                }

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
                finishedTweetingNotification();
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
        String status;
        private boolean secondTry;
        private int remaining;
        private InputStream stream;

        public updateTwitterStatus(String text, int length) {
            this.text = text;
            this.remaining = length;
            this.secondTry = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    makeTweetingNotification();
                }
            }, 50);
        }

        public updateTwitterStatus(String text, int length, boolean secondTry) {
            this.text = text;
            this.remaining = length;
            this.secondTry = secondTry;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    makeTweetingNotification();
                }
            }, 50);
        }

        protected Boolean doInBackground(String... args) {
            status = args[0];
            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext(), settings);
                Twitter twitter2 = Utils.getSecondTwitter(getApplicationContext());

                if (remaining < 0 && !pwiccer) {
                    // twitlonger goes here

                    boolean isDone = false;

                    if (useAccOne) {
                        TwitLongerHelper helper = new TwitLongerHelper(text, twitter, context);

                        if (notiId != 0) {
                            helper.setInReplyToStatusId(notiId);
                        }

                        if (addLocation) {
                            if (waitForLocation()) {
                                Location location = mLastLocation;
                                GeoLocation geolocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                                helper.setLocation(geolocation);
                            }
                        }

                        if (helper.createPost() != 0) {
                            isDone = true;
                        }
                    }

                    if (useAccTwo) {
                        TwitLongerHelper helper = new TwitLongerHelper(text, twitter2, context);

                        if (notiId != 0) {
                            helper.setInReplyToStatusId(notiId);
                        }

                        if (addLocation) {
                            waitForLocation();

                            if (waitForLocation()) {
                                Location location = mLastLocation;
                                GeoLocation geolocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                                helper.setLocation(geolocation);
                            }
                        }

                        if (helper.createPost() != 0) {
                            isDone = true;
                        }
                    }

                    return isDone;
                } else {
                    boolean autoPopulateMetadata = false;
                    if (shouldReplaceTo(text)) {
                        String replaceable = to.replaceAll("#[a-zA-Z]+ ", "");
                        if (!replaceable.equals(" ")) {
                            status = status.replaceAll(replaceable, "");
                            autoPopulateMetadata = true;
                        }
                    }

                    StatusUpdate media = new StatusUpdate(status);
                    StatusUpdate media2 = new StatusUpdate(status);

                    if (autoPopulateMetadata) {
                        media.setAutoPopulateReplyMetadata(autoPopulateMetadata);
                        media2.setAutoPopulateReplyMetadata(autoPopulateMetadata);
                    }

                    /*if (attachmentUrl != null) {
                        media.attachmentUrl(attachmentUrl);
                        media2.attachmentUrl(attachmentUrl);
                    }*/

                    if (notiId != 0) {
                        media.setInReplyToStatusId(notiId);
                        media2.setInReplyToStatusId(notiId);
                    }

                    if (imagesAttached == 0) {
                        // Update status
                        if(addLocation) {
                            if (waitForLocation()) {
                                Location location = mLastLocation;
                                GeoLocation geolocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                                media.setLocation(geolocation);
                                media2.setLocation(geolocation);
                            }
                        }

                        if (useAccOne) {
                            twitter.updateStatus(media);
                        }
                        if (useAccTwo) {
                            twitter2.updateStatus(media2);
                        }

                        return true;

                    } else {
                        // status with picture(s)
                        File[] files = new File[imagesAttached];
                        File outputDir = context.getCacheDir();

                        if (attachButton.isEnabled()) {
                            for (int i = 0; i < imagesAttached; i++) {
                                double bytes = 0;
                                try {
                                    files[i] = new File(URI.create(attachedUri[i]));
                                    bytes = files[i].length();
                                } catch (Exception e) {

                                }


                                if (bytes == 0 || bytes > GiphyHelper.TWITTER_SIZE_LIMIT) {
                                    files[i] = File.createTempFile("compose", "picture_" + i, outputDir);

                                    Bitmap bitmap = getBitmapToSend(Uri.parse(attachedUri[i]));
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                                    if (secondTry) {
                                        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
                                    }

                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                                    byte[] bitmapdata = bos.toByteArray();

                                    FileOutputStream fos = new FileOutputStream(files[i]);
                                    fos.write(bitmapdata);
                                    fos.flush();
                                    fos.close();
                                }
                            }
                        }


                        // use twitter4j's because it is easier
                        if (attachButton.isEnabled()) {
                            if (imagesAttached == 1) {
                                //media.setMedia(files[0]);
                                if (useAccOne) {
                                    long mediaId = 0;
                                    UploadedMedia upload = twitter.uploadMedia(files[0]);
                                    mediaId = upload.getMediaId();

                                    media.setMediaIds(new long[]{mediaId});
                                }

                                if (useAccTwo) {
                                    long mediaId = 0;
                                    UploadedMedia upload = twitter2.uploadMedia(files[0]);
                                    mediaId = upload.getMediaId();

                                    media2.setMediaIds(new long[]{mediaId});
                                }
                            } else {
                                // has multiple images and should be done through twitters service

                                if (useAccOne) {
                                    long[] mediaIds = new long[files.length];
                                    for (int i = 0; i < files.length; i++) {
                                        UploadedMedia upload = twitter.uploadMedia(files[i]);
                                        mediaIds[i] = upload.getMediaId();
                                    }

                                    media.setMediaIds(mediaIds);
                                }

                                if (useAccTwo) {
                                    long[] mediaIds = new long[files.length];
                                    for (int i = 0; i < files.length; i++) {
                                        UploadedMedia upload = twitter2.uploadMedia(files[i]);
                                        mediaIds[i] = upload.getMediaId();
                                    }

                                    media2.setMediaIds(mediaIds);
                                }
                            }
                        } else {
                            // animated gif
                            Log.v("talon_compose", "attaching: " + attachmentType);

                            files[0] = File.createTempFile("compose", "giphy_gif", outputDir);
                            InputStream stream = getContentResolver().openInputStream(Uri.parse(attachedUri[0]));
                            FileOutputStream fos = new FileOutputStream(files[0]);

                            int read = 0;
                            byte[] bytes = new byte[1024];

                            while ((read = stream.read(bytes)) != -1) {
                                fos.write(bytes, 0, read);
                            }

                            stream.close();
                            fos.close();

                            if (useAccOne) {
                                UploadedMedia upload = twitter.uploadMedia(files[0]);
                                long mediaId = upload.getMediaId();

                                media.setMediaIds(new long[]{mediaId});
                            }

                            if (useAccTwo) {
                                UploadedMedia upload = twitter2.uploadMedia(files[0]);
                                long mediaId = upload.getMediaId();

                                media2.setMediaIds(new long[]{mediaId});
                            }
                        }

                        if (addLocation) {
                            if (waitForLocation()) {
                                Location location = mLastLocation;
                                GeoLocation geolocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                                media.setLocation(geolocation);
                                media2.setLocation(geolocation);
                            }
                        }

                        twitter4j.Status s = null;
                        if (useAccOne) {
                            s = twitter.updateStatus(media);
                        }
                        if (useAccTwo) {
                            s = twitter2.updateStatus(media2);
                        }

                        if (status != null) {
                            final String[] text = status.split(" ");

                            new TimeoutThread(new Runnable() {
                                @Override
                                public void run() {
                                    ArrayList<String> tags = new ArrayList<String>();
                                    for (final String split : text) {
                                        if (split.contains("#")) {
                                            tags.add(split);
                                        }
                                    }

                                    HashtagDataSource source = HashtagDataSource.getInstance(context);

                                    for (String s : tags) {
                                        source.deleteTag(s);
                                        source.createTag(s);
                                    }
                                }
                            }).start();
                        }

                        return true;


                    }
                }

            } catch (final Exception e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayErrorNotification(e);
                    }
                });

                if (e.getMessage() != null && e.getMessage().contains("the uploaded media is too large.")) {
                    tryingAgain = true;
                    return false;
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                outofmem = true;
            }
            return false;
        }

        private boolean waitForLocation() {
            if (mLastLocation == null) {
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(1500);
                    } catch (Exception e) {

                    }
                    if (mLastLocation != null) {
                        break;
                    }
                }
            }

            return mLastLocation != null;
        }

        boolean outofmem = false;
        boolean tryingAgain = false;

        protected void onPostExecute(Boolean success) {
            // dismiss the dialog after getting all products
            try {
                stream.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            if (!tryingAgain) {
                if (success) {
                    finishedTweetingNotification();
                } else if (outofmem) {
                    Toast.makeText(context, getString(R.string.error_attaching_image), Toast.LENGTH_SHORT).show();
                } else {
                    makeFailedNotification(text);
                }
            } else {
                new updateTwitterStatus(text, remaining, true).execute(status);
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

    public boolean isAndroidN() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.M || Build.VERSION.CODENAME.equals("N");
    }
}
