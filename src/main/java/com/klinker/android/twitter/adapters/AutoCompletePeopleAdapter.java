package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.EditText;

import com.klinker.android.twitter.data.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.twitter.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter.utils.ImageUtils;

public class AutoCompletePeopleAdapter extends SearchedPeopleCursorAdapter {

    private boolean insertSpace;

    public AutoCompletePeopleAdapter(Context context, Cursor cursor, EditText text) {
        super(context, cursor, text);
        this.insertSpace = true;
    }

    public AutoCompletePeopleAdapter(Context context, Cursor cursor, EditText text, boolean insertSpace) {
        super(context, cursor, text);
        this.insertSpace = insertSpace;
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

        //holder.picture.loadImage(url, true, null, NetworkedCacheableImageView.CIRCLE);
        if(settings.roundContactImages) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (holder.userId == id) {
                        loadCircleImage(context, holder, url, mCache, id);
                    }
                }
            }, 500);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (holder.userId == id) {
                        loadImage(context, holder, url, mCache, id);
                    }
                }
            }, 500);
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

                if (!insertSpace) {
                    newText = newText.substring(0, newText.length() - 1);
                }

                text.setText(newText);
                text.setSelection(text.getText().length());
            }
        });
    }
}
