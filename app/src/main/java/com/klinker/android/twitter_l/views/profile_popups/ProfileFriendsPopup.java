package com.klinker.android.twitter_l.views.profile_popups;

import android.content.Context;

import com.klinker.android.twitter_l.R;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.User;

public class ProfileFriendsPopup extends ProfileUsersPopup {

    public ProfileFriendsPopup(Context context, User user) {
        super(context, user);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.following);
    }

    @Override
    public PagableResponseList<User> getData(Twitter twitter, long cursor) {
        try {
            return twitter.getFriendsList(user.getId(), cursor, 200);
        } catch (Exception e) {
            return null;
        }
    }
}
