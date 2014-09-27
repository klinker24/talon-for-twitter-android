package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.content.Context;
import android.view.View;
import com.klinker.android.twitter_l.R;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.User;

/**
 * Created by lucasklinker on 9/27/14.
 */
public class ProfileFollowersPopup extends ProfileUsersPopup {

    public ProfileFollowersPopup(Context context, User user) {
        super(context, user);
    }

    public ProfileFollowersPopup(Context context, User user, boolean windowed) {
        super(context, user, windowed);
    }

    @Override
    public String getTitle() {
        return getContext().getResources().getString(R.string.followers);
    }

    @Override
    public PagableResponseList<User> getData(Twitter twitter, long cursor) {
        try {
            return twitter.getFollowersList(user.getId(), cursor);
        } catch (Exception e) {
            return null;
        }
    }
}