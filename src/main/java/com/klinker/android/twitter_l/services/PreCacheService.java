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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.Utils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;


public class PreCacheService extends IntentService {

    private static final boolean DEBUG = false;

    SharedPreferences sharedPrefs;

    public PreCacheService() {
        super("PreCacheService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        if (DEBUG) {
            Log.v("talon_pre_cache", "starting the service, current time: " + Calendar.getInstance().getTime().toString());
        }

        // if they want it only over wifi and they are on mobile data
        if (getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE)
                .getBoolean("pre_cache_wifi_only", false) &&
                Utils.getConnectionStatus(this)) {

            if (DEBUG) {
                Log.v("talon_pre_cache", "quit for connection");
            }

            // just quit because we don't want it to happen
            return;
        }

        AppSettings settings = AppSettings.getInstance(this);
        Cursor cursor = HomeDataSource.getInstance(this).getUnreadCursor(settings.currentAccount);

        if (cursor.moveToFirst()) {
            if (DEBUG) {
                Log.v("talon_pre_cache", "found database and moved to first picture. cursor size: " + cursor.getCount());
            }
            boolean cont = true;
            do {
                String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
                String imageUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));
                // image url can contain spaces, which means there are multiple pictures

                Glide.with(this).load(profilePic).downloadOnly(1000, 1000);
                Glide.with(this).load(imageUrl).downloadOnly(1000, 1000);

            } while (cursor.moveToNext() && cont);

            if (DEBUG) {
                Log.v("talon_pre_cache", "done with service. time: " + Calendar.getInstance().getTime().toString());
            }
        }
    }
}
