package com.klinker.android.twitter.ui.fragments.extension_fragments;


import android.database.Cursor;

import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.ui.fragments.HomeExtensionFragment;

public class LinksFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getLinksCursor(sharedPrefs.getInt("current_account", 1));
    }
}