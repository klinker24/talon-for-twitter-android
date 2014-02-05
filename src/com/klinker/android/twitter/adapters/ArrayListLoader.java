package com.klinker.android.twitter.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;

import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.ImageUtils;

import org.lucasr.smoothie.SimpleItemLoader;

import java.net.URL;

import twitter4j.Status;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

public class ArrayListLoader extends SimpleItemLoader<String, CacheableBitmapDrawable> {
    final BitmapLruCache mCache;
    private Context context;
    private boolean circleImages;

    public ArrayListLoader(BitmapLruCache cache, Context context) {
        mCache = cache;
        this.context = context;

        AppSettings settings = new AppSettings(context);

        // if the layout is talon's, then they should have circle images
        circleImages = settings.roundContactImages;
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
        CacheableBitmapDrawable wrapper = mCache.get(url);
        if (wrapper == null) {

            try {
                URL mUrl = new URL(url);

                Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                if (circleImages) {
                    image = ImageUtils.getCircle(image, context);
                }

                wrapper = mCache.put(url, image);
            } catch (Exception e) {

            }
        }

        return wrapper;
    }

    @Override
    public void displayItem(View itemView, CacheableBitmapDrawable result, boolean fromMemory) {
        try {

            TimelineArrayAdapter.ViewHolder holder = (TimelineArrayAdapter.ViewHolder) itemView.getTag();

            if (result == null) {
                return;
            }

            holder.profilePic.setImageBitmap(result.getBitmap());
        } catch (Exception e) {
            // happens sometimes when searching for users i guess
        }
    }

}