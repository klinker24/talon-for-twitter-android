package com.klinker.android.talon.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;

import com.klinker.android.talon.sq_lite.HomeSQLiteHelper;
import com.klinker.android.talon.utils.ImageUtils;

import org.lucasr.smoothie.SimpleItemLoader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class CursorListLoader extends SimpleItemLoader<String, CacheableBitmapDrawable> {
    final BitmapLruCache mCache;
    private Context context;

    public CursorListLoader(BitmapLruCache cache, Context context) {
        mCache = cache;
        this.context = context;
    }

    @Override
    public CacheableBitmapDrawable loadItemFromMemory(String url) {
        return mCache.getFromMemoryCache(url);
    }

    @Override
    public String getItemParams(Adapter adapter, int position) {
        Cursor cursor = (Cursor) adapter.getItem(0);
        cursor.moveToPosition(cursor.getCount() - position - 1);
        String url = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
        return url;
    }

    @Override
    public CacheableBitmapDrawable loadItem(String url) {

        CacheableBitmapDrawable wrapper = mCache.get(url);
        if (wrapper == null) {

            try {
                URL mUrl = new URL(url);

                Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                image = ImageUtils.getCircle(image, context);

                wrapper = mCache.put(url, image);
            } catch (Exception e) {

            }
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        TimeLineCursorAdapter.ViewHolder holder = (TimeLineCursorAdapter.ViewHolder) itemView.getTag();

        if (result == null) {
            return;
        }

        holder.profilePic.setImageDrawable(result);
    }
}