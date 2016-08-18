package com.klinker.android.twitter_l.services;
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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.utils.IOUtils;

import android.content.pm.PackageInfo;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.redirects.RedirectToPlayStore;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

public class TrimDataService extends KillerIntentService {

    SharedPreferences sharedPrefs;

    public static final int TRIM_ID = 161;

    public TrimDataService() {
        super("TrimDataService");
    }

    @Override
    public void handleIntent(Intent intent) {
        Log.v("trimming_database", "trimming database from service");
        IOUtils.trimDatabase(getApplicationContext(), 1); // trims first account
        IOUtils.trimDatabase(getApplicationContext(), 2); // trims second account

        getContentResolver().notifyChange(HomeContentProvider.CONTENT_URI, null);

        scheduleRefresh(this, 60 * 24); // every 24 hours
    }

    public static void scheduleRefresh(Context context, long mins) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long now = new Date().getTime();
        long alarm = now + (1000 * 60 * mins);

        Log.v("alarm_date", "auto trim " + new Date(alarm).toString());

        PendingIntent pendingIntent = PendingIntent.getService(context, TrimDataService.TRIM_ID, new Intent(context, TrimDataService.class), 0);

        am.cancel(pendingIntent);
        am.set(AlarmManager.RTC_WAKEUP, alarm, pendingIntent);
    }
}
