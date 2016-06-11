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

package com.klinker.android.twitter_l.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.Tweet;
import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.glide.CircleBitmapTransform;
import com.klinker.android.twitter_l.utils.text.TextUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new WidgetViewsFactory(this.getApplicationContext(), intent);
    }
}

class WidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private int mCount;
    private List<Tweet> mWidgetItems = new ArrayList<Tweet>();
    private Context mContext;
    private boolean darkTheme;
    private AppSettings settings;

    public WidgetViewsFactory(Context context, Intent intent) {
        mContext = context;
        darkTheme = Integer.parseInt(context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE).getString("theme", "1")) != 0;

        settings = AppSettings.getInstance(context);
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public RemoteViews getViewAt(int arg0) {
        int res = 0;
        switch (Integer.parseInt(mContext.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE).getString("widget_theme", "4"))) {
            case 0:
                res = R.layout.widget_conversation_light;
                break;
            case 1:
                res = R.layout.widget_conversation_dark;
                break;
            case 2:
                res = R.layout.widget_conversation_light;
                break;
            case 3:
                res = R.layout.widget_conversation_dark;
                break;
            case 4:
                res = R.layout.widget_conversation_light;
                break;
            case 5:
                res = R.layout.widget_conversation_dark;
                break;
        }

        final RemoteViews card = new RemoteViews(mContext.getPackageName(), res);

        try {
            card.setTextViewText(R.id.contactName, settings.displayScreenName ? "@" + mWidgetItems.get(arg0).getScreenName() : mWidgetItems.get(arg0).getName());
            card.setTextViewText(R.id.contactText, TextUtils.colorText(mContext, mWidgetItems.get(arg0).getTweet(), settings.themeColors.accentColor));
            card.setTextViewText(R.id.time, Utils.getTimeAgo(mWidgetItems.get(arg0).getTime(), mContext));

            if (mContext.getResources().getBoolean(R.bool.expNotifications)) {
                try {
                    card.setTextViewTextSize(R.id.contactName, TypedValue.COMPLEX_UNIT_DIP, settings.textSize + 2);
                    card.setTextViewTextSize(R.id.contactText, TypedValue.COMPLEX_UNIT_DIP, settings.textSize);
                    card.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_DIP, settings.textSize - 2);
                } catch (Throwable t) {

                }
            }

            final int arg = arg0;

            card.setImageViewBitmap(R.id.contactPicture, getCachedPic(mWidgetItems.get(arg).getPicUrl()));

            String picUrl = mWidgetItems.get(arg0).getWebsite();
            String otherUrl = mWidgetItems.get(arg0).getOtherWeb();
            String link;

            boolean displayPic = !picUrl.equals("") && !picUrl.contains("youtube");
            if (displayPic) {
                link = picUrl;
                card.setViewVisibility(R.id.picture, View.VISIBLE);
                card.setImageViewBitmap(R.id.picture, getCachedPic(link));
            } else {
                link = otherUrl.split("  ")[0];
                card.setViewVisibility(R.id.picture, View.GONE);
            }

            Bundle extras = new Bundle();
            extras.putString("name", mWidgetItems.get(arg0).getName());
            extras.putString("screenname", mWidgetItems.get(arg0).getScreenName());
            extras.putLong("time", mWidgetItems.get(arg0).getTime());
            extras.putString("tweet", mWidgetItems.get(arg0).getTweet());
            extras.putString("retweeter", mWidgetItems.get(arg0).getRetweeter());
            extras.putString("webpage", link);
            extras.putBoolean("picture", displayPic);
            extras.putString("other_links", mWidgetItems.get(arg0).getOtherWeb());
            extras.putLong("tweetid", mWidgetItems.get(arg0).getId());
            extras.putString("propic", mWidgetItems.get(arg0).getPicUrl());
            extras.putString("users", mWidgetItems.get(arg0).getUsers());
            extras.putString("hashtags", mWidgetItems.get(arg0).getHashtags());

            // also have to add the strings in the widget provider

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
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public void onDataSetChanged() {
        mWidgetItems = new ArrayList<Tweet>();
        AppSettings settings = AppSettings.getInstance(mContext);
        Cursor query;

        if (!settings.useMentionsOnWidget) {
            HomeDataSource data = HomeDataSource.getInstance(mContext);
            query = data.getWidgetCursor(settings.widgetAccountNum);
        } else {
            MentionsDataSource data = MentionsDataSource.getInstance(mContext);
            query = data.getWidgetCursor(settings.widgetAccountNum);
        }

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
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_URL)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_USERS)),
                            query.getString(query.getColumnIndex(HomeSQLiteHelper.COLUMN_HASHTAGS))
                    ));
                } while (query.moveToNext());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            query.close();
        } catch (Exception e) {

        }

        Log.v("talon_widget", "size of " + mWidgetItems.size());

        mCount = mWidgetItems.size();
    }

    @Override
    public void onDestroy() {

    }

    public Bitmap getCachedPic(String url) {
        try {
            /*return ImageUtils.getCircleBitmap(Glide.
                    with(mContext).
                    load(url).
                    asBitmap().
                    into(200, 200).
                    get());*/
            return Glide.with(mContext)
                    .load(url)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    //.transform(new CircleBitmapTransform(mContext))
                    .into(200,200)
                    .get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}