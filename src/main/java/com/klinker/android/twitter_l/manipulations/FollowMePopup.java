package com.klinker.android.twitter_l.manipulations;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;

/**
 * Created by luke on 9/26/14.
 */
public class FollowMePopup extends PopupLayout {

    Context context;
    public FollowMePopup(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public View setMainLayout() {
        View root = ((Activity)context).getLayoutInflater().inflate(R.layout.follow_me_popup, null, false);

        return root;
    }
}
