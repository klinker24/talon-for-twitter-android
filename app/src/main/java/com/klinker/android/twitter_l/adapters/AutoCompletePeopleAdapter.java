package com.klinker.android.twitter_l.adapters;
/*
 * Copyright 2013 Luke Klinker
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
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;
import android.widget.ListPopupWindow;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.twitter_l.utils.AutoCompleteHelper;

public class AutoCompletePeopleAdapter extends SearchedPeopleCursorAdapter {

    private AutoCompleteHelper helper;
    private ListPopupWindow listPopupWindow;

    public AutoCompletePeopleAdapter(ListPopupWindow listPopupWindow, Context context, Cursor cursor, EditText text) {
        super(context, cursor, text);
        this.listPopupWindow = listPopupWindow;
        helper = new AutoCompleteHelper();
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String name = cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_NAME));
        final String screenName = cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME));
        final String url = cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_PRO_PIC));
        final long id = cursor.getLong(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_ID));
        holder.userId = id;

        if (holder.divider != null && holder.divider.getVisibility() == View.VISIBLE) {
            holder.divider.setVisibility(View.GONE);
        }

        holder.name.setText(name);
        holder.screenName.setText("@" + screenName);

        Glide.with(context).load(url).into(holder.picture);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.completeTweet(text, screenName, '@');
                if (listPopupWindow != null && listPopupWindow.isShowing()) {
                    listPopupWindow.dismiss();
                }
            }
        });
    }
}
