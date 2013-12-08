package com.klinker.android.talon.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;

import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.talon.ui.UserProfileActivity;
import com.klinker.android.talon.ui.widgets.HoloEditText;
import com.squareup.picasso.Picasso;

public class SearchedPeopleCursorAdapter extends PeopleCursorAdapter {

    private HoloEditText text;

    public SearchedPeopleCursorAdapter(Context context, Cursor cursor, HoloEditText text) {
        super(context, cursor);
        this.text = text;
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


        Picasso.with(context)
                .load(url)
                .transform(new CircleTransform())
                .into(holder.picture);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text.setText("@" + screenName);
            }
        });
    }
}
