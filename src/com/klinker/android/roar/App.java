package com.klinker.android.roar;

import android.app.Application;
import android.content.Context;
import uk.co.senab.bitmapcache.BitmapLruCache;

import java.io.File;

public class App extends Application {
    private BitmapLruCache mCache;

    @Override
    public void onCreate() {
        super.onCreate();

        File cacheDir = new File(getCacheDir(), "roar");
        cacheDir.mkdirs();

        BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
        builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
        builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir);

        mCache = builder.build();
    }

    public BitmapLruCache getBitmapCache() {
        return mCache;
    }

    public static App getInstance(Context context) {
        return (App) context.getApplicationContext();
    }
}