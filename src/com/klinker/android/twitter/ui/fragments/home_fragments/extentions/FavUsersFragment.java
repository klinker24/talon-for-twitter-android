package com.klinker.android.twitter.ui.fragments.home_fragments.extentions;

import android.database.Cursor;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.ui.fragments.home_fragments.HomeExtensionFragment;


public class FavUsersFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getFavUsersCursor(sharedPrefs.getInt("current_account", 1));
    }
}