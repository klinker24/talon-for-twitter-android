package com.klinker.android.talon.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;
import com.klinker.android.talon.ui.UserProfileActivity;

import java.util.ArrayList;

import twitter4j.User;

/**
 * Created by luke on 11/26/13.
 */
public class PeopleArrayAdapter extends ArrayAdapter<User> {

    public Context context;

    public ArrayList<User> users;

    public LayoutInflater inflater;
    public AppSettings settings;

    public static class ViewHolder {
        public TextView name;
        public TextView screenName;
        public NetworkedCacheableImageView picture;
        public LinearLayout background;
    }

    public PeopleArrayAdapter(Context context, ArrayList<User> users) {
        super(context, R.layout.tweet);

        this.context = context;
        this.users = users;

        settings = new AppSettings(context);
        inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public User getItem(int position) {
        return users.get(position);
    }

    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(R.layout.person, viewGroup, false);

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

    public void bindView(final View view, Context mContext, final User user) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.name.setText(user.getName());
        holder.screenName.setText("@" + user.getScreenName());

        holder.picture.loadImage(user.getBiggerProfileImageURL(), true, null, NetworkedCacheableImageView.CIRCLE);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewProfile = new Intent(context, UserProfileActivity.class);
                viewProfile.putExtra("name", user.getName());
                viewProfile.putExtra("screenname", user.getScreenName());
                viewProfile.putExtra("proPic", user.getBiggerProfileImageURL());
                //viewProfile.putExtra("tweetid", holder.tweetId);
                viewProfile.putExtra("retweet", false);

                context.startActivity(viewProfile);
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

            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            holder.picture.setImageDrawable(context.getResources().getDrawable(resource));
        }

        bindView(v, context, users.get(position));

        return v;
    }
}
