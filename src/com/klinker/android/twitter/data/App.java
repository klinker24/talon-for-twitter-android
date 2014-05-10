package com.klinker.android.twitter.data;

import android.app.Application;
import android.content.Context;

import com.klinker.android.twitter.ui.launcher_page.LauncherPage;
import com.klinker.android.twitter.utils.EmojiUtils;

import java.io.File;

import uk.co.senab.bitmapcache.BitmapLruCache;

public class App extends Application {
    private BitmapLruCache mCache;

    public App() {
        super();
        ClassLoader mClassLoader = LauncherPage.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(mClassLoader);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            File cacheDir = new File(createPackageContext("com.klinker.android.twitter", CONTEXT_IGNORE_SECURITY).getCacheDir(), "talon");
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
        } catch (Exception e) {
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
    }

    public BitmapLruCache getBitmapCache() {
        return mCache;
    }

    public static App getInstance(Context context) {
        return (App) context.getApplicationContext();
    }
}