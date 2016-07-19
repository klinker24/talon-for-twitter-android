package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.content.Context;
import android.view.View;
import com.klinker.android.twitter_l.R;

import java.util.ArrayList;

import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.User;

public class ProfileFollowersPopup extends ProfileUsersPopup {

    ArrayList<User> ids;

    public ProfileFollowersPopup(Context context, User user) {
        super(context, user);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.followers);
    }

    public void setIds(User u) {

    }

    @Override
    public PagableResponseList<User> getData(Twitter twitter, long cursor) {
        try {
            return twitter.getFollowersList(user.getId(), cursor, 200);
        } catch (Exception e) {
            return null;
        }
    }
}