package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;

import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.InputStream;

public class TwitterUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new TwitterUrlLoader(context);
        }

        @Override
        public void teardown() {

        }
    }

    private Context context;

    public TwitterUrlLoader(Context context) {
        this.context = context;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl model, int width, int height) {
        return new TwitterStreamFetcher(context, model);
    }
}