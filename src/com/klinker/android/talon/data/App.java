package com.klinker.android.talon.data;

import android.app.Application;
import android.content.Context;

import com.klinker.android.talon.utils.EmojiUtils;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;

public class App extends Application {
    private BitmapLruCache mCache;

    @Override
    public void onCreate() {
        super.onCreate();

        File cacheDir = new File(getCacheDir(), "talon");
        cacheDir.mkdirs();

        BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
        builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
        builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir);

        mCache = builder.build();

        new Thread(new Runnable() {
            @Override
            public void run() {
                EmojiUtils.init(App.this);
            }
        }).start();
    }

    public BitmapLruCache getBitmapCache() {
        return mCache;
    }

    public static App getInstance(Context context) {
        return (App) context.getApplicationContext();
    }
}