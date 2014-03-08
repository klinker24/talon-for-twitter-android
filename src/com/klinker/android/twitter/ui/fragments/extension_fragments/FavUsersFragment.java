package com.klinker.android.twitter.ui.fragments.extension_fragments;

import android.database.Cursor;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.ui.fragments.HomeExtensionFragment;


public class FavUsersFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getFavUsersCursor(sharedPrefs.getInt("current_account", 1));
    }
}