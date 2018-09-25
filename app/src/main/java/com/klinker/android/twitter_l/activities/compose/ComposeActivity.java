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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.afollestad.materialcamera.MaterialCamera;
import com.bumptech.glide.Glide;
import com.github.ajalt.reprint.core.Reprint;
import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.UserAutoCompleteHelper;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefEditText;
import com.klinker.android.twitter_l.views.widgets.text.FontPrefTextView;
import com.klinker.android.twitter_l.activities.scheduled_tweets.ViewScheduledTweets;
import com.klinker.android.twitter_l.utils.Utils;

import java.io.File;
import java.io.IOException;


public class ComposeActivity extends Compose {

    public void setUpLayout() {
        setContentView(R.layout.compose_activity);

        setUpSimilar();

        int count = 0; // number of accounts logged in

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }

        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        if (count == 2) {
            findViewById(R.id.accounts).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String[] options = new String[2];
//                    String[] options = new String[3];

                    options[0] = "@" + settings.myScreenName;
                    options[1] = "@" + settings.secondScreenName;
//                    options[2] = getString(R.string.both_accounts);

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, final int item) {
                            ImageView pic = (ImageView) findViewById(R.id.profile_pic);
                            FontPrefTextView currentName = (FontPrefTextView) findViewById(R.id.current_name);

                            switch (item) {
                                case 0:
                                    useAccOne = true;
                                    useAccTwo = false;

                                    Glide.with(ComposeActivity.this).load(settings.myProfilePicUrl).into(pic);
                                    currentName.setText("@" + settings.myScreenName);

                                    String tweetText = reply.getText().toString();
                                    tweetText = tweetText.replace("@" + settings.myScreenName + " ", "")
                                                    .replace("@" + settings.myScreenName, "");

                                    reply.setText(tweetText);
                                    reply.setSelection(tweetText.length());

                                    break;
                                case 1:
                                    useAccOne = false;
                                    useAccTwo = true;

                                    Glide.with(ComposeActivity.this).load(settings.secondProfilePicUrl).into(pic);
                                    currentName.setText("@" + settings.secondScreenName);

                                    tweetText = reply.getText().toString();
                                    tweetText = tweetText.replace("@" + settings.secondScreenName + " ", "")
                                                    .replace("@" + settings.secondScreenName, "");

                                    reply.setText(tweetText);
                                    reply.setSelection(tweetText.length());

                                    break;
                                case 2:
                                    useAccOne = true;
                                    useAccTwo = true;

                                    TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.bothAccounts});
                                    int resource = a.getResourceId(0, 0);
                                    a.recycle();
                                    pic.setImageResource(resource);

                                    currentName.setText(getString(R.string.both_accounts));

                                    break;
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        final UserAutoCompleteHelper userAutoCompleteHelper = UserAutoCompleteHelper.applyTo(this, reply);

        overflow = (ImageButton) findViewById(R.id.overflow_button);
        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attachImage();
            }
        });

        ImageButton at = (ImageButton) findViewById(R.id.at_button);
        at.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int start = reply.getSelectionStart();
                reply.getText().insert(start, "@");
                reply.setSelection(start + 1);

                ListPopupWindow window = userAutoCompleteHelper.getUserAutoComplete();
                try {
                    if (!window.isShowing()) {
                        window.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        ImageButton hashtag = (ImageButton) findViewById(R.id.hashtag_button);
        hashtag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int start = reply.getSelectionStart();
                reply.getText().insert(start, "#");
                reply.setSelection(start + 1);

                ListPopupWindow window = userAutoCompleteHelper.getHashtagAutoComplete();
                try {
                    if (AppSettings.getInstance(context).autoCompleteHashtags && !window.isShowing()) {
                        window.show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final int SAVE_DRAFT = 0;
        final int VIEW_DRAFTS = 1;
        final int VIEW_QUEUE = 2;
        final int SCHEDULE = 3;
        final int ENABLE_FINGERPRINT_LOCK = 4;
        final int DISABLE_FINGERPRINT_LOCK = 5;

        final ImageButton overflow = (ImageButton) findViewById(R.id.overflow_button);
        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu menu = new PopupMenu(context, findViewById(R.id.overflow_button));

                menu.getMenu().add(Menu.NONE, SAVE_DRAFT, Menu.NONE, context.getString(R.string.menu_save_draft));
                menu.getMenu().add(Menu.NONE, VIEW_DRAFTS, Menu.NONE, context.getString(R.string.menu_view_drafts));
                menu.getMenu().add(Menu.NONE, VIEW_QUEUE, Menu.NONE, context.getString(R.string.menu_view_queued));
                menu.getMenu().add(Menu.NONE, SCHEDULE, Menu.NONE, context.getString(R.string.menu_schedule_tweet));

                if (Reprint.isHardwarePresent() && Reprint.hasFingerprintRegistered()) {
                    if (!AppSettings.getInstance(ComposeActivity.this).fingerprintLock) {
                        menu.getMenu().add(Menu.NONE, ENABLE_FINGERPRINT_LOCK, Menu.NONE, context.getString(R.string.enable_fingerprint_lock));
                    } else {
                        menu.getMenu().add(Menu.NONE, DISABLE_FINGERPRINT_LOCK, Menu.NONE, context.getString(R.string.disable_fingerprint_lock));
                    }
                }

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case DISABLE_FINGERPRINT_LOCK:
                                sharedPrefs.edit().putBoolean("fingerprint_lock", false).apply();
                                AppSettings.invalidate();
                                break;
                            case ENABLE_FINGERPRINT_LOCK:
                                sharedPrefs.edit().putBoolean("fingerprint_lock", true).apply();
                                AppSettings.invalidate();
                                finish();
                                break;
                            case SAVE_DRAFT:
                                if (reply.getText().length() > 0) {
                                    QueuedDataSource.getInstance(context).createDraft(reply.getText().toString(), currentAccount);
                                    Toast.makeText(context, getResources().getString(R.string.saved_draft), Toast.LENGTH_SHORT).show();
                                    reply.setText("");
                                    finish();
                                } else {
                                    Toast.makeText(context, getResources().getString(R.string.no_tweet), Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case VIEW_DRAFTS:
                                final String[] drafts = QueuedDataSource.getInstance(context).getDrafts();
                                if (drafts.length > 0) {
                                    final String[] draftsAndDelete = new String[drafts.length + 1];
                                    draftsAndDelete[0] = getString(R.string.delete_all);
                                    for (int i = 1; i < draftsAndDelete.length; i++) {
                                        draftsAndDelete[i] = drafts[i - 1];
                                    }

                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setItems(draftsAndDelete, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int item) {

                                            if (item == 0) {
                                                // clicked the delete all item
                                                new AlertDialog.Builder(context)
                                                        .setMessage(getString(R.string.delete_all) + "?")
                                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                QueuedDataSource.getInstance(context).deleteAllDrafts();
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                        .create()
                                                        .show();

                                                dialog.dismiss();
                                            } else {
                                                new AlertDialog.Builder(context)
                                                        .setTitle(context.getResources().getString(R.string.apply))
                                                        .setMessage(draftsAndDelete[item])
                                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                reply.setText(draftsAndDelete[item]);
                                                                reply.setSelection(reply.getText().length());
                                                                QueuedDataSource.getInstance(context).deleteDraft(draftsAndDelete[item]);
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                        .setNegativeButton(R.string.delete_draft, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                QueuedDataSource.getInstance(context).deleteDraft(draftsAndDelete[item]);
                                                                dialogInterface.dismiss();
                                                            }
                                                        })
                                                        .create()
                                                        .show();

                                                dialog.dismiss();
                                            }
                                        }
                                    });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                } else {
                                    Toast.makeText(context, R.string.no_drafts, Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case SCHEDULE:
                                Intent schedule = new Intent(context, ViewScheduledTweets.class);
                                if (!reply.getText().toString().isEmpty()) {
                                    schedule.putExtra("has_text", true);
                                    schedule.putExtra("text", reply.getText().toString());
                                }
                                startActivity(schedule);
                                finish();
                                break;
                            case VIEW_QUEUE:
                                final String[] queued = QueuedDataSource.getInstance(context).getQueuedTweets(currentAccount);
                                if (queued.length > 0) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setItems(queued, new DialogInterface.OnClickListener() {
                                        public void onClick(final DialogInterface dialog, final int item) {

                                            new AlertDialog.Builder(context)
                                                    .setTitle(context.getResources().getString(R.string.keep_queued_tweet))
                                                    .setMessage(queued[item])
                                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                        }
                                                    })
                                                    .setNegativeButton(R.string.delete_draft, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            QueuedDataSource.getInstance(context).deleteQueuedTweet(queued[item]);
                                                            dialogInterface.dismiss();
                                                        }
                                                    })
                                                    .create()
                                                    .show();

                                            dialog.dismiss();
                                        }
                                    });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                } else {
                                    Toast.makeText(context, R.string.no_queued, Toast.LENGTH_SHORT).show();
                                }
                                break;
                        }

                        return false;
                    }
                });

                menu.show();
            }
        });

        final ImageButton convertToDM = (ImageButton) findViewById(R.id.dm_button);
        convertToDM.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(context, R.string.menu_direct_message, Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        convertToDM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent dm = new Intent(context, ComposeDMActivity.class);
                startActivity(dm);
                overridePendingTransition(0,0);
                finish();
                overridePendingTransition(0,0);
            }
        });

        final ImageButton location = (ImageButton) findViewById(R.id.location);
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!addLocation) {
                    sharedPrefs.edit().putBoolean("share_location", true).apply();
                    addLocation = true;

                    location.setColorFilter(settings.themeColors.accentColor);
                } else {
                    sharedPrefs.edit().putBoolean("share_location", false).apply();
                    addLocation = false;

                    location.clearColorFilter();
                }
            }
        });

        if (sharedPrefs.getBoolean("share_location", false)) {
            location.performClick();
        }

        if (!settings.useEmoji) {
            emojiButton.setVisibility(View.GONE);
        } else {
            emojiKeyboard.setAttached((FontPrefEditText) reply);

            reply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button_changing});
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
                                        INPUT_METHOD_SERVICE);
                                imm.showSoftInput(reply, 0);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button_changing});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    } else {
                        InputMethodManager imm = (InputMethodManager)getSystemService(
                                INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(reply.getWindowToken(), 0);

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                emojiKeyboard.setVisibility(true);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.keyboard_button_changing});
                        int resource = a.getResourceId(0, 0);
                        a.recycle();
                        emojiButton.setImageResource(resource);
                    }
                }
            });
        }
    }

    public void attachImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(R.array.attach_options, new DialogInterface.OnClickListener() {
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
                        int permission = ContextCompat.checkSelfPermission(ComposeActivity.this,
                                Manifest.permission.CAMERA);
                        if (permission == PackageManager.PERMISSION_DENIED) {
                            PermissionModelUtils utils = new PermissionModelUtils(context);
                            utils.requestCameraPermission();
                        }

                        permission = ContextCompat.checkSelfPermission(ComposeActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (permission == PackageManager.PERMISSION_DENIED) {
                            PermissionModelUtils utils = new PermissionModelUtils(context);
                            utils.requestStoragePermission();
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
                    Toast.makeText(ComposeActivity.this, "GIFs must be less than 5 MB", Toast.LENGTH_SHORT).show();

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
                    try {
                        new MaterialCamera(ComposeActivity.this)
                                .saveDir(getFilesDir().getPath())
                                .qualityProfile(MaterialCamera.QUALITY_480P)
                                .allowRetry(false)
                                .autoSubmit(true)
                                .primaryColor(AppSettings.getInstance(ComposeActivity.this).themeColors.primaryColor)
                                .showPortraitWarning(false)
                                .maxAllowedFileSize(14 * 1024 * 1024)
                                .start(CAPTURE_VIDEO);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (item == 4) {
                    try {
                        Intent gifIntent = new Intent();
                        gifIntent.setType("video/mp4");
                        gifIntent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(gifIntent, SELECT_VIDEO);
                    } catch (Exception e) {
                        Intent gifIntent = new Intent();
                        gifIntent.setType("video/mp4");
                        gifIntent.setAction(Intent.ACTION_PICK);
                        startActivityForResult(gifIntent, SELECT_VIDEO);
                    }
                }
            }
        });

        builder.create().show();
    }

    public void setUpReplyText() {
        // for failed notification
        if (getIntent().getStringExtra("failed_notification_text") != null) {
            reply.setText(getIntent().getStringExtra("failed_notification_text"));
            reply.setSelection(reply.getText().length());
        }

        to = getIntent().getStringExtra("user") + (isDM ? "" : " ");
        to = to.trim() + " ";

        if ((!to.equals("null ") && !isDM) || (isDM && !to.equals("null"))) {
            if(!isDM) {
                Log.v("username_for_noti", "to place: " + to);
                if (to.contains("/status/")) {
                    // quoting a tweet
                    quotingAStatus = to;
                    attachmentUrl = to;
                    reply.setText("");
                    reply.setSelection(0);
                } else {
                    reply.setText(to);
                    reply.setSelection(reply.getText().toString().length());
                }
            } else {
                contactEntry.setText(to);
                reply.requestFocus();
            }

            sharedPrefs.edit().putString("draft", "").apply();
        }

        notiId = getIntent().getLongExtra("id", 0);
        replyText = getIntent().getStringExtra("reply_to_text");

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            sharingSomething = true;
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    replyText = "";
                }
            }, 1000);
        }
    }

    public boolean doneClick() {

        if (emojiKeyboard.isShowing()) {
            emojiKeyboard.setVisibility(false);

            TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            emojiButton.setImageResource(resource);
        }

        EditText editText = (EditText) findViewById(R.id.tweet_content);
        final String status = editText.getText().toString();

        if (!Utils.hasInternetConnection(context) && !status.isEmpty() && imagesAttached == 0) {
            // we are going to queue this tweet to send for when they get a connection
            QueuedDataSource.getInstance(context).createQueuedTweet(status, currentAccount);
            Toast.makeText(context, R.string.tweet_queued, Toast.LENGTH_SHORT).show();
            return true;
        } else if (!Utils.hasInternetConnection(context) && imagesAttached > 0) {
            // we only queue tweets without pictures
            Toast.makeText(context, R.string.only_queue_no_pic, Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for blank text
        if (Integer.parseInt(charRemaining.getText().toString()) >= 0 || settings.twitlonger) {
            // update status
            doneClicked = true;
            sendStatus(status, Integer.parseInt(charRemaining.getText().toString()));
            return true;
        } else {
            if (editText.getText().length() + (attachedUri.equals("") ? 0 : 22) <= AppSettings.getInstance(this).tweetCharacterCount) {
                // EditText is empty
                Toast.makeText(context, context.getResources().getString(R.string.error_sending_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    public void sendStatus(String status, int length) {
        new UpdateTwitterStatus(reply.getText().toString(), length).execute(status);
    }

    @Override
    public void onPause() {
        sharedPrefs.edit().putString("draft", "").apply();
        try {
            if (!(doneClicked || discardClicked)) {
                QueuedDataSource.getInstance(context).createDraft(reply.getText().toString(), currentAccount);
            }
        } catch (Exception e) {
            // it is a direct message
        }

        super.onPause();
    }

}
