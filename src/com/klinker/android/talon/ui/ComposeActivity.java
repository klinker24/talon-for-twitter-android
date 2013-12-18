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
import android.widget.Button;
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
import com.klinker.android.talon.ui.widgets.QustomDialogBuilder;
import com.klinker.android.talon.utils.EmojiUtils;
import com.klinker.android.talon.utils.IOUtils;
import com.klinker.android.talon.utils.Utils;

import java.io.File;
import java.io.IOException;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.co.senab.photoview.PhotoViewAttacher;


public class ComposeActivity extends Compose {

    public void setUpLayout() {
        setContentView(R.layout.compose_activity);

        setUpSimilar();
        setUpToastBar();

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

    }

    public boolean doneClick() {
        EditText editText = (EditText) findViewById(R.id.tweet_content);
        String status = editText.getText().toString();

        // Check for blank text
        if ((status.trim().length() > 0 || !attachedFilePath.equals("")) && editText.getText().length() < 140) {
            // update status
            doneClicked = true;
            sendStatus(status);
            return true;
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

    public void sendStatus(String status) {
        new updateTwitterStatus(reply.getText().toString()).execute(status);
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