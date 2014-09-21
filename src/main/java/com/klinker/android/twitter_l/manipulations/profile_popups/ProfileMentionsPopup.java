package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.content.Context;
import android.view.View;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.ProfileListPopupLayout;
import twitter4j.*;

import java.util.List;


public class ProfileMentionsPopup extends ProfileListPopupLayout {

    public ProfileMentionsPopup(Context context, View main, User user, boolean windowed) {
        super(context, main, user, windowed);
    }

    public ProfileMentionsPopup(Context context, View main, User user) {
        super(context, main, user);
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.mentions);
    }

    public boolean incrementQuery() {
        if (result.hasNext()) {
            query = result.nextQuery();
            return true;
        } else {
            return false;
        }
    }

    private Query query;
    private QueryResult result;

    @Override
    public List<Status> getData(Twitter twitter) {
        try {
            if (query == null) {
                query = new Query("@" + user.getScreenName() + " -RT");
                query.sinceId(1);
                result = twitter.search(query);

                return result.getTweets();
            } else {
                result = twitter.search(query);

                return result.getTweets();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
