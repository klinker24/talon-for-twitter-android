package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;

import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.InputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class TwitterUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    public static class Factory extends OkHttpUrlLoader.Factory {

        private static volatile Call.Factory internalClient;
        private Call.Factory client;

        public Factory() {
            this(getInternalClient());
        }

        public Factory(Call.Factory client) {
            super(client);
            this.client = client;
        }

        @Override
        public ModelLoader<GlideUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new TwitterUrlLoader(client, context);
        }

        private static Call.Factory getInternalClient() {
            if (internalClient == null) {
                synchronized (OkHttpUrlLoader.Factory.class) {
                    if (internalClient == null) {
                        internalClient = new OkHttpClient();
                    }
                }
            }
            return internalClient;
        }
    }

    private Context context;
    private final Call.Factory client;

    public TwitterUrlLoader(Call.Factory client, Context context) {
        this.context = context;
        this.client = client;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(GlideUrl model, int width, int height) {
        return new TwitterStreamFetcher(context, client, model);
        //return new OkHttpStreamFetcher(client, model);
    }
}