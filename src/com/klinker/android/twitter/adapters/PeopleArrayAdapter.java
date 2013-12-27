package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.ui.UserProfileActivity;

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

    public boolean talonLayout;
    public int layout;
    public int border;

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

        v = inflater.inflate(layout, viewGroup, false);

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

            holder.picture.setImageDrawable(context.getResources().getDrawable(border));
        }

        bindView(v, context, users.get(position));

        return v;
    }
}
