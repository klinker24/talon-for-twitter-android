package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.data.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.twitter.ui.UserProfileActivity;

public class PeopleCursorAdapter extends CursorAdapter {

    public Context context;
    public Cursor cursor;
    public LayoutInflater inflater;
    public AppSettings settings;

    public boolean talonLayout;
    public int layout;
    public int border;

    private SharedPreferences sharedPrefs;

    public static class ViewHolder {
        public TextView name;
        public TextView screenName;
        public NetworkedCacheableImageView picture;
        public LinearLayout background;
    }

    public PeopleCursorAdapter(Context context, Cursor cursor) {

        super(context, cursor, 0);

        this.context = context;
        this.cursor = cursor;
        this.inflater = LayoutInflater.from(context);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        settings = new AppSettings(context);

        talonLayout = settings.layout == AppSettings.LAYOUT_TALON;

        layout = talonLayout ? R.layout.person : R.layout.person_hangouts;

        TypedArray b;
        if (talonLayout) {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        } else {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.squareBorder});
        }
        border = b.getResourceId(0, 0);
        b.recycle();

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v;
        v = inflater.inflate(layout, viewGroup, false);
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
                Intent viewProfile = new Intent(context, UserProfileActivity.class);
                viewProfile.putExtra("name", name);
                viewProfile.putExtra("screenname", screenName);
                viewProfile.putExtra("proPic", url);
                viewProfile.putExtra("retweet", false);

                context.startActivity(viewProfile);
            }
        });
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

            holder.picture.setImageDrawable(context.getResources().getDrawable(border));
        }

        bindView(v, context, cursor);

        return v;
    }
}
