package com.klinker.android.twitter_l.activities.main_fragments.home_fragments.extentions;


import android.database.Cursor;

import com.klinker.android.twitter_l.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter_l.activities.main_fragments.home_fragments.HomeExtensionFragment;

public class PicFragment extends HomeExtensionFragment {

    @Override
    public Cursor getCursor() {
        return HomeDataSource.getInstance(context).getPicsCursor(currentAccount);
    }
}