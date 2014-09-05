package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.sq_lite.FavoriteUsersSQLiteHelper;

public class SearchedPeopleCursorAdapter extends PeopleCursorAdapter {

    public EditText text;

    public SearchedPeopleCursorAdapter(Context context, Cursor cursor, EditText text) {
        super(context, cursor);
        this.text = text;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenName = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.profile_pic);

        // sets up the font sizes
        holder.name.setTextSize(settings.textSize + 4);
        holder.screenName.setTextSize(settings.textSize);

        holder.picture.setClipToOutline(true);

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

                String currentText = text.getText().toString();

                try {
                    String[] curr = currentText.split(" ");
                    currentText = "";

                    for (String s : curr) {
                        if (s.contains("@")) {
                            currentText += s + " ";
                        }
                    }
                } catch (Exception e) {
                    currentText = text.getText().toString();
                }

                text.setText(currentText + "@" + screenName);
                text.setSelection(text.getText().length());

            }
        });
    }
}
