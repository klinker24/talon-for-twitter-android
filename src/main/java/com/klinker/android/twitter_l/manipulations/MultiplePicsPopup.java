package com.klinker.android.twitter_l.manipulations;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.MultipleTweetPicturesGridAdapter;
import com.klinker.android.twitter_l.adapters.PicturesGridAdapter;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.TweetLinkUtils;
import com.klinker.android.twitter_l.utils.Utils;
import twitter4j.*;

import java.util.ArrayList;

/**
 * Created by lucasklinker on 9/27/14.
 */
public class MultiplePicsPopup extends PopupLayout {

    GridView listView;
    LinearLayout spinner;

    private String pics;

    public MultiplePicsPopup(Context context, String pics) {
        super(context);

        this.pics = pics;

        setUp();
    }

    public MultiplePicsPopup(Context context, boolean windowed, String pics) {
        super(context, windowed);

        this.pics = pics;

        setUp();
    }

    @Override
    public View setMainLayout() {
        return null;
    }

    private void setUp() {
        setFullScreen();
        setTitle(getContext().getString(R.string.pictures));

        View root = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.picture_popup_layout, null, false);

        listView = (GridView) root.findViewById(R.id.gridView);
        spinner = (LinearLayout) root.findViewById(R.id.spinner);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
        params.width = width;
        spinner.setLayoutParams(params);

        MultipleTweetPicturesGridAdapter adapter = new MultipleTweetPicturesGridAdapter(getContext(), pics, width / 2);

        listView.setAdapter(adapter);
        listView.setNumColumns(2);

        spinner.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        content.addView(root);
    }
}