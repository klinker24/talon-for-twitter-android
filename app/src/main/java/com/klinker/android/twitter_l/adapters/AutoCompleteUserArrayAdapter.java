package com.klinker.android.twitter_l.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;

import java.util.List;

import twitter4j.User;

public class AutoCompleteUserArrayAdapter extends PeopleArrayAdapter {

    public AutoCompleteUserArrayAdapter(Context context, List<User> users) {
        super(context, users);
    }

    public void bindView(final View view, int position, final User user) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final long id = user.getId();
        holder.userId = id;

        holder.name.setText(user.getName());
        holder.screenName.setText("@" + user.getScreenName());

        Glide.with(context).load(user.getOriginalProfileImageURL())
                .into(holder.picture);
    }
}
