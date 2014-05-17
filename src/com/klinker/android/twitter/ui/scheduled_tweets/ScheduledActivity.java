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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.adapters.ScheduledArrayAdapter;
import com.klinker.android.twitter.data.ScheduledTweet;
import com.klinker.android.twitter.services.SendScheduledTweet;
import com.klinker.android.twitter.utils.IOUtils;

import java.util.ArrayList;

public class ScheduledActivity extends Activity {

    public final static String EXTRA_TIME = "com.klinker.android.twitter.scheduled.TIME";
    public final static String EXTRA_TEXT = "com.klinker.android.twitter.scheduled.TEXT";
    public final static String EXTRA_ALARM_ID = "com.klinker.android..twitter.scheduled.ALARM_ID";

    public static Context context;
    public ListView sms;
    public Button addNew;
    public SharedPreferences sharedPrefs;
    public ArrayList<ScheduledTweet> tweets;

    @Override
    public void setUpView() {
        setContentView(R.layout.scheduled_tweet_viewer);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(5);

        sms = (ListView) findViewById(R.id.smsListView);
        addNew = (Button) findViewById(R.id.addNewButton);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        context = this;

        tweets = IOUtils.readScheduled(this, true);

        ScheduledArrayAdapter adapter = new ScheduledArrayAdapter(this, tweets);
        sms.setAdapter(adapter);
        sms.setStackFromBottom(false);

        sms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                new AlertDialog.Builder(context)
                        .setMessage(context.getResources().getString(R.string.delete))
                        .setPositiveButton(context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                try {
                                    cancelAlarm(tweets.get(i).alarmId);
                                } catch (Exception e) {

                                }

                                tweets.remove(i);

                                IOUtils.writeScheduled(context, text);
                            }
                        })
                        .setNegativeButton(context.getResources().getString(R.string.edit), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        addNew.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(context, NewScheduledSmsActivity.class);

                intent.putExtra(EXTRA_NUMBER, "");
                intent.putExtra(EXTRA_DATE, "");
                intent.putExtra(EXTRA_REPEAT, "0");
                intent.putExtra(EXTRA_MESSAGE, "");

                startActivity(intent);
            }

        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    public void cancelAlarm(int alarmId) {
        Intent serviceIntent = new Intent(getApplicationContext(), SendScheduledTweet.class);

        PendingIntent pi = getDistinctPendingIntent(serviceIntent, alarmId);

        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        am.cancel(pi);
    }

    protected PendingIntent getDistinctPendingIntent(Intent intent, int requestId) {
        PendingIntent pi =
                PendingIntent.getService(
                        this,         //context
                        requestId,    //request id
                        intent,       //intent to be delivered
                        0);

        return pi;
    }
}