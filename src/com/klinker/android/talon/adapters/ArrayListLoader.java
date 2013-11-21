package com.klinker.android.talon.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.*;
import android.view.View;
import android.widget.Adapter;
import com.klinker.android.talon.sq_lite.HomeSQLiteHelper;
import org.lucasr.smoothie.SimpleItemLoader;
import twitter4j.Status;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

import java.net.URL;

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
            Status staus = (Status) adapter.getItem(position);
            String url = staus.getUser().getOriginalProfileImageURL();
            return url;
        } catch (Exception e) {
            // no items...
            return "";
        }
    }

    @Override
    public CacheableBitmapDrawable loadItem(String url) {
        CacheableBitmapDrawable wrapper = mCache.get(url);
        if (wrapper == null) {

            try {
                URL mUrl = new URL(url);

                Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                image = getClip(image);

                wrapper = mCache.put(url, image);
            } catch (Exception e) {

            }
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        RepliesArrayAdapter.ViewHolder holder = (RepliesArrayAdapter.ViewHolder) itemView.getTag();

        if (result == null) {
            return;
        }

        holder.profilePic.setImageDrawable(result);
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