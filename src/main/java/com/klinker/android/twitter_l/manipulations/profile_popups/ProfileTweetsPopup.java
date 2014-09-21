package com.klinker.android.twitter_l.manipulations.profile_popups;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.manipulations.ProfileListPopupLayout;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;
import org.lucasr.smoothie.AsyncListView;
import twitter4j.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucasklinker on 9/20/14.
 */
public class ProfileTweetsPopup extends ProfileListPopupLayout {

    public ProfileTweetsPopup(Context context, View main, User user, boolean windowed) {
        super(context, main, user, windowed);
    }

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
