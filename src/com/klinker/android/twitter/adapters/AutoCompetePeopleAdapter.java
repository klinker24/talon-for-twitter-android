package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;

import com.klinker.android.twitter.data.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.twitter.ui.widgets.HoloEditText;
import com.klinker.android.twitter.utils.ImageUtils;

public class AutoCompetePeopleAdapter extends SearchedPeopleCursorAdapter {

    public AutoCompetePeopleAdapter(Context context, Cursor cursor, HoloEditText text) {
        super(context, cursor, text);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String name = cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_NAME));
        final String screenName = cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_SCREEN_NAME));
        final String url = cursor.getString(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_PRO_PIC));
        final long id = cursor.getLong(cursor.getColumnIndex(FavoriteUsersSQLiteHelper.COLUMN_ID));

        holder.name.setText(name);
        holder.screenName.setText("@" + screenName);

        //holder.picture.loadImage(url, true, null, NetworkedCacheableImageView.CIRCLE);
        if(settings.roundContactImages) {
            ImageUtils.loadCircleImage(context, holder.picture, url, mCache);
        } else {
            ImageUtils.loadImage(context, holder.picture, url, mCache);
        }

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentText = text.getText().toString();

                String[] split = currentText.split(" ");

                split[split.length - 1] = "@" + screenName;

                String newText = "";

                for (String s : split) {
                    newText += s + " ";
                }

                text.setText(newText);
                text.setSelection(text.getText().length());
            }
        });
    }
}
