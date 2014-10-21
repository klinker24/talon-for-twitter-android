package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.PhotoViewerDialog;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetActivity;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import twitter4j.Status;
import twitter4j.User;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

import java.util.ArrayList;

/**
 * Created by lucasklinker on 9/27/14.
 */
public class MultipleTweetPicturesGridAdapter extends BaseAdapter {
    private Context context;
    private String[] pics;
    private int gridWidth;

    public MultipleTweetPicturesGridAdapter(Context context, String links, int gridWidth) {
        this.context = context;
        pics = links.split(" ");
        this.gridWidth = gridWidth;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.picture, null);

            AbsListView.LayoutParams params = new AbsListView.LayoutParams(gridWidth, gridWidth);
            convertView.setLayoutParams(params);

            ViewHolder holder = new ViewHolder();
            holder.iv = (NetworkedCacheableImageView) convertView.findViewById(R.id.picture);
            convertView.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) convertView.getTag();

        final String url = pics[position];

        holder.iv.loadImage(url, false, null);

        holder.iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photo = new Intent(context, PhotoViewerDialog.class).putExtra("url", url);
                photo.putExtra("shared_trans", true);

                ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation(((Activity)context), holder.iv, "image");

                context.startActivity(photo, options.toBundle());
            }
        });

        return convertView;
    }

    @Override
    public int getCount() {
        return pics.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public static class ViewHolder {
        public NetworkedCacheableImageView iv;
        public String url;
    }
}