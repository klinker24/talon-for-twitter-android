/*
 * Copyright 2013 Jacob Klinker
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

package com.klinker.android.twitter.ui.scheduled_tweets;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.ScheduledTweet;
import com.klinker.android.twitter.manipulations.EmojiKeyboard;
import com.klinker.android.twitter.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter.services.SendScheduledTweet;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.Utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewScheduledSmsActivity extends Activity {

    private Context context;

    private ListPopupWindow lpw;

    private Date setDate;

    private int currentYear;
    private int currentMonth;
    private int currentDay;
    private int currentHour;
    private int currentMinute;

    private int setYear = -1;
    private int setMonth = -1;
    private int setDay = -1;
    private int setHour = -1;
    private int setMinute = -1;

    static final int DATE_DIALOG_ID = 0;
    static final int TIME_DIALOG_ID = 1;

    private Button btDate;
    private Button btTime;

    private TextView timeDisplay;
    private TextView dateDisplay;

    private EditText mEditText;
    private ImageButton emojiButton;
    private EmojiKeyboard emojiKeyboard;

    public SharedPreferences sharedPrefs;

    private boolean timeDone = false;

    public Date currentDate;

    String startDate;
    String startMessage;

    private AppSettings settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = AppSettings.getInstance(this);

        Utils.setUpTheme(this, settings);

        setContentView(R.layout.scheduled_new_tweet_activity);

        Intent intent = getIntent();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        context = this;

        mEditText = (EditText) findViewById(R.id.messageEntry2);
        emojiButton = (ImageButton) findViewById(R.id.emojiButton);
        emojiKeyboard = (EmojiKeyboard) findViewById(R.id.emojiKeyboard);

        if (!sharedPrefs.getBoolean("keyboard_type", true)) {
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            mEditText.setImeOptions(EditorInfo.IME_ACTION_NONE);
        }

        startDate = intent.getStringExtra(ScheduledActivity.EXTRA_TIME);
        startMessage = intent.getStringExtra(ScheduledActivity.EXTRA_TEXT);

        if (startDate == null) {
            startDate = "";
        }

        if (startMessage == null) {
            startMessage = "";
        }

        final Calendar c = Calendar.getInstance();
        currentYear = c.get(Calendar.YEAR);
        currentMonth = c.get(Calendar.MONTH);
        currentDay = c.get(Calendar.DAY_OF_MONTH);
        currentHour = c.get(Calendar.HOUR_OF_DAY);
        currentMinute = c.get(Calendar.MINUTE);

        currentDate = new Date(currentYear, currentMonth, currentDay, currentHour, currentMinute);

        timeDisplay = (TextView) findViewById(R.id.currentTime);
        dateDisplay = (TextView) findViewById(R.id.currentDate);
        btDate = (Button) findViewById(R.id.setDate);
        btTime = (Button) findViewById(R.id.setTime);

        if (!startDate.equals("") && !startDate.equals("null")) {
            setDate = new Date(Long.parseLong(startDate));
            timeDone = true;
            btTime.setEnabled(true);
        } else {
            btTime.setEnabled(false);
        }

        mEditText.setText(startMessage);

        if (!startDate.equals("")) {
            Date startDateObj = new Date(Long.parseLong(startDate));
            if (sharedPrefs.getBoolean("hour_format", false)) {
                timeDisplay.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(startDateObj));
            } else {
                timeDisplay.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(startDateObj));
            }

            if (sharedPrefs.getBoolean("hour_format", false)) {
                dateDisplay.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(startDateObj));
            } else {
                dateDisplay.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(startDateObj));
            }
        }

        if (!settings.useEmoji) {
            emojiButton.setVisibility(View.GONE);
        } else {
            emojiKeyboard.setAttached((HoloEditText) mEditText);

            mEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (emojiKeyboard.isShowing()) {
                        emojiKeyboard.setVisibility(false);
                    }
                }
            });

            emojiButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                    emojiKeyboard.toggleVisibility();

                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboard.getLayoutParams();
                    params.topMargin = mEditText.getHeight();
                    emojiKeyboard.setLayoutParams(params);
                }
            });
        }

        // sets the date button listener to call the dialog
        btDate.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                com.android.datetimepicker.date.DatePickerDialog.newInstance(reservationDate, currentYear, currentMonth, currentDay, settings.theme != AppSettings.THEME_LIGHT)
                        .show(getFragmentManager(), "date_picker");
                btTime.setEnabled(true);
            }
        });

        // sets the time button listener to call the dialog
        btTime.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                com.android.datetimepicker.time.TimePickerDialog dialog = com.android.datetimepicker.time.TimePickerDialog.newInstance(timeDate, currentHour, currentMinute, settings.militaryTime);
                if (settings.theme != AppSettings.THEME_LIGHT) {
                    dialog.setThemeDark(true);
                }
                dialog.show(getFragmentManager(), "time_picker");
            }
        });

        // Inflate a "Done/Discard" custom action bar view.
        LayoutInflater inflater = (LayoutInflater) getActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);

        final View customActionBarView = inflater.inflate(
                R.layout.actionbar_done_discard, null);

        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doneClick();
                    }
                });

        customActionBarView.findViewById(R.id.actionbar_discard).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        discardClick();
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

    // To-do: make a date object to display in different time formats, check out the messageArrayAdapter class, it works with the dates
    // gets the date text from what is entered in the dialog and displays it
    private com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener reservationDate = new com.android.datetimepicker.date.DatePickerDialog.OnDateSetListener() {
        public void onDateSet(com.android.datetimepicker.date.DatePickerDialog view, int year, int month, int day) {
            setYear = year;
            setMonth = month;
            setDay = day;

            if (setHour != -1 && setMinute != -1) {
                setDate = new Date(setYear - 1900, setMonth, setDay, setHour, setMinute);

                if (settings.militaryTime) {
                    dateDisplay.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(setDate));
                } else {
                    dateDisplay.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(setDate));
                }
            } else {
                setDate = new Date(setYear - 1900, setMonth, setDay);

                if (settings.militaryTime) {
                    dateDisplay.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(setDate));
                } else {
                    dateDisplay.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(setDate));
                }
            }
            //dateDisplay.setText((month + 1) + "/" + day + "/" + year);
        }

    };

    // gets the time text from what is entered in the dialog and displays it
    private com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener timeDate = new com.android.datetimepicker.time.TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(RadialPickerLayout view, int hours, int minutes) {
            setHour = hours;
            setMinute = minutes;

            setDate.setHours(setHour);
            setDate.setMinutes(setMinute);

            currentDate.setYear(currentYear - 1900);

            if (!setDate.before(currentDate)) {
                if (settings.militaryTime) {
                    timeDisplay.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.GERMAN).format(setDate));
                } else {
                    timeDisplay.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(setDate));
                }

                timeDone = true;
            } else {
                Context context = getApplicationContext();
                CharSequence text = "Date must be forward!";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                btTime.setEnabled(false);

                timeDisplay.setText("");
                dateDisplay.setText("");

                timeDone = false;
            }

        }
    };

    // finishes the activity when the discard button is clicked, without making any changes or saving anything
    public boolean discardClick() {
        finish();
        return true;
    }

    // this is where we will set everything up when the user has entered all the information
    // including the alarm manager and writing the files to the database to save them
    public boolean doneClick() {
        if (!mEditText.getText().toString().equals("") && timeDone) {
            int alarmIdNum = sharedPrefs.getInt("scheduled_alarm_id", 400);
            alarmIdNum++;

            SharedPreferences.Editor prefEdit = sharedPrefs.edit();
            prefEdit.putInt("scheduled_alarm_id", alarmIdNum);
            prefEdit.commit();

            ScheduledTweet tweet = new ScheduledTweet(mEditText.getText().toString(), alarmIdNum, setDate.getTime());

        } else {
            Context context = getApplicationContext();
            CharSequence text = "Please complete the form!";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        return true;
    }

    public void createAlarm(int alarmId) {
        Intent serviceIntent = new Intent(getApplicationContext(), SendScheduledTweet.class);

        serviceIntent.putExtra(ScheduledActivity.EXTRA_TEXT, mEditText.getText().toString());

        PendingIntent pi = getDistinctPendingIntent(serviceIntent, alarmId);

        // Schedule the alarm!
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        am.set(AlarmManager.RTC_WAKEUP,
                setDate.getTime(),
                pi);
    }

    protected PendingIntent getDistinctPendingIntent(Intent intent, int requestId) {
        PendingIntent pi =
                PendingIntent.getService(
                        this,
                        requestId,
                        intent,
                        0);

        return pi;
    }

    @Override
    public void onBackPressed() {
        if (emojiKeyboard.isShowing()) {
            emojiKeyboard.setVisibility(false);
            return;
        }

        super.onBackPressed();
    }
}