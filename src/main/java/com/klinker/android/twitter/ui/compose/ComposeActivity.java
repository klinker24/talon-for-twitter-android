package com.klinker.android.twitter.ui.compose;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.AutoCompetePeopleAdapter;
import com.klinker.android.twitter.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter.data.sq_lite.QueuedDataSource;
import com.klinker.android.twitter.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter.manipulations.QustomDialogBuilder;
import com.klinker.android.twitter.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter.ui.scheduled_tweets.ViewScheduledTweets;
import com.klinker.android.twitter.utils.IOUtils;
import com.klinker.android.twitter.utils.Utils;
import com.klinker.android.twitter.utils.text.TextUtils;

import java.io.File;
import java.io.IOException;

import uk.co.senab.photoview.PhotoViewAttacher;


public class ComposeActivity extends Compose {

    @Override
    public void onDestroy() {
        try {
            ((AutoCompetePeopleAdapter)autocomplete.getListView().getAdapter()).getCursor().close();
        } catch (Exception e) {

        }
        super.onDestroy();
    }

    public void setUpLayout() {
        setContentView(R.layout.compose_activity);

        setUpSimilar();
        setUpToastBar();

        selectAccounts = (LinearLayout) findViewById(R.id.select_account);
        accountOneCheck = (CheckBox) findViewById(R.id.account_one_check);
        accountTwoCheck = (CheckBox) findViewById(R.id.account_two_check);
        accountOneName = (HoloTextView) findViewById(R.id.account_one_name);
        accountTwoName = (HoloTextView) findViewById(R.id.account_two_name);

        int count = 0; // number of accounts logged in

        if (sharedPrefs.getBoolean("is_logged_in_1", false)) {
            count++;
        }

        if (sharedPrefs.getBoolean("is_logged_in_2", false)) {
            count++;
        }

        if (count == 2) {
            selectAccounts.setVisibility(View.VISIBLE);
            accountOneName.setText("@" + settings.myScreenName);
            accountTwoName.setText("@" + settings.secondScreenName);

            if (settings.addonTheme) {
                try {
                    Resources resourceAddon = context.getPackageManager().getResourcesForApplication(settings.addonThemePackage);
                    int back = resourceAddon.getIdentifier("checkmark_background", "drawable", settings.addonThemePackage);
                    accountOneCheck.setBackgroundDrawable(resourceAddon.getDrawable(back));
                    accountTwoCheck.setBackgroundDrawable(resourceAddon.getDrawable(back));
                } catch (Exception e) {
                    // theme does not include a reply entry box
                }
            }
        }

        autocomplete = new ListPopupWindow(context);
        autocomplete.setAnchorView(findViewById(R.id.prompt_pos));
        autocomplete.setHeight(toDP(150));
        autocomplete.setWidth(toDP(275));
        autocomplete.setAdapter(new AutoCompetePeopleAdapter(context,
                FollowersDataSource.getInstance(context).getCursor(currentAccount, reply.getText().toString()), reply));
        autocomplete.setPromptPosition(ListPopupWindow.POSITION_PROMPT_ABOVE);

        autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                autocomplete.dismiss();
            }
        });

        reply.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                String searchText = reply.getText().toString();

                try {
                    if (searchText.substring(searchText.length() - 1, searchText.length()).equals("@")) {
                        autocomplete.show();

                    } else if (searchText.substring(searchText.length() - 1, searchText.length()).equals(" ")) {
                        autocomplete.dismiss();
                    } else if (autocomplete.isShowing()) {
                        String[] split = reply.getText().toString().split(" ");
                        String adapterText;
                        if (split.length > 1) {
                            adapterText = split[split.length - 1];
                        } else {
                            adapterText = split[0];
                        }
                        adapterText = adapterText.replace("@", "");
                        autocomplete.setAdapter(new AutoCompetePeopleAdapter(context,
                                FollowersDataSource.getInstance(context).getCursor(currentAccount, adapterText), reply));
                    }
                } catch (Exception e) {
                    // there is no text
                    try {
                        autocomplete.dismiss();
                    } catch (Exception x) {
                        // something went really wrong i guess haha
                    }
                }

            }
        });

        mAttacher = new PhotoViewAttacher(attachImage);

        overflow = (ImageButton) findViewById(R.id.overflow_button);
        overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout buttons = (LinearLayout) findViewById(R.id.buttons);
                if (buttons.getVisibility() == View.VISIBLE) {

                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.compose_rotate_back);
                    ranim.setFillAfter(true);
                    overflow.startAnimation(ranim);

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);

                    buttons.setVisibility(View.GONE);
                } else {
                    buttons.setVisibility(View.VISIBLE);

                    Animation ranim = AnimationUtils.loadAnimation(context, R.anim.compose_rotate);
                    ranim.setFillAfter(true);
                    overflow.startAnimation(ranim);

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);
                }
            }
        });

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imagesAttached > 0 && !sharedPrefs.getBoolean("know_twitpic_for_mult_attach", false)) {
                    new AlertDialog.Builder(context)
                            .setTitle(context.getResources().getString(R.string.twitpic_disclaimer))
                            .setMessage(getResources().getString(R.string.twitpic_disclaimer_multi_summary))
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    attachImage();
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNeutralButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    sharedPrefs.edit().putBoolean("know_twitpic_for_mult_attach", true).commit();
                                    attachImage();
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
                } else {
                    attachImage();
                }

            }
        });

        ImageButton at = (ImageButton) findViewById(R.id.at_button);
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
                        reply.append(qustomDialogBuilder.text.getText().toString());
                    }
                });

                qustomDialogBuilder.show();

                overflow.performClick();
            }
        });

        final ImageButton location = (ImageButton) findViewById(R.id.location);
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!addLocation) {
                    Toast.makeText(context, getResources().getString(R.string.location_connected), Toast.LENGTH_SHORT).show();

                    addLocation = true;

                    location.setImageDrawable(getResources().getDrawable(R.drawable.ic_cancel_light));
                } else {
                    Toast.makeText(context, getResources().getString(R.string.location_disconnected), Toast.LENGTH_SHORT).show();

                    addLocation = false;

                    location.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_place_light));
                }
            }
        });

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
                                        INPUT_METHOD_SERVICE);
                                imm.showSoftInput(reply, 0);
                            }
                        }, 250);

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.emoji_button});
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

                        TypedArray a = getTheme().obtainStyledAttributes(new int[]{R.attr.keyboardButton});
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

                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    try {
                        startActivityForResult(captureIntent, CAPTURE_IMAGE);
                    } catch (Throwable t) {
                        // no app to preform this..? hmm, tell them that I guess
                        Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                    }
                } else { // attach picture

                    if (Build.VERSION.SDK_INT < 19) {
                        Intent photoPickerIntent = new Intent();
                        photoPickerIntent.setType("image/*");
                        photoPickerIntent.setAction(Intent.ACTION_GET_CONTENT);
                        try {
                            startActivityForResult(Intent.createChooser(photoPickerIntent,
                                    "Select Picture"), SELECT_PHOTO);
                        } catch (Throwable t) {
                            // no app to preform this..? hmm, tell them that I guess
                            Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        try {
                            startActivityForResult(Intent.createChooser(intent,
                                    "Select Picture"), SELECT_PHOTO);
                        } catch (Throwable t) {
                            // no app to preform this..? hmm, tell them that I guess
                            Toast.makeText(context, "No app available to select pictures!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                overflow.performClick();
            }
        });

        builder.create().show();
    }

    public void setUpReplyText() {
        // for failed notification
        if (!sharedPrefs.getString("draft", "").equals("")) {
            reply.setText(sharedPrefs.getString("draft", ""));
            reply.setSelection(reply.getText().length());
        }

        String to = getIntent().getStringExtra("user") + (isDM ? "" : " ");

        if ((!to.equals("null ") && !isDM) || (isDM && !to.equals("null"))) {
            if(!isDM) {
                Log.v("username_for_noti", "to place: " + to);
                reply.setText(to);
                reply.setSelection(reply.getText().toString().length());
            } else {
                contactEntry.setText(to);
                reply.requestFocus();
            }

            sharedPrefs.edit().putString("draft", "").commit();
        }

        notiId = getIntent().getLongExtra("id", 0);

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
            if (Integer.parseInt(charRemaining.getText().toString()) < 0) {
                onBackPressed();
                doneClicked = true;
                sendStatus(status, Integer.parseInt(charRemaining.getText().toString()));
                return true;
            } else {
                doneClicked = true;
                sendStatus(status, Integer.parseInt(charRemaining.getText().toString()));
                return true;
            }
        } else {
            if (editText.getText().length() + (attachedUri.equals("") ? 0 : 22) <= 140) {
                // EditText is empty
                Toast.makeText(context, context.getResources().getString(R.string.error_sending_tweet), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getResources().getString(R.string.tweet_to_long), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    public void sendStatus(String status, int length) {
        new updateTwitterStatus(reply.getText().toString(), length).execute(status);
    }

    @Override
    public void onPause() {
        sharedPrefs.edit().putString("draft", "").commit();
        try {
            if (!(doneClicked || discardClicked)) {
                QueuedDataSource.getInstance(context).createDraft(reply.getText().toString(), currentAccount);
            }
        } catch (Exception e) {
            // it is a direct message
        }

        super.onPause();
    }

    private boolean isToastShowing = false;
    private boolean infoBar = false;

    private View toastBar;
    private TextView toastDescription;
    private TextView toastButton;

    private void setUpToastBar() {
        toastBar = findViewById(R.id.toastBar);
        toastDescription = (TextView) findViewById(R.id.toastDescription);
        toastButton = (TextView) findViewById(R.id.toastButton);

        if (settings.addonTheme) {
            LinearLayout toastBackground = (LinearLayout) findViewById(R.id.toast_background);
            toastBackground.setBackgroundColor(Color.parseColor("#DD" + settings.accentColor));
        }
    }

    private void showToastBar(String description, String buttonText, final long length, final boolean quit, View.OnClickListener listener) {
        toastDescription.setText(description);
        toastButton.setText(buttonText);
        toastButton.setOnClickListener(listener);

        toastBar.setVisibility(View.VISIBLE);

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isToastShowing = true;
                if (quit) {
                    infoBar = true;
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (quit) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            hideToastBar(length);
                            infoBar = false;
                        }
                    }, 3000);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(length);
        toastBar.startAnimation(anim);
    }

    private void hideToastBar(long length) {
        if (!isToastShowing) {
            return;
        }

        Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                isToastShowing = false;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                toastBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        anim.setDuration(length);
        toastBar.startAnimation(anim);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.compose_activity, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_save_draft:
                if (reply.getText().length() > 0) {
                    QueuedDataSource.getInstance(this).createDraft(reply.getText().toString(), currentAccount);
                    Toast.makeText(this, getResources().getString(R.string.saved_draft), Toast.LENGTH_SHORT).show();
                    reply.setText("");
                    finish();
                } else {
                    Toast.makeText(this, getResources().getString(R.string.no_tweet), Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.menu_view_drafts:
                final String[] drafts = QueuedDataSource.getInstance(this).getDrafts();
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
                return true;
            case R.id.menu_schedule_tweet:
                Intent schedule = new Intent(context, ViewScheduledTweets.class);
                if (!reply.getText().toString().isEmpty()) {
                    schedule.putExtra("has_text", true);
                    schedule.putExtra("text", reply.getText().toString());
                }
                startActivity(schedule);
                finish();
                return true;
            case R.id.menu_view_queued:
                final String[] queued = QueuedDataSource.getInstance(this).getQueuedTweets(currentAccount);
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}