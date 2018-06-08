package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.util.ContentLengthInputStream;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
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
import java.util.Map;

import okhttp3.Call;
import okhttp3.ResponseBody;

public class TwitterStreamFetcher extends HttpUrlFetcher {

    private static final String TAG = "glide_ok";
    private Context context;
    private GlideUrl url;

    private final Call.Factory client;
    private InputStream stream;
    private ResponseBody responseBody;
    private volatile Call call;

    public TwitterStreamFetcher(Context context, Call.Factory client, GlideUrl glideUrl) {
        super(glideUrl);
        this.context = context;
        this.url = glideUrl;
        this.client = client;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        String urlString;

        if (!url.toStringUrl().contains("bytebucket.org/jklinker")) {
            urlString = URLDecoder.decode(url.toStringUrl());
        } else {
            urlString = url.toStringUrl();
        }

        if (urlString.contains("ton.twitter.com") || urlString.contains("twitter.com/messages/")) {
            // direct message picture. Need to authorize
            TwitterDMPicHelper helper = new TwitterDMPicHelper();
            Bitmap bitmap = helper.getDMPicture(urlString, Utils.getTwitter(context, AppSettings.getInstance(context)), context);
            return convertToInputStream(bitmap);
        } else if (urlString.contains(" ")) {
            String[] pics = urlString.split(" ");
            Bitmap[] bitmaps = new Bitmap[pics.length];

            // need to download all of them, then combine them
            for (int i = 0; i < pics.length; i++) {
                String url = pics[i];
                bitmaps[i] = Glide.with(context).load(url).asBitmap().into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
            }

            // now that we have all of them, we need to put them together
            Bitmap combined = ImageUtils.combineBitmaps(context, bitmaps);
            return convertToInputStream(combined);
        } else {
            Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl().replace("http://", "https://"));

            for (Map.Entry<String, String> headerEntry : url.getHeaders().entrySet()) {
                String key = headerEntry.getKey();
                requestBuilder.addHeader(key, headerEntry.getValue());
            }
            Request request = requestBuilder.build();

            Response response;
            call = client.newCall(request);
            response = call.execute();
            responseBody = response.body();
            if (!response.isSuccessful()) {
                throw new IOException("Request failed with code: " + response.code());
            }

            long contentLength = responseBody.contentLength();
            stream = ContentLengthInputStream.obtain(responseBody.byteStream(), contentLength);
            return stream;
        }
    }

    private InputStream convertToInputStream(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
        return new ByteArrayInputStream(bos.toByteArray());
    }
}
