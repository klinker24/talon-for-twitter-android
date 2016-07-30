package com.klinker.android.twitter_l.views.popups;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.GridView;
import android.widget.LinearLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.MultipleTweetPicturesGridAdapter;
import com.klinker.android.twitter_l.views.widgets.PopupLayout;

/*
    Used on the TimelineArrayAdapter, TweetActivity, TimelineCursorAdapter, and the tweet view
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