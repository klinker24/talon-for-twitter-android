package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.io.InputStream;

public class TwitterGlideModule implements GlideModule {
    @Override public void applyOptions(Context context, GlideBuilder builder) {
        AppSettings settings = AppSettings.getInstance(context);
        if (settings.higherQualityImages) {
            builder.setDecodeFormat(DecodeFormat.PREFER_ARGB_8888);
        }
    }

    @Override public void registerComponents(Context context, Glide glide) {
        glide.register(GlideUrl.class, InputStream.class, new TwitterUrlLoader.Factory());
    }
}