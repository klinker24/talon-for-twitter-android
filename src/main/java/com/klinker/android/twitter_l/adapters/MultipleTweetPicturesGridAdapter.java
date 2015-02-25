package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoPagerActivity;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.photo_viewer.PhotoViewerActivity;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;

public class MultipleTweetPicturesGridAdapter extends BaseAdapter {
    private Context context;
    private String[] pics;
    private String links;
    private int gridWidth;

    public MultipleTweetPicturesGridAdapter(Context context, String links, int gridWidth) {
        this.context = context;
        this.links = links;
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

                Intent viewImage = new Intent(context, PhotoPagerActivity.class);
                viewImage.putExtra("url", links);
                viewImage.putExtra("start_page", position);

                /*ActivityOptions options = ActivityOptions
                        .makeSceneTransitionAnimation(((Activity)context), holder.iv, "image");

                context.startActivity(viewImage, options.toBundle());*/
                context.startActivity(viewImage);
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