package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.InteractionsSQLiteHelper;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.widgets.HoloTextView;

import uk.co.senab.bitmapcache.BitmapLruCache;

/**
 * Created by luke on 1/7/14.
 */
public class InteractionsCursorAdapter extends CursorAdapter {

    public Context context;
    public Cursor cursor;
    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;
    public boolean talonLayout;
    public BitmapLruCache mCache;
    public int border;
    public ColorDrawable color;
    public ColorDrawable transparent;

    public static class ViewHolder {
        public HoloTextView title;
        public HoloTextView text;
        public NetworkedCacheableImageView picture;
        public LinearLayout background;
    }

    public InteractionsCursorAdapter(Context context, Cursor cursor) {

        super(context, cursor, 0);

        this.context = context;
        this.cursor = cursor;
        this.inflater = LayoutInflater.from(context);

        settings = new AppSettings(context);

        setUpLayout();
    }

    public void setUpLayout() {
        talonLayout = settings.layout == AppSettings.LAYOUT_TALON;

        layout = R.layout.interaction;

        TypedArray b;
        if (talonLayout) {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        } else {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.squareBorder});
        }
        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = App.getInstance(context).getBitmapCache();

        b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.message_color});
        color = new ColorDrawable(context.getResources().getColor(b.getResourceId(0, 0)));
        b.recycle();

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.title = (HoloTextView) v.findViewById(R.id.title);
        holder.text = (HoloTextView) v.findViewById(R.id.text);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (NetworkedCacheableImageView) v.findViewById(R.id.picture);

        // sets up the font sizes
        holder.title.setTextSize(15);
        holder.text.setTextSize(14);

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

        holder.title.setText(Html.fromHtml(title));

        if(!text.equals("")) {
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(text);
        } else {
            holder.text.setVisibility(View.GONE);
        }

        if(settings.roundContactImages) {
            holder.picture.loadImage(url, true, null, NetworkedCacheableImageView.CIRCLE);
        } else {
            holder.picture.loadImage(url, true, null);
        }

        // set the background color
        if (unread == 1) {
            //holder.background.setBackgroundDrawable(color);
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

            holder.picture.setImageDrawable(context.getResources().getDrawable(border));
        }

        bindView(v, context, cursor);

        return v;
    }
}
