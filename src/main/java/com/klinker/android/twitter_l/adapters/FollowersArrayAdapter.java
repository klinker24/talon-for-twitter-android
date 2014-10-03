package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

import twitter4j.User;

/**
 * Created by luke on 10/3/14.
 */
public class FollowersArrayAdapter extends PeopleArrayAdapter {

    ArrayList<Long> followingIds;

    public FollowersArrayAdapter(Context context, ArrayList<User> users, ArrayList<Long> followingIds) {
        super(context, users);

        this.followingIds = followingIds;

        Log.v("talon_followers", followingIds.size() + " followers");
    }

    @Override
    public void setFollowingStatus(ViewHolder holder, User u) {
        if (holder.following != null) {
            Log.v("talon_followers", "checking follow status for: " + u.getName());
            Long l = u.getId();
            if (followingIds.contains(l)) {
                holder.following.setVisibility(View.VISIBLE);
                Log.v("talon_followers", "i am following this person");
            } else {
                holder.following.setVisibility(View.GONE);
                Log.v("talon_followers", "i am not following this person");
            }
        }
    }
}
