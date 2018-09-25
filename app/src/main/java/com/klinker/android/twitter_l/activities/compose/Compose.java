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
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Vibrator;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.util.Pair;
import android.text.Editable;
import android.text.Html;
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
import com.github.ajalt.reprint.core.Reprint;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.GiphySearch;
import com.klinker.android.twitter_l.data.ThemeColor;
import com.klinker.android.twitter_l.data.sq_lite.HashtagDataSource;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.utils.FingerprintDialog;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.views.widgets.EmojiKeyboard;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.api_helper.GiphyHelper;
import com.klinker.android.twitter_l.utils.api_helper.TwitLongerHelper;
import com.klinker.android.twitter_l.utils.Utils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.klinker.android.twitter_l.utils.text.TextUtils;
import com.klinker.android.twitter_l.views.widgets.ImageKeyboardEditText;
import com.yalantis.ucrop.UCrop;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.format.AndroidStandardFormatStrategy;
import net.ypresto.androidtranscoder.format.MediaFormatStrategyPresets;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.*;
import uk.co.senab.photoview.PhotoViewAttacher;

public abstract class Compose extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        InputConnectionCompat.OnCommitContentListener {

    private static final boolean DEBUG = false;

    public GoogleApiClient mGoogleApiClient;
    public AppSettings settings;
    public Context context;
    public SharedPreferences sharedPrefs;

    public EditText contactEntry;
    public ImageKeyboardEditText reply;
    public ImageView[] attachImage = new ImageView[4];
    public ImageButton[] cancelButton = new ImageButton[4];
    public FrameLayout[] holders = new FrameLayout[4];
    public ImageButton gifButton;
    public ImageButton attachButton;
    public ImageButton emojiButton;
    public EmojiKeyboard emojiKeyboard;
    public ImageButton overflow;
    public TextView charRemaining;
    public ListPopupWindow hashtagAutoComplete;
    public FontPrefTextView numberAttached;

    protected boolean useAccOne = true;
    protected boolean useAccTwo = false;

    protected boolean sharingSomething = false;
    protected String attachmentUrl = null; // quoted tweet

    // attach up to four images
    public String[] attachedUri = new String[]{"", "", "", ""};
    public int imagesAttached = 0;

    public PhotoViewAttacher mAttacher;

    public boolean isDM = false;

    public String to = null;
    public long notiId = 0;
    public String replyText = "";
    public String quotingAStatus = null;

    protected boolean attachButtonEnabled = true;

    public int currentAccount;

    final Pattern p = Patterns.WEB_URL;

    private int getCountFromString(String text) {
        if (AppSettings.isLimitedTweetCharLanguage()) {
            return text.getBytes().length;
        } else {
            return text.length();
        }
    }

    public Handler countHandler;
    public Runnable getCount = new Runnable() {
        @Override
        public void run() {
            String text = reply.getText().toString();

            if (shouldReplaceTo(text)) {
                String replaceable = to.replaceAll("#[a-zA-Z]+ ", "");

                if (!replaceable.equals(" ")) {
                    try {
                        text = text.replaceAll(replaceable, "");
                    } catch (Exception e) {
                    }
                }
            }

            if (!Patterns.WEB_URL.matcher(text).find() && quotingAStatus == null) { // no links, normal tweet
                try {
                    charRemaining.setText(AppSettings.getInstance(context).tweetCharacterCount - getCountFromString(text) + "");
                } catch (Exception e) {
                    charRemaining.setText("0");
                }
            } else {
                int count = getCountFromString(text);
                Matcher m = p.matcher(text);
                while (m.find()) {
                    String url = m.group();
                    count -= url.length(); // take out the length of the url
                    count += 23; // add 23 for the shortened url
                }

                if (quotingAStatus != null) {
                    count += 24;
                }

                charRemaining.setText(AppSettings.getInstance(context).tweetCharacterCount - count + "");
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

        if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered() &&
                AppSettings.getInstance(this).fingerprintLock) {
            new FingerprintDialog(this).show();
        }

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
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

//        int currentOrientation = getResources().getConfiguration().orientation;
//        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
//        } else {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
//        }

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
            if (quotingAStatus != null) {
                replyTo.setText(Html.fromHtml("<b>" + getString(R.string.quoting) + "</b><br/><br/>" + replyText));
            } else {
                replyTo.setText(replyText);
            }
            TextUtils.linkifyText(context, replyTo, null, true, "", true);

            View replyToCard = findViewById(R.id.reply_to_card);

            if (replyToCard != null) {
                replyToCard.setVisibility(View.VISIBLE);
            }

            replyTo.setTextSize(settings.textSize);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String text = reply.getText().toString();

                try {
                    if (!android.text.TextUtils.isEmpty(text) && !(text.startsWith(" RT @") || quotingAStatus != null)) {
                        text = text.replaceAll("  ", " ");
                        reply.setText(text);
                        reply.setSelection(text.length());

                        if (!text.isEmpty() && !text.endsWith(" ")) {
                            reply.append(" ");
                        }

                        if (text.trim().isEmpty()) {
                            reply.setText("");
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

        // change the background color for the cursor
        if (settings.darkTheme && (settings.theme == AppSettings.THEME_BLACK || settings.theme == AppSettings.THEME_DARK_BACKGROUND_COLOR)) {
            if (Utils.isAndroidP()) {
                return;
            }

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
                                            boolean close = doneClick();
                                            if (close) {
                                                onBackPressed();
                                            }
                                        }
                                    }).setNeutralButton(R.string.pwiccer, new DialogInterface.OnClickListener() {
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
                            }).setNegativeButton(R.string.split_tweet, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    multiTweet = true;
                                    boolean close = doneClick();
                                    if (close) {
                                        onBackPressed();
                                    }
                                }
                            }).create()
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

        cancelButton[0] = (ImageButton) findViewById(R.id.cancel1);
        cancelButton[1] = (ImageButton) findViewById(R.id.cancel2);
        cancelButton[2] = (ImageButton) findViewById(R.id.cancel3);
        cancelButton[3] = (ImageButton) findViewById(R.id.cancel4);

        holders[0] = (FrameLayout) findViewById(R.id.holder1);
        holders[1] = (FrameLayout) findViewById(R.id.holder2);
        holders[2] = (FrameLayout) findViewById(R.id.holder3);
        holders[3] = (FrameLayout) findViewById(R.id.holder4);

        attachButton = (ImageButton) findViewById(R.id.attach);
        gifButton = (ImageButton) findViewById(R.id.gif);
        emojiButton = (ImageButton) findViewById(R.id.emoji);
        emojiKeyboard = (EmojiKeyboard) findViewById(R.id.emojiKeyboard);
        reply = (ImageKeyboardEditText) findViewById(R.id.tweet_content);
        charRemaining = (TextView) findViewById(R.id.char_remaining);

        reply.setCommitContentListener(this);

        gifButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findGif();
            }
        });

        for (int i = 0; i < cancelButton.length; i++) {
            final int pos = i;
            cancelButton[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    imagesAttached--;

                    List<String> uris = new ArrayList<String>();
                    for (String uri : attachedUri) {
                        uris.add(uri);

                    }
                    uris.remove(pos);

                    for (int i = 0; i < attachImage.length; i++) {
                        attachImage[i].setImageDrawable(null);
                        attachedUri[i] = null;
                        holders[i].setVisibility(View.GONE);
                    }
                    for (int i = 0; i < imagesAttached; i++) {
                        attachImage[i].setImageURI(Uri.parse(uris.get(i)));
                        attachedUri[i] = uris.get(i);
                        holders[i].setVisibility(View.VISIBLE);
                    }

                    attachButton.setEnabled(true);
                    attachButtonEnabled = true;
                }
            });
        }

        findViewById(R.id.prompt_pos).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v("talon_input", "clicked the view");
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(reply, InputMethodManager.SHOW_FORCED);
            }
        });

        ImageView pic = (ImageView) findViewById(R.id.profile_pic);
        FontPrefTextView currentName = (FontPrefTextView) findViewById(R.id.current_name);

        if (!(this instanceof ComposeSecAccActivity))
            Glide.with(this).load(settings.myProfilePicUrl).into(pic);

        currentName.setText("@" + settings.myScreenName);

        //numberAttached.setText("0 " + getString(R.string.attached_images));

        charRemaining.setText(AppSettings.getInstance(this).tweetCharacterCount - reply.getText().length() + "");

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

    public void findGif() {
        Intent gif = new Intent(context, GiphySearch.class);
        startActivityForResult(gif, FIND_GIF);
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

    private Bitmap getThumbnail(Uri uri) throws IOException {
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

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Log.v("talon_composing_image", "rotation: " + orientation);

        try {
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
                new NotificationCompat.Builder(this, NotificationChannelUtil.FAILED_TWEETS_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.tweet_failed))
                        .setContentText(e.getMessage());

        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(context);
        mNotificationManager.cancelAll();
        mNotificationManager.notify(221, mBuilder.build());
    }

    public void makeFailedNotification(String text) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, NotificationChannelUtil.FAILED_TWEETS_CHANNEL)
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
                new NotificationCompat.Builder(this, NotificationChannelUtil.TWEETING_NOTIFICATION_CHANNEL)
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
                            new NotificationCompat.Builder(context, NotificationChannelUtil.TWEETING_NOTIFICATION_CHANNEL)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setContentTitle(getResources().getString(R.string.tweet_success))
                                    .setOngoing(false)
                                    .setTicker(getResources().getString(R.string.tweet_success));

                    if (settings.vibrate) {
                        Log.v("talon_vibrate", "vibrate on compose");
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        long[] pattern = {0, 50, 500};
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

            if (settings.theme == AppSettings.THEME_WHITE) {
                ThemeColor color = new ThemeColor("darkTheme", this);
                options.setToolbarColor(color.primaryColor);
                options.setStatusBarColor(color.primaryColorDark);
            } else {
                options.setToolbarColor(settings.themeColors.primaryColor);
                options.setStatusBarColor(settings.themeColors.primaryColorDark);
            }

            options.setActiveWidgetColor(settings.themeColors.accentColor);
            options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
            options.setCompressionQuality(100);
            options.setFreeStyleCropEnabled(true);

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
    public static final int CAPTURE_VIDEO = 105;
    public static final int PWICCER = 420;

    public boolean pwiccer = false;
    private boolean multiTweet = false;

    public String attachmentType = "";

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent imageReturnedIntent) {
        Log.v("talon_image_attach", "got the result, code: " + requestCode);
        switch (requestCode) {
            case UCrop.REQUEST_CROP:
                if (resultCode == RESULT_OK) {
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

                        if (this instanceof ComposeDMActivity) {
                            attachButton.setEnabled(false);
                            attachButtonEnabled = false;
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
                if (resultCode == RESULT_OK) {
                    startUcrop(imageReturnedIntent.getData());
                }

                break;
            case CAPTURE_IMAGE:
                if (resultCode == RESULT_OK) {
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
                if (resultCode == RESULT_OK) {
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
                        attachButtonEnabled = false;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                    }
                }
                countHandler.post(getCount);
                break;
            case CAPTURE_VIDEO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();

                    Log.v("talon_compose_pic", "path to surfaceView on sd card: " + selectedImage);

                    Glide.with(this)
                            .load(selectedImage)
                            .into(attachImage[0]);

                    holders[0].setVisibility(View.VISIBLE);
                    attachedUri[0] = selectedImage.toString();
                    imagesAttached = 1;

                    attachmentType = "video/mp4";
                    attachButton.setEnabled(false);
                    attachButtonEnabled = false;
                }
                break;
            case SELECT_VIDEO:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        Log.v("talon_compose_pic", "path to surfaceView on sd card: " + selectedImage);

                        Glide.with(this)
                                .load(selectedImage)
                                .into(attachImage[0]);

                        holders[0].setVisibility(View.VISIBLE);
                        attachedUri[0] = selectedImage.toString();
                        imagesAttached = 1;

                        startVideoEncoding(imageReturnedIntent);

                        attachmentType = "video/mp4";
                        attachButton.setEnabled(false);
                        attachButtonEnabled = false;
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
        return tweetText != null && to != null && quotingAStatus == null &&
                notiId != 0 && !sharingSomething &&
                tweetText.contains(to) && tweetText.startsWith("@") &&
                !tweetText.contains("@" + AppSettings.getInstance(this).myScreenName) &&
                !replyText.contains("@" + AppSettings.getInstance(this).myScreenName + ": ") &&
                !tweetText.contains(" RT @");
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

                String sendTo = contactEntry.getText().toString().replace("@", "").replace(" ", "");
                User user = twitter.showUser(sendTo);
                MessageData data = new MessageData(user.getId(), status);

                if (!attachedUri[0].equals("")) {
                    try {
                        File f;

                        if (attachmentType == null) {
                            // image file
                            f = ImageUtils.scaleToSend(context, Uri.parse(attachedUri[0]));
                        } else {
                            f = new File(URI.create(attachedUri[0]));
                        }

                        UploadedMedia media = twitter.uploadMedia(f);
                        data.setMediaId(media.getMediaId());
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(context, getString(R.string.error_attaching_image), Toast.LENGTH_SHORT).show());
                    }

                }

                DirectMessageEvent event = twitter.createMessage(data);
                return event != null;

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

    class UpdateTwitterStatus extends AsyncTask<String, String, Boolean> {

        String text;
        String status;
        private boolean secondTry;
        private int remaining;
        private InputStream stream;

        public UpdateTwitterStatus(String text, int length) {
            if (quotingAStatus != null) {
                text += " " + quotingAStatus;
            }

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

        public UpdateTwitterStatus(String text, int length, boolean secondTry) {
            if (quotingAStatus != null) {
                text += " " + quotingAStatus;
            }

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

        /**
         * Helper method for posting the status update using TwitLonger
         *
         * @param twitter the account to tweet from
         * @return bool true if successful else false
         */
        private boolean tweetUsingTwitLonger(Twitter twitter) {
            boolean isDone = false;
            TwitLongerHelper helper = new TwitLongerHelper(text, twitter, Compose.this);

            if (notiId != 0) {
                helper.setInReplyToStatusId(notiId);
            }

            if (addLocation) {
                //waitForLocation();
                if (waitForLocation()) {
                    Location location = mLastLocation;
                    GeoLocation geolocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                    helper.setLocation(geolocation);
                }
            }

            if (helper.createPost() != 0) {
                isDone = true;
            }

            return isDone;
        }

        private Pair<String, List<String>> getMultipeTweets(String message) {
            List<String> multiTweets = new Vector<>();
            String mentions = "";
            String[] tokens = message.split(" ");
            String tempString = "";
            /* Only check for 272 as we are adding (xx/xx) at the end of long tweets */
            for (int i = 0; i < tokens.length; i++) {
                if (notiId != 0) {
                    /* This is a reply tweet Take any mentions out of the tweets */
                    if (tokens[i].contains("@")) {
                        mentions += tokens[i] + " ";
                        continue;
                    }
                }
                if (tempString.length() + tokens[i].length() + 1 <= 272) {
                    tempString += tokens[i] + " ";
                } else {
                    /* We have our split tweet */
                    multiTweets.add(tempString);
                    tempString = tokens[i] + " ";
                }
            }
            /* Last tweet will fall out of loop */
            multiTweets.add(tempString);
            return Pair.create(mentions, multiTweets);
        }

        /**
         * Helper function to tweet the updates without attaching images.
         *
         * @param twitter The account used to tweet
         */
        private void tweetWithoutImages(Twitter twitter) throws Exception {
            tweetWithoutImages(twitter, false, 0);
        }

        /**
         * Helper function to tweet updates without attaching images. Set the tweet to
         * scheduled as a workaround for the rate-limit from twitter. Provide a time if the
         * tweet is scheduled.
         *
         * @param twitter   The account used to tweet
         * @param scheduled True if tweet needs to be scheduled
         */
        private void tweetWithoutImages(Twitter twitter, boolean scheduled, long time) throws Exception {
            if (scheduled) {
                // some guy wanted this for the future I guess. The one the did the multi tweet PR
//                ScheduledTweet tweet = new ScheduledTweet(getApplicationContext(), context, status, time, 0);
//                tweet.createScheduledTweet();
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
                media.setAutoPopulateReplyMetadata(autoPopulateMetadata);

                if (notiId != 0) {
                    media.setInReplyToStatusId(notiId);
                }

                // Update status
                if (addLocation) {
                    if (waitForLocation()) {
                        Location location = mLastLocation;
                        GeoLocation geolocation = new GeoLocation(location.getLatitude(), location.getLongitude());
                        media.setLocation(geolocation);
                    }
                }

                twitter4j.Status status = twitter.updateStatus(media);
                if (status != null) {
                    notiId = status.getId();
                }
            }
        }

        private UploadedMedia uploadImage(Twitter twitter, String uri) throws FileNotFoundException, TwitterException {
            try {
                return twitter.uploadMedia("talon_" + new Date().getTime(),
                        getContentResolver().openInputStream(Uri.parse(uri)));
            } catch (Exception e) {
                long bytes;
                try {
                    File file = new File(URI.create(uri));
                    bytes = file.length();

                    if (bytes == 0 || bytes > GiphyHelper.TWITTER_SIZE_LIMIT) {
                        file = ImageUtils.scaleToSend(Compose.this, Uri.parse(uri));
                    }

                    return twitter.uploadMedia(file);
                } catch (Exception x) {

                }
            }

            return null;
        }

        protected Boolean doInBackground(String... args) {
            status = args[0];

            if (quotingAStatus != null) {
                status += " " + quotingAStatus;
            }

            try {
                Twitter twitter = Utils.getTwitter(getApplicationContext(), settings);
                Twitter twitter2 = Utils.getSecondTwitter(getApplicationContext());

                if (remaining < 0 && !pwiccer && !multiTweet) {
                    // twitlonger goes here
                    boolean isDone = false;

                    if (useAccOne) {
                        isDone = tweetUsingTwitLonger(twitter);
                    }

                    if (useAccTwo) {
                        isDone = tweetUsingTwitLonger(twitter2);
                    }

                    return isDone;
                } else if (multiTweet && remaining < 0) {
                    Pair<String, List<String>> multiTweets = getMultipeTweets(status);
                    int noOfTweets = multiTweets.second.size();
                    int tweetNo = 1;
                    for (int i = 0; i < noOfTweets; i++) {
                        status = multiTweets.first.length() != 0 ? multiTweets.first : "";
                        status += multiTweets.second.get(i) + "(" + tweetNo + "/" + noOfTweets + ")";
                        tweetNo++;
                        if (useAccOne) {
                            tweetWithoutImages(twitter);
                        }
                        if (useAccTwo) {
                            tweetWithoutImages(twitter2);
                        }
                    }

                    multiTweet = false;
                    return true;
                } else {
                    if (imagesAttached == 0) {
                        if (useAccOne) {
                            tweetWithoutImages(twitter);
                        }
                        if (useAccTwo) {
                            tweetWithoutImages(twitter2);
                        }

                        return true;

                    } else {
                        // status with picture(s)
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

                        File[] files = new File[imagesAttached];
                        File outputDir = context.getCacheDir();

//                        if (attachButton.isEnabled()) {
//                            for (int i = 0; i < imagesAttached; i++) {
//                                double bytes = 0;
//                                try {
//                                    files[i] = new File(URI.create(attachedUri[i]));
//                                    bytes = files[i].length();
//                                } catch (Exception e) {
//
//                                }
//
//                                if (bytes == 0 || bytes > GiphyHelper.TWITTER_SIZE_LIMIT) {
//                                    files[i] = ImageUtils.scaleToSend(Compose.this, Uri.parse(attachedUri[i]));
//                                }
//                            }
//                        }


                        // use twitter4j's because it is easier
                        if (attachButtonEnabled) {
                            if (imagesAttached == 1) {
                                //media.setMedia(files[0]);
                                if (useAccOne) {
                                    UploadedMedia upload = uploadImage(twitter, attachedUri[0]);

                                    if (upload != null) {
                                        long mediaId = upload.getMediaId();
                                        media.setMediaIds(mediaId);
                                    }
                                }

                                if (useAccTwo) {
                                    UploadedMedia upload = uploadImage(twitter2, attachedUri[0]);

                                    if (upload != null) {
                                        long mediaId = upload.getMediaId();
                                        media2.setMediaIds(mediaId);
                                    }
                                }
                            } else {
                                // has multiple images and should be done through twitters service

                                if (useAccOne) {
                                    long[] mediaIds = new long[files.length];
                                    for (int i = 0; i < files.length; i++) {
                                        UploadedMedia upload = uploadImage(twitter, attachedUri[i]);
                                        if (upload != null) {
                                            mediaIds[i] = upload.getMediaId();
                                        }
                                    }

                                    media.setMediaIds(mediaIds);
                                }

                                if (useAccTwo) {
                                    long[] mediaIds = new long[files.length];
                                    for (int i = 0; i < files.length; i++) {
                                        UploadedMedia upload = uploadImage(twitter2, attachedUri[i]);
                                        if (upload != null) {
                                            mediaIds[i] = upload.getMediaId();
                                        }
                                    }

                                    media2.setMediaIds(mediaIds);
                                }
                            }
                        } else {
                            // animated gif or video
                            Log.v("talon_compose", "attaching: " + attachmentType);
                            Log.v("talon_compose", "media: " + attachedUri[0]);

                            if (attachmentType.equals("animated_gif")) {
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

                                    media.setMediaIds(mediaId);
                                }

                                if (useAccTwo) {
                                    UploadedMedia upload = twitter2.uploadMedia(files[0]);
                                    long mediaId = upload.getMediaId();

                                    media2.setMediaIds(mediaId);
                                }
                            } else {
                                files[0] = File.createTempFile("compose", "video", outputDir);
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
                                    UploadedMedia upload = twitter.uploadVideo(files[0]);
                                    long mediaId = upload.getMediaId();

                                    media.setMediaIds(mediaId);
                                }

                                if (useAccTwo) {
                                    UploadedMedia upload = twitter2.uploadVideo(files[0]);
                                    long mediaId = upload.getMediaId();

                                    media2.setMediaIds(mediaId);
                                }
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

                            new Thread(new Runnable() {
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
                new UpdateTwitterStatus(text, remaining, true).execute(status);
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

    public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
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

    public static boolean isAndroidN() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.M || Build.VERSION.CODENAME.equals("N");
    }

    public void startVideoEncoding(final Intent data) {
        startVideoEncoding(data, AndroidStandardFormatStrategy.Encoding.HD_720P);
    }

    public void startVideoEncoding(final Intent data, final AndroidStandardFormatStrategy.Encoding encoding) {
        final File file;
        try {
            File outputDir = new File(getExternalFilesDir(null), "outputs");
            outputDir.mkdir();
            file = File.createTempFile("transcode_video", ".mp4", outputDir);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create temporary file.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentResolver resolver = getContentResolver();
        final ParcelFileDescriptor parcelFileDescriptor;
        try {
            parcelFileDescriptor = resolver.openFileDescriptor(data.getData(), "r");
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "File not found.", Toast.LENGTH_LONG).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.preparing_video));

        final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
            @Override
            public void onTranscodeCanceled() {
            }

            @Override
            public void onTranscodeFailed(Exception exception) {
                try {
                    progressDialog.cancel();
                } catch (Exception e) {

                }

                attachedUri[0] = data.getData().toString();
            }

            @Override
            public void onTranscodeProgress(double progress) {
            }

            @Override
            public void onTranscodeCompleted() {
                if (file.length() > 15 * 1024 * 1024 && !encoding.equals(AndroidStandardFormatStrategy.Encoding.SD_HIGH)) {
                    startVideoEncoding(data, AndroidStandardFormatStrategy.Encoding.SD_HIGH);
                } else {
                    attachedUri[0] = Uri.fromFile(file).toString();
                }

                try {
                    progressDialog.cancel();
                } catch (Exception e) {

                }
            }
        };

        progressDialog.show();
        MediaTranscoder.getInstance().transcodeVideo(fileDescriptor, file.getAbsolutePath(),
                MediaFormatStrategyPresets.createStandardFormatStrategy(AndroidStandardFormatStrategy.Encoding.HD_720P), listener);

    }

    @Override
    public boolean onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
        String mime = inputContentInfo.getDescription().getMimeType(0);

        if (mime.equals("image/gif")) {
            try {
                attachImage[0].setImageBitmap(getThumbnail(inputContentInfo.getContentUri()));
                holders[0].setVisibility(View.VISIBLE);
                attachedUri[0] = inputContentInfo.getContentUri().toString();
                imagesAttached = 1;

                attachmentType = "animated_gif";

                attachButton.setEnabled(false);
                attachButtonEnabled = false;
            } catch (Exception e) {

            }
        } else if (mime.contains("image/")) {
            try {
                attachImage[imagesAttached].setImageBitmap(getThumbnail(inputContentInfo.getContentUri()));
                holders[imagesAttached].setVisibility(View.VISIBLE);
                attachedUri[imagesAttached] = inputContentInfo.getContentUri().toString();
                imagesAttached++;
            } catch (Throwable e) {
                Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT);
            }
        } else if (mime.contains("video/mp4")) {
            Glide.with(this)
                    .load(inputContentInfo.getContentUri())
                    .into(attachImage[0]);

            holders[0].setVisibility(View.VISIBLE);
            attachedUri[0] = inputContentInfo.getContentUri().toString();
            imagesAttached = 1;

            attachmentType = "video/mp4";
            attachButton.setEnabled(false);
            attachButtonEnabled = false;
        }

        return true;
    }
}
