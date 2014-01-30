package com.klinker.android.twitter.ui.compose;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.AutoCompetePeopleAdapter;
import com.klinker.android.twitter.adapters.SearchedPeopleCursorAdapter;
import com.klinker.android.twitter.data.sq_lite.FollowersDataSource;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.ui.widgets.QustomDialogBuilder;

import java.io.File;
import java.io.IOException;

import uk.co.senab.photoview.PhotoViewAttacher;


public class ComposeActivity extends Compose {

    public void setUpLayout() {
        setContentView(R.layout.compose_activity);

        setUpSimilar();
        setUpToastBar();

        autocomplete = new ListPopupWindow(context);
        autocomplete.setAnchorView(findViewById(R.id.prompt_pos));
        autocomplete.setHeight(toDP(110));
        autocomplete.setWidth(toDP(275));
        autocomplete.setAdapter(new AutoCompetePeopleAdapter(context, data.getCursor(currentAccount, reply.getText().toString()), reply));
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
                        autocomplete.setAdapter(new AutoCompetePeopleAdapter(context, data.getCursor(currentAccount, adapterText), reply));
                    }
                } catch (Exception e) {
                    // there is no text
                    autocomplete.dismiss();
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

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_out_right);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);

                    buttons.setVisibility(View.GONE);
                } else {
                    buttons.setVisibility(View.VISIBLE);

                    Animation anim = AnimationUtils.loadAnimation(context, R.anim.slide_in_left);
                    anim.setDuration(300);
                    buttons.startAnimation(anim);
                }
            }
        });

        attachButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                //builder.setTitle(getResources().getString(R.string.open_what) + "?");
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
                            startActivityForResult(captureIntent, CAPTURE_IMAGE);
                        } else { // attach picture
                            if (attachedFilePath == null || attachedFilePath.equals("")) {
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            } else {
                                attachedFilePath = "";
                                attachImage.setImageDrawable(null);
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
                            }
                        }

                        overflow.performClick();
                    }
                });

                builder.create().show();
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
                    //Toast.makeText(context, getResources().getString(R.string.finding_location), Toast.LENGTH_SHORT);
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

    public void setUpReplyText() {
        // for drafts
        if (!sharedPrefs.getString("draft", "").equals("") && !getIntent().getBooleanExtra("failed_notification", false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showToastBar(getResources().getString(R.string.draft_found), getResources().getString(R.string.apply), 300, true, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            reply.setText(sharedPrefs.getString("draft", ""));
                            reply.setSelection(reply.getText().length());
                            hideToastBar(300);
                        }
                    });
                }
            }, 300);
        } else if (getIntent().getBooleanExtra("failed_notification", false)) {
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
        EditText editText = (EditText) findViewById(R.id.tweet_content);
        final String status = editText.getText().toString();

        // Check for blank text
        if (Integer.parseInt(charRemaining.getText().toString()) >= 0 || settings.twitlonger) {
            // update status
            if (Integer.parseInt(charRemaining.getText().toString()) < 0) {
                finish();
                doneClicked = true;
                sendStatus(status, Integer.parseInt(charRemaining.getText().toString()));
                return true;
            } else {
                doneClicked = true;
                sendStatus(status, Integer.parseInt(charRemaining.getText().toString()));
                return true;
            }
        } else {
            if (editText.getText().length() + (attachedFilePath.equals("") ? 0 : 22) <= 140) {
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

        try {
            if (doneClicked || discardClicked) {
                sharedPrefs.edit().putString("draft", "").commit();
            } else {
                sharedPrefs.edit().putString("draft", reply.getText().toString()).commit();
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
}