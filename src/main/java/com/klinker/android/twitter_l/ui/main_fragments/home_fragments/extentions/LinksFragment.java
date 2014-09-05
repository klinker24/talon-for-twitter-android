package com.klinker.android.twitter_l.ui.main_fragments.home_fragments.extentions;


import android.database.Cursor;

import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.ui.main_fragments.home_fragments.HomeExtensionFragment;

public class LinksFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getLinksCursor(currentAccount);
    }
}