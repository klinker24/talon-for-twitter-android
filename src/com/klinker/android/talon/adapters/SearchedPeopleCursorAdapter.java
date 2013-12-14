package com.klinker.android.talon.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.talon.ui.widgets.HoloEditText;

public class SearchedPeopleCursorAdapter extends PeopleCursorAdapter {

    private HoloEditText text;

    public SearchedPeopleCursorAdapter(Context context, Cursor cursor, HoloEditText text) {
        super(context, cursor);
        this.text = text;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v;
        v = inflater.inflate(R.layout.person_no_background, viewGroup, false);
        final ViewHolder holder;

        holder = new ViewHolder();

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenName = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (NetworkedCacheableImageView) v.findViewById(R.id.profile_pic);

        // sets up the font sizes
        holder.name.setTextSize(settings.textSize + 4);
        holder.screenName.setTextSize(settings.textSize);
        v.setTag(holder);
        return v;
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

        holder.picture.loadImage(url, true, null, NetworkedCacheableImageView.CIRCLE);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                text.setText("@" + screenName);
                text.setSelection(text.getText().length());

            }
        });
    }
}
