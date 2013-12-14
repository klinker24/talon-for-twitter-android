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

package com.klinker.android.talon.widget;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.klinker.android.talon.R;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.HomeSQLiteHelper;
import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.ImageUtils;
import com.klinker.android.talon.utils.Tweet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.v("talon_widget", "setting factory");
        return new WidgetViewsFactory(this.getApplicationContext(), intent);
    }
}

class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private int mCount;
    private List<Tweet> mWidgetItems = new ArrayList<Tweet>();
    private Context mContext;
    private boolean darkTheme;
    private BitmapLruCache mCache;

    public WidgetViewsFactory(Context context, Intent intent) {
        mContext = context;
        darkTheme = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mContext).getString("theme", "1")) != 0;

        mCache = App.getInstance(context).getBitmapCache();
    }

    @Override
    public void onCreate() {
        Log.v("talon_widget", "oncreate");
        onDataSetChanged();
    }

    @Override
    public int getCount() {
        Log.v("talon_widget", "getting count");
        return mCount;
    }

    @Override
    public RemoteViews getViewAt(int arg0) {
        final RemoteViews card = new RemoteViews(mContext.getPackageName(), darkTheme ? R.layout.widget_conversation_dark : R.layout.widget_conversation_light);

        try {
            Log.v("talon_widget", "starting getviewat");
            card.setTextViewText(R.id.contactName, mWidgetItems.get(arg0).getName());
            card.setTextViewText(R.id.contactText, mWidgetItems.get(arg0).getTweet());
            final int arg = arg0;

            card.setImageViewBitmap(R.id.contactPicture, getCachedPic(mWidgetItems.get(arg).getPicUrl()));

            Bundle extras = new Bundle();
            extras.putString("name", mWidgetItems.get(arg0).getName());
            extras.putString("screenname", mWidgetItems.get(arg0).getScreenName());
            extras.putLong("time", mWidgetItems.get(arg0).getTime());
            extras.putString("tweet", mWidgetItems.get(arg0).getTweet());
            extras.putString("retweeter", mWidgetItems.get(arg0).getRetweeter());
            extras.putString("webpage", mWidgetItems.get(arg0).getWebsite());
            extras.putLong("tweetid", mWidgetItems.get(arg0).getId());
            extras.putString("propic", mWidgetItems.get(arg0).getPicUrl());

            Intent cardFillInIntent = new Intent();
            cardFillInIntent.putExtras(extras);
            card.setOnClickFillInIntent(R.id.widget_card_background, cardFillInIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return card;
    }

    @Override
    public long getItemId(int position) {
        Log.v("talon_widget", "getting item id");
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        Log.v("talon_widget", "getting view type count");
        return 4;
    }

    @Override
    public boolean hasStableIds() {
        Log.v("talon_widget", "getting stable ids");
        return false;
    }

    @Override
    public void onDataSetChanged() {
        Log.v("talon_widget", "on data set changed");
        mWidgetItems = new ArrayList<Tweet>();

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        HomeDataSource data = new HomeDataSource(mContext);
        data.open();
        Cursor query = data.getWidgetCursor(sharedPrefs.getInt("current_account", 1));

        try {
            if (query.moveToFirst()) {
                do {
                    mWidgetItems.add(new Tweet(
                            query.getLong(query.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME)),
                            query.getLong(query.getColumnIndex(HomeSQLiteHelper.COLUMN_TIME)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_RETWEETER)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL))
                    ));
                } while (query.moveToNext());
            }

            query.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.v("talon_widget", "size of " + mWidgetItems.size());

        mCount = mWidgetItems.size();
    }

    @Override
    public void onDestroy() {

    }

    public Bitmap getCachedPic(String url) {
        CacheableBitmapDrawable result = mCache.get(url, null);

        try {
            if (null == result) {
                Log.d("ImageUrlAsyncTask", "Downloading: " + url);

                // The bitmap isn't cached so download from the web
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                InputStream is = new BufferedInputStream(conn.getInputStream());

                // Add to cache
                result = mCache.put(url, is, null);
            } else {
                Log.d("ImageUrlAsyncTask", "Got from Cache: " + url);
            }
        } catch (Exception e) {

        }

        return ImageUtils.getCircle(result.getBitmap());
    }
}