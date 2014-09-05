package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;

import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.twitter_l.utils.AutoCompleteHelper;

public class AutoCompletePeopleAdapter extends SearchedPeopleCursorAdapter {

    private AutoCompleteHelper helper;

    public AutoCompletePeopleAdapter(Context context, Cursor cursor, EditText text) {
        super(context, cursor, text);
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

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.userId == id) {
                    loadImage(context, holder, url, mCache, id);
                }
            }
        }, 500);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.completeTweet(text, screenName, '@');
            }
        });
    }
}
