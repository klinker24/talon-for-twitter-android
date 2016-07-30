package com.klinker.android.twitter_l.views.popups;

import android.content.Context;

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
