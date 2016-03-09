package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitterDMPicHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TwitterStreamFetcher extends HttpUrlFetcher {

    private Context context;
    private GlideUrl url;

    public TwitterStreamFetcher(Context context, GlideUrl glideUrl) {
        super(glideUrl);
        this.context = context;
        this.url = glideUrl;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        String urlString = url.toStringUrl();
        if (urlString.contains("ton.twitter.com") || urlString.contains("twitter.com/messages/")) {
            // direct message picture. Need to authorize
            TwitterDMPicHelper helper = new TwitterDMPicHelper();
            Bitmap bitmap = helper.getDMPicture(urlString, Utils.getTwitter(context, AppSettings.getInstance(context)), context);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] bitmapdata = bos.toByteArray();
            return new ByteArrayInputStream(bitmapdata);
        } else if (url.toStringUrl().contains(" ")) {
            // multiple pictures. Need to download and combine

            return null;
        } else {
            return super.loadData(priority);
        }
    }
}
