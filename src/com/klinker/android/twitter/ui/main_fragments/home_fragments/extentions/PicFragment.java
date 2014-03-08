package com.klinker.android.twitter.ui.main_fragments.home_fragments.extentions;


import android.database.Cursor;

import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.ui.main_fragments.home_fragments.HomeExtensionFragment;

public class PicFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getPicsCursor(sharedPrefs.getInt("current_account", 1));
    }
}