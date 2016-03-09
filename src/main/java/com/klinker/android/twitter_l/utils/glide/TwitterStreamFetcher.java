package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.HttpUrlFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.target.Target;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitterDMPicHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;

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
        String urlString = URLDecoder.decode(url.toStringUrl());
        if (urlString.contains("ton.twitter.com") || urlString.contains("twitter.com/messages/")) {
            // direct message picture. Need to authorize
            TwitterDMPicHelper helper = new TwitterDMPicHelper();
            Bitmap bitmap = helper.getDMPicture(urlString, Utils.getTwitter(context, AppSettings.getInstance(context)), context);
            return convertToInputStream(bitmap);
        } else if (urlString.contains(" ")) {
            Log.v("talon_loader", "url: " + urlString);
            String[] pics = urlString.split(" ");
            Bitmap[] bitmaps = new Bitmap[pics.length];

            // need to download all of them, then combine them
            for (int i = 0; i < pics.length; i++) {
                Log.v("talon_loader", "downloading: " + pics[i]);
                String url = pics[i];
                bitmaps[i] = Glide.with(context).load(url).asBitmap().into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
            }

            // now that we have all of them, we need to put them together
            Bitmap combined = ImageUtils.combineBitmaps(context, bitmaps);
            return convertToInputStream(combined);
        } else {
            return super.loadData(priority);
        }
    }

    private InputStream convertToInputStream(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
        return new ByteArrayInputStream(bos.toByteArray());
    }
}
