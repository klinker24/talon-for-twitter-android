package com.klinker.android.twitter_l.manipulations;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.klinker.android.twitter_l.R;

public class FavoritersPopupLayout extends RetweetersPopupLayout {
    public FavoritersPopupLayout(Context context) {
        super(context);
    }

    @Override
    public void setUserWindowTitle() {
        setTitle(getContext().getString(R.string.favorites));
        noContentText.setText(getResources().getString(R.string.no_favorites));
    }

}
