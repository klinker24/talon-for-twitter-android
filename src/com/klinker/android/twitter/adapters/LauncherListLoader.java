package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.view.View;

import com.klinker.android.twitter.ui.launcher_page.adapters.LauncherTimelineCursorAdapter;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;


public class LauncherListLoader extends CursorListLoader {
    public LauncherListLoader(BitmapLruCache cache, Context context) {
        super(cache, context);
    }

    public LauncherListLoader(BitmapLruCache cache, Context context, boolean circle) {
        super(cache, context, circle);
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        final LauncherTimelineCursorAdapter.ViewHolder holder = (LauncherTimelineCursorAdapter.ViewHolder) itemView.getTag();

        if (result == null) {
            return;
        }

        holder.profilePic.setImageDrawable(result);
    }
}
