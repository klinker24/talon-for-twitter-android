package com.klinker.android.twitter_l.data;

import android.app.Application;
import android.content.Context;

import com.klinker.android.twitter_l.utils.EmojiUtils;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;

public class App extends Application {
    private BitmapLruCache mCache;
    private BitmapLruCache profileCache;

    private static final int MEGA_BYTE = 1024 * 1024;

    @Override
    public void onCreate() {
        super.onCreate();

        File cacheDir = new File(getCacheDir(), "talon");
        cacheDir.mkdirs();

        File proCacheDir = new File(getCacheDir(), "talon-profile");
        proCacheDir.mkdirs();

        BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
        builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize(.25f);
        builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir).setDiskCacheMaxSize(100 * MEGA_BYTE);

        mCache = builder.build();

        builder.setDiskCacheLocation(proCacheDir);

        profileCache = builder.build();

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

    public BitmapLruCache getProfileCache() {
        return profileCache;
    }

    public static App getInstance(Context context) {
        return (App) context.getApplicationContext();
    }
}