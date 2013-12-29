package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;

import java.util.ArrayList;

import twitter4j.Status;

/**
 * Created by luke on 11/30/13.
 */
public class ProfilesArrayAdapter extends TimelineArrayAdapter {
    public ProfilesArrayAdapter(Context context, ArrayList<Status> statuses) {
        super(context, statuses);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = new AppSettings(context);
        talonLayout = settings.layout == AppSettings.LAYOUT_TALON;

        layout = talonLayout ? R.layout.tweet : R.layout.tweet_hangout;

        TypedArray b;
        if (talonLayout) {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        } else {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.squareBorder});
        }
        border = b.getResourceId(0, 0);
        b.recycle();

        this.type = NORMAL;

        this.username = "";
    }

    public ProfilesArrayAdapter(Context context, ArrayList<Status> statuses, String username) {
        super(context, statuses, username);

        this.context = context;
        this.statuses = statuses;
        this.inflater = LayoutInflater.from(context);

        this.settings = new AppSettings(context);
        talonLayout = settings.layout == AppSettings.LAYOUT_TALON;

        layout = talonLayout ? R.layout.tweet : R.layout.tweet_hangout;

        TypedArray b;
        if (settings.roundContactImages) {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});
        } else {
            b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.squareBorder});
        }
        border = b.getResourceId(0, 0);
        b.recycle();

        this.type = NORMAL;
        this.username = username;
    }

    public View newView(ViewGroup viewGroup) {
        View v;
        final ViewHolder holder;

        v = inflater.inflate(layout, viewGroup, false);

        holder = new ViewHolder();

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.profilePic = (ImageView) v.findViewById(R.id.profile_pic);
        holder.time = (TextView) v.findViewById(R.id.time);
        holder.tweet = (TextView) v.findViewById(R.id.tweet);
        holder.reply = (EditText) v.findViewById(R.id.reply);
        holder.favorite = (ImageButton) v.findViewById(R.id.favorite);
        holder.retweet = (ImageButton) v.findViewById(R.id.retweet);
        holder.favCount = (TextView) v.findViewById(R.id.fav_count);
        holder.retweetCount = (TextView) v.findViewById(R.id.retweet_count);
        holder.expandArea = (LinearLayout) v.findViewById(R.id.expansion);
        holder.replyButton = (ImageButton) v.findViewById(R.id.reply_button);
        holder.image = (NetworkedCacheableImageView) v.findViewById(R.id.image);
        holder.retweeter = (TextView) v.findViewById(R.id.retweeter);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.playButton = (NetworkedCacheableImageView) v.findViewById(R.id.play_button);

        // sets up the font sizes
        holder.tweet.setTextSize(settings.textSize);
        holder.name.setTextSize(settings.textSize + 4);
        holder.time.setTextSize(settings.textSize - 3);
        holder.retweeter.setTextSize(settings.textSize - 3);
        holder.favCount.setTextSize(settings.textSize + 1);
        holder.retweetCount.setTextSize(settings.textSize + 1);
        holder.reply.setTextSize(settings.textSize);

        v.setTag(holder);
        return v;
    }
}
