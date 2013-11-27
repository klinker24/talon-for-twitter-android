package com.klinker.android.talon.adapters;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.sq_lite.FavoriteUsersSQLiteHelper;
import com.klinker.android.talon.ui.UserProfileActivity;
import com.squareup.picasso.Picasso;

/**
 * Created by luke on 11/27/13.
 */
public class PeopleCursorAdapter extends CursorAdapter {

    private Context context;
    private Cursor cursor;
    private LayoutInflater inflater;
    private AppSettings settings;

    private SharedPreferences sharedPrefs;

    public static class ViewHolder {
        public TextView name;
        public TextView screenName;
        public ImageView picture;
        public LinearLayout background;
    }

    public PeopleCursorAdapter(Context context, Cursor cursor) {

        super(context, cursor, 0);

        this.context = context;
        this.cursor = cursor;
        this.inflater = LayoutInflater.from(context);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        settings = new AppSettings(context);

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v;
        v = inflater.inflate(R.layout.person, viewGroup, false);
        final ViewHolder holder;

        holder = new ViewHolder();

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenName = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.profile_pic);

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


        Picasso.with(context)
                .load(url)
                .transform(new CircleTransform())
                .into(holder.picture);

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

            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.picture.setImageDrawable(context.getResources().getDrawable(resource));
        }

        bindView(v, context, cursor);

        return v;
    }
}
