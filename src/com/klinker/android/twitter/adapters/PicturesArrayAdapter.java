package com.klinker.android.twitter.adapters;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.PhotoViewerDialog;
import com.klinker.android.twitter.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.drawer_activities.discover.trends.SearchedTrendsActivity;

import java.util.ArrayList;

import twitter4j.User;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class PicturesArrayAdapter extends ArrayAdapter<String> {

    protected Context context;

    private ArrayList<String> text;

    private LayoutInflater inflater;
    private AppSettings settings;

    private Handler handler;

    public static class ViewHolder {
        public NetworkedCacheableImageView iv;
        public String url;
    }

    public PicturesArrayAdapter(Context context, ArrayList<String> text) {
        super(context, R.layout.picture);

        this.context = context;
        this.text = text;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        handler = new Handler();
    }

    @Override
    public int getCount() {
        try {
            return text.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(R.layout.picture, viewGroup, false);

        holder = new ViewHolder();

        holder.iv = (NetworkedCacheableImageView) v.findViewById(R.id.picture);

        v.setTag(holder);
        return v;
    }

    public void bindView(final View view, Context mContext, final String url) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.url = url;

        holder.iv.loadImage(url, false, new NetworkedCacheableImageView.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(CacheableBitmapDrawable result) {
                holder.iv.setBackgroundDrawable(null);
            }
        });

        holder.iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(context, PhotoViewerDialog.class).putExtra("url", url));
            }
        });

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();
            holder.iv.setImageDrawable(null);

            if (settings.theme == AppSettings.THEME_LIGHT) {
                holder.iv.setBackgroundResource(R.drawable.rect_border_light);
            } else {
                holder.iv.setBackgroundResource(R.drawable.rect_border_dark);
            }
        }

        bindView(v, context, text.get(position));

        return v;
    }

    public String getElement(int pos) {
        return text.get(pos);
    }
}
