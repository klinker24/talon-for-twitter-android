package com.klinker.android.twitter_l.adapters;
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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.InteractionsSQLiteHelper;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.SDK11;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

public class InteractionsCursorAdapter extends CursorAdapter {

    public Context context;
    public Cursor cursor;
    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;
    public ColorDrawable color;
    public ColorDrawable transparent;

    public Handler mHandler;

    public static class ViewHolder {
        public TextView title;
        public TextView text;
        public ImageView picture;
        public LinearLayout background;
        public String check;
    }

    public InteractionsCursorAdapter(Context context, Cursor cursor) {

        super(context, cursor, 0);

        this.context = context;
        this.cursor = cursor;
        this.inflater = LayoutInflater.from(context);

        settings = AppSettings.getInstance(context);

        mHandler = new Handler();

        setUpLayout();
    }

    public void setUpLayout() {
        layout = R.layout.interaction;
        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.title = (TextView) v.findViewById(R.id.title);
        holder.text = (TextView) v.findViewById(R.id.text);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.picture);

        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String title = cursor.getString(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_TITLE));
        final String text = cursor.getString(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_TEXT));
        final String url = cursor.getString(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_PRO_PIC));
        final int unread = cursor.getInt(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_UNREAD));
        holder.check = title;

        holder.title.setText(Html.fromHtml(title));

        if (!TextUtils.isEmpty(text)) {
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(text);
        } else {
            holder.text.setVisibility(View.GONE);
        }

        Glide.with(context).load(url).into(holder.picture);

        // set the background color
        if (unread == 1) {
            //surfaceView.background.setBackgroundDrawable(color);
            holder.background.setBackgroundDrawable(transparent);
        } else {
            holder.background.setBackgroundDrawable(transparent);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (!cursor.moveToPosition(cursor.getCount() - 1 - position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v;
        if (convertView == null) {

            v = newView(context, cursor, parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.picture.setImageDrawable(null);
        }

        bindView(v, context, cursor);

        return v;
    }
}
