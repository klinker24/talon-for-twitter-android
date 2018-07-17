package com.klinker.android.twitter_l.activities;
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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.compose.Compose;
import com.klinker.android.twitter_l.adapters.DMCursorAdapter;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.ThemeColor;
import com.klinker.android.twitter_l.data.sq_lite.DMDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefEditText;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.DirectMessageEvent;
import twitter4j.MessageData;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.UploadedMedia;
import twitter4j.User;


public class DirectMessageConversation extends WhiteToolbarActivity {

    public AppSettings settings;
    private Context context;
    private SharedPreferences sharedPrefs;

    private android.support.v7.app.ActionBar actionBar;

    private ListView listView;
    private FontPrefEditText composeBar;
    private ImageButton sendButton;
    private FontPrefTextView charRemaining;
    private ImageButton attachButton;

    private String listName;

    final Pattern p = Patterns.WEB_URL;
    public Handler countHandler;
    public Runnable getCount = new Runnable() {
        @Override
        public void run() {
            String text = composeBar.getText().toString();

            if (!Patterns.WEB_URL.matcher(text).find()) { // no links, normal tweet
                try {
                    charRemaining.setText(AppSettings.getInstance(context).tweetCharacterCount -
                            composeBar.getText().length() - (attachedUri.equals("") ? 0 : 23) + "");
                } catch (Exception e) {
                    charRemaining.setText("0");
                }
            } else {
                int count = text.length();
                Matcher m = p.matcher(text);
                while(m.find()) {
                    String url = m.group();
                    count -= url.length(); // take out the length of the url
                    count += 23; // add 23 for the shortened url
                }

                if (!attachedUri.equals("")) {
                    count += 23;
                }

                charRemaining.setText(AppSettings.getInstance(context).tweetCharacterCount - count + "");
            }
        }
    };

    @Override
    public void onDestroy() {
        try {
            cursorAdapter.getCursor().close();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        countHandler = new Handler();

        context = this;
        sharedPrefs = AppSettings.getSharedPreferences(this);

        settings = AppSettings.getInstance(this);

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

        Utils.setUpMainTheme(this, settings);

        setContentView(R.layout.dm_conversation);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(settings.themeColors.primaryColor);
        setSupportActionBar(toolbar);

        attachImage = (ImageView) findViewById(R.id.attached_image);

        listView = (ListView) findViewById(R.id.listView);
        sendButton = (ImageButton) findViewById(R.id.send_button);
        composeBar = (FontPrefEditText) findViewById(R.id.tweet_content);
        charRemaining = (FontPrefTextView) findViewById(R.id.char_remaining);
        attachButton = (ImageButton) findViewById(R.id.attach_button);

        charRemaining.setVisibility(View.GONE);

        listName = getIntent().getStringExtra("screenname");

        actionBar = getSupportActionBar();
        actionBar.setTitle(getIntent().getStringExtra("name"));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        if (settings.revampedTweets()) {
            listView.setDivider(null);
        }

        new GetList().execute();

        charRemaining.setText(AppSettings.getInstance(this).tweetCharacterCount - composeBar.getText().length() + "");
        composeBar.addTextChangedListener(new TextWatcher() {
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

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attachImage();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = composeBar.getText().toString();

                new SendDirectMessage().execute(status);
                composeBar.setText("");
                attachImage.setVisibility(View.GONE);
                Toast.makeText(context, getString(R.string.sending), Toast.LENGTH_SHORT).show();
            }
        });

        //Utils.setActionBar(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(settings.themeColors.primaryColorDark);
        } else {
            View status = findViewById(R.id.kitkat_status_bar);
            status.setVisibility(View.VISIBLE);

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) status.getLayoutParams();
            params.height = Utils.getStatusBarHeight(this);
            status.setLayoutParams(params);
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

    class GetList extends AsyncTask<String, Void, Cursor> {

        protected Cursor doInBackground(String... urls) {
            try {
                Cursor cursor = DMDataSource.getInstance(context)
                        .getConvCursor(listName, settings.currentAccount);
                return cursor;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Cursor cursor) {

            if (cursor != null) {
                Cursor c = null;
                try {
                    c = cursorAdapter.getCursor();
                } catch (Exception e) {

                }
                cursorAdapter = new DMCursorAdapter(context, cursor, true);
                try {
                    listView.setAdapter(cursorAdapter);
                } catch (Exception e) {
                    // database is closed
                    try {
                        DMDataSource.getInstance(context).close();
                    } catch (Exception x) {

                    }
                    new GetList().execute();
                    return;
                }

                listView.setVisibility(View.VISIBLE);
                listView.setStackFromBottom(true);
                listView.setSelection(cursorAdapter.getCount());
                listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);


                try {
                    c.close();
                } catch (Exception e) {

                }
            }

            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    }

    public TimeLineCursorAdapter cursorAdapter;

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
                User user = twitter.showUser(sendTo);
                MessageData data = new MessageData(user.getId(), status);

                if (!attachedUri.equals("")) {
                    try {
                        File f;

                        if (attachmentType == null) {
                            // image file
                            f = ImageUtils.scaleToSend(context, Uri.parse(attachedUri));
                        } else {
                            f = new File(URI.create(attachedUri));
                        }

                        UploadedMedia media = twitter.uploadMedia(f);
                        data.setMediaId(media.getMediaId());
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(context, getString(R.string.error_attaching_image), Toast.LENGTH_SHORT).show());
                    }

                }

                DirectMessageEvent message = twitter.createMessage(data);

                if (!settings.pushNotifications) {
                    DMDataSource.getInstance(context).createSentDirectMessage(message, user, settings, settings.currentAccount);
                }

                sharedPrefs.edit().putLong("last_direct_message_id_" + sharedPrefs.getInt("current_account", 1), message.getId()).apply();
                sharedPrefs.edit().putBoolean("refresh_me_dm", true).apply();

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

            attachedUri = "";
            attachmentType = "";

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
        filter.addAction("com.klinker.android.twitter.NEW_DIRECT_MESSAGE");
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

    public ImageView attachImage;
    public String attachedUri = "";
    public String attachmentType = "";

    public static final int SELECT_PHOTO = 100;
    public static final int CAPTURE_IMAGE = 101;
    public static final int SELECT_GIF = 102;
    public static final int FIND_GIF = 104;

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
                            attachImage.setImageBitmap(getThumbnail(selectedImage));
                            attachImage.setVisibility(View.VISIBLE);
                            attachedUri = selectedImage.toString();
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
            case FIND_GIF:
            case SELECT_GIF:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri selectedImage = imageReturnedIntent.getData();

                        String filePath = IOUtils.getPath(selectedImage, context);

                        Log.v("talon_compose_pic", "path to gif on sd card: " + filePath);

                        attachImage.setImageBitmap(getThumbnail(selectedImage));
                        attachImage.setVisibility(View.VISIBLE);
                        attachedUri = selectedImage.toString();
                        attachmentType = "animated_gif";
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
                    .start(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void attachImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(R.array.attach_dm_options, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0) { // take picture
                    Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File f = new File(Environment.getExternalStorageDirectory() + "/Talon/", "photoToTweet.jpg");

                    if (!f.exists()) {
                        try {
                            f.getParentFile().mkdirs();
                            f.createNewFile();
                        } catch (IOException e) {

                        }
                    }

                    captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    try {
                        Uri photoURI = FileProvider.getUriForFile(context,
                                BuildConfig.APPLICATION_ID + ".provider", f);

                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(captureIntent, CAPTURE_IMAGE);
                    } catch (Exception e) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Toast.makeText(DirectMessageConversation.this, "Have you given Talon the storage permission?", Toast.LENGTH_LONG).show();
                        }
                    }

                } else if (item == 1) { // attach picture
                    try {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO);
                    } catch (Exception e) {
                        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                        photoPickerIntent.setType("image/*");
                        startActivityForResult(Intent.createChooser(photoPickerIntent,
                                "Select Picture"), SELECT_PHOTO);
                    }
                } else if (item == 2) {
                    Toast.makeText(DirectMessageConversation.this, "GIFs must be less than 5 MB", Toast.LENGTH_SHORT).show();

                    try {
                        Intent gifIntent = new Intent();
                        gifIntent.setType("image/gif");
                        gifIntent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(gifIntent, SELECT_GIF);
                    } catch (Exception e) {
                        Intent gifIntent = new Intent();
                        gifIntent.setType("image/gif");
                        gifIntent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(gifIntent, SELECT_GIF);
                    }
                } else if (item == 3) {
                    Intent gif = new Intent(context, GiphySearch.class);
                    startActivityForResult(gif, FIND_GIF);
                }
            }
        });

        builder.create().show();
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

            options.inSampleSize = Compose.calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            Bitmap b = BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            if (!Compose.isAndroidN()) {
                ExifInterface exif = new ExifInterface(IOUtils.getPath(uri, context));
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                input.close();

                b = ImageUtils.cropSquare(b);
                return Compose.rotateBitmap(b, orientation);
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

}