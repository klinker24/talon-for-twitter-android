package com.klinker.android.talon.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.CircleTransform;
import com.klinker.android.talon.utilities.Utils;
import com.squareup.picasso.Picasso;
import twitter4j.Status;

import java.util.ArrayList;

public class RepliesArrayAdapter extends ArrayAdapter<Status> {

    private Context context;
    private ArrayList<Status> statuses;
    private LayoutInflater inflater;

    public static class ViewHolder {
        public TextView name;
        public ImageView profilePic;
        public TextView tweet;
        public TextView time;
    }

    public RepliesArrayAdapter(Context context, ArrayList<Status> statuses) {
        super(context, R.layout.tweet);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return statuses.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;

        if (rowView == null) {
            rowView = inflater.inflate(R.layout.tweet, null);

            ViewHolder viewHolder = new ViewHolder();

            viewHolder.name = (TextView) rowView.findViewById(R.id.name);
            viewHolder.profilePic = (ImageView) rowView.findViewById(R.id.profile_pic);
            viewHolder.tweet = (TextView) rowView.findViewById(R.id.tweet);
            viewHolder.time = (TextView) rowView.findViewById(R.id.time);

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        holder.name.setText(statuses.get(position).getUser().getName());
        holder.tweet.setText(statuses.get(position).getText());
        holder.time.setText(Utils.getTimeAgo(statuses.get(position).getCreatedAt().getTime()));

        Picasso.with(context)
                .load(statuses.get(position).getUser().getBiggerProfileImageURL())
                .transform(new CircleTransform())
                .into(holder.profilePic);

        return rowView;
    }
}
