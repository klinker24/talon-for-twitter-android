package com.klinker.android.twitter_l.adapters;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.profile_viewer.ProfilePager;

import java.util.ArrayList;
import java.util.List;

import twitter4j.User;

public class PeopleArrayAdapter extends ArrayAdapter<User> {

    public Context context;

    public boolean openFirst = false;

    public List<User> users;

    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;

    public Handler mHandler;

    public static class ViewHolder {
        public TextView name;
        public TextView screenName;
        public TextView following;
        public ImageView picture;
        public LinearLayout background;
        public long userId;
    }

    public PeopleArrayAdapter(Context context, ArrayList<User> users, boolean openFirst) {
        super(context, R.layout.tweet);

        this.context = context;
        this.users = users;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        this.openFirst = openFirst;

        setUpLayout();

        mHandler = new Handler();
    }

    public PeopleArrayAdapter(Context context, List<User> users) {
        super(context, R.layout.tweet);

        this.context = context;
        this.users = users;

        settings = AppSettings.getInstance(context);
        inflater = LayoutInflater.from(context);

        setUpLayout();

        mHandler = new Handler();
    }

    public void setUpLayout() {
        layout = R.layout.person;
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
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.name = (TextView) v.findViewById(R.id.name);
        holder.screenName = (TextView) v.findViewById(R.id.screen_name);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (ImageView) v.findViewById(R.id.profile_pic);
        holder.following = (TextView) v.findViewById(R.id.following);

        v.setTag(holder);
        return v;
    }

    public void setFollowingStatus(ViewHolder holder, Long id) {

    }

    public void bindView(final View view, int position, final User user) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final long id = user.getId();
        holder.userId = id;

        setFollowingStatus(holder, id);

        holder.name.setText(user.getName());
        holder.screenName.setText("@" + user.getScreenName());

        final String url = user.getOriginalProfileImageURL();

        Glide.with(context).load(url).into(holder.picture);

        holder.background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProfilePager.start(context, user);
            }
        });

        if (openFirst && position == 0) {
            holder.background.performClick();
            ((Activity) context).finish();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v;
        if (convertView == null) {

            v = newView(parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.picture.setImageDrawable(null);
        }

        bindView(v, position, users.get(position));

        return v;
    }
}
