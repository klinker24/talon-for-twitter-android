package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.content.Context;
import android.view.View;

import com.klinker.android.twitter_l.R;

import twitter4j.*;

import java.util.ArrayList;
import java.util.List;

public class ProfileTweetsPopup extends ProfileListPopupLayout {

    public ProfileTweetsPopup(Context context, View main, User user) {
        super(context, main, user);
    }

    public String getTitle() {
        return getResources().getString(R.string.tweets);
    }

    @Override
    public boolean incrementQuery() {
        paging.setPage(paging.getPage() + 1);
        return true;
    }

    @Override
    public List<Status> getData(Twitter twitter) {
        try {
            ArrayList<Status> statuses = new ArrayList<Status>();
            for (Status s : twitter.getUserTimeline(user.getId(), paging)) {
                statuses.add(s);
            }
            return statuses;
        } catch (Exception e) {
            return null;
        }
    }
}
