package com.klinker.android.talon.adapters;

import android.content.Context;
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

import org.lucasr.smoothie.SimpleItemLoader;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import twitter4j.Status;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class ArrayListLoader extends SimpleItemLoader<String, CacheableBitmapDrawable> {
    final BitmapLruCache mCache;
    private Context context;

    public ArrayListLoader(BitmapLruCache cache, Context context) {
        mCache = cache;
        this.context = context;
    }

    @Override
    public CacheableBitmapDrawable loadItemFromMemory(String url) {
        return mCache.getFromMemoryCache(url);
    }

    @Override
    public String getItemParams(Adapter adapter, int position) {
        try {
            Status status = (Status) adapter.getItem(position);
            String url;

            if (!status.isRetweet()) {
                url = status.getUser().getBiggerProfileImageURL();
            } else {
                url = status.getRetweetedStatus().getUser().getBiggerProfileImageURL();
            }
            return url;
        } catch (Exception e) {
            // no items...
            Log.v("getting_url", "no url found");
            return "";
        }
    }

    @Override
    public CacheableBitmapDrawable loadItem(String url) {
        CacheableBitmapDrawable result = mCache.get(url, null);

        try {
            if (null == result) {
                Log.d("ImageUrlAsyncTask", "Downloading: " + url);

                // The bitmap isn't cached so download from the web
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                InputStream is = new BufferedInputStream(conn.getInputStream());

                // Add to cache
                result = mCache.put(url, is, null);
            } else {
                Log.d("ImageUrlAsyncTask", "Got from Cache: " + url);
            }
        } catch (Exception e) {

        }

        return result;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        TimelineArrayAdapter.ViewHolder holder = (TimelineArrayAdapter.ViewHolder) itemView.getTag();

        if (result == null) {
            return;
        }

        holder.profilePic.setImageBitmap(getClip(result.getBitmap()));
    }

    private Bitmap getClip(Bitmap currentImage) {
        Bitmap bitmap = currentImage;
        Bitmap output = Bitmap.createBitmap(currentImage.getWidth(),
                currentImage.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, currentImage.getWidth(),
                currentImage.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(currentImage.getWidth() / 2,
                currentImage.getHeight() / 2, (currentImage.getWidth() / 2) - (currentImage.getWidth() / 25), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }
}