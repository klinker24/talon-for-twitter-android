package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.media_viewer.image.ImageViewerActivity;

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
            holder.iv = (ImageView) convertView.findViewById(R.id.picture);
            convertView.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder) convertView.getTag();

        final String url = pics[position];

        Glide.with(context).load(url).into(holder.iv);

        holder.iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageViewerActivity.Companion.startActivity(context, position, links.split(" "));
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
        public ImageView iv;
        public String url;
    }
}