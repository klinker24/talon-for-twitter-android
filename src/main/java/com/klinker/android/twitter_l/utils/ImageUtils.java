package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.klinker.android.launcher.api.ResourceHelper;
import com.klinker.android.twitter_l.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;


public class ImageUtils {

    public static Bitmap notificationResize(Context context, Bitmap currentImage) {
        try {
            int scale = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics());

            return Bitmap.createScaledBitmap(currentImage, scale, scale, true);
        } catch (OutOfMemoryError e) {
            return currentImage;
        }
    }

    public static Bitmap resizeImage(Context context, Bitmap currentImage, int dp) {
        try {
            int scale = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());

            return Bitmap.createScaledBitmap(currentImage, scale, scale, true);
        } catch (OutOfMemoryError e) {
            return currentImage;
        }
    }

    public static Bitmap cropSquare(Bitmap currentImage) {
        if (currentImage.getWidth() >= currentImage.getHeight()) {
            currentImage = Bitmap.createBitmap(
                    currentImage,
                    currentImage.getWidth() / 2 - currentImage.getHeight() / 2,
                    0,
                    currentImage.getHeight(),
                    currentImage.getHeight()
            );
        } else {
            currentImage = Bitmap.createBitmap(
                    currentImage,
                    0,
                    currentImage.getHeight()/2 - currentImage.getWidth()/2,
                    currentImage.getWidth(),
                    currentImage.getWidth()
            );
        }

        return currentImage;
    }

    public static Bitmap blur(Bitmap sentBitmap) {

        int radius = 4;

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }

            stackpointer = radius;

            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }

                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }

            yw += w;
        }

        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;

            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }

            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }

                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    public static Bitmap overlayPlay(Bitmap bmp1, Context context) {
        Bitmap bmp2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_play, null);

        Bitmap bmOverlay = Bitmap.createBitmap( bmp1.getWidth(), bmp1.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas2 = new Canvas(bmOverlay);
        canvas2.drawBitmap(bmp1, 0, 0, null);
        canvas2.drawBitmap(bmp2, Utils.toDP(64, context), Utils.toDP(64, context), null);
        return bmOverlay;
    }

    public static void loadImage(Context context, final ImageView iv, String url, BitmapLruCache mCache) {

        if (url == null) {
            return;
        }

        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper && iv.getVisibility() != View.GONE) {
            // The cache has it, so just display it
            iv.setImageDrawable(wrapper);

            try {
                Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                iv.startAnimation(fadeInAnimation);
            } catch (Exception e) {

            }
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            iv.setImageDrawable(null);

            imageUrlAsyncTask(context, iv, mCache, false, url);

        }
    }

    private static void imageUrlAsyncTask(final Context context, final ImageView imageView, final BitmapLruCache mCache, final boolean profile, final String url) {

        final WeakReference<ImageView> mImageViewRef = new WeakReference<ImageView>(imageView);

        Thread imageDownload = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Return early if the ImageView has disappeared.
                    if (null == mImageViewRef.get()) {
                        return;
                    }

                    // Now we're not on the main thread we can check all caches
                    CacheableBitmapDrawable result;

                    result = mCache.get(url, null);

                    if (null == result || profile) {

                        String mUrl = url;

                        if (url.contains("twitpic")) {
                            try {
                                URL address = new URL(url);
                                HttpURLConnection connection = (HttpURLConnection) address.openConnection(Proxy.NO_PROXY);
                                connection.setConnectTimeout(1000);
                                connection.setInstanceFollowRedirects(false);
                                connection.setReadTimeout(1000);
                                connection.connect();
                                String expandedURL = connection.getHeaderField("Location");
                                if(expandedURL != null) {
                                    mUrl = expandedURL;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // The bitmap isn't cached so download from the web
                        HttpURLConnection conn = (HttpURLConnection) new URL(mUrl).openConnection();
                        InputStream is = new BufferedInputStream(conn.getInputStream());

                        Bitmap b = decodeSampledBitmapFromResourceMemOpt(is, 1000, 1000);

                        try {
                            is.close();
                        } catch (Exception e) {

                        }
                        try {
                            conn.disconnect();
                        } catch (Exception e) {

                        }

                        // Add to cache
                        if (b != null) {
                            result = mCache.put(mUrl, b);
                        }
                    }

                    final CacheableBitmapDrawable fResult = result;
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final ImageView iv = mImageViewRef.get();

                                if (null != iv && iv.getVisibility() != View.GONE) {
                                    iv.setImageDrawable(fResult);
                                    Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

                                    iv.startAnimation(fadeInAnimation);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (IOException e) {
                    Log.e("ImageUrlAsyncTask", e.toString());
                } catch (OutOfMemoryError e) {
                    Log.v("ImageUrlAsyncTask", "Out of memory error here");
                } catch (Exception e) {
                    // something else
                    e.printStackTrace();
                }
            }
        });

        imageDownload.setPriority(8);
        imageDownload.start();
    }

    public static Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }


    public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static int getBrightness(String color) {
        return getBrightness((int)Long.parseLong(color, 16));
    }
    public static int getBrightness(int colorInt) {

        int r = (colorInt >> 16) & 0xFF;
        int g = (colorInt >> 8) & 0xFF;
        int b = (colorInt >> 0) & 0xFF;

        return (int) Math.sqrt(
                r * r * .241 +
                        g * g * .691 +
                        b * b * .068);
    }

    private static final int GROUP_RES = 1000;

    public static Bitmap combineBitmaps(Context context, Bitmap[] bitmaps) {
        int size = Utils.toDP(GROUP_RES, context);
        // need to make them square
        for (int i = 0; i < bitmaps.length; i++) {
            Bitmap currentImage = bitmaps[i];

            if (currentImage == null) {
                return bitmaps[0];
            }

            if (currentImage.getWidth() >= currentImage.getHeight()){
                currentImage = Bitmap.createBitmap(
                        currentImage,
                        currentImage.getWidth() / 2 - currentImage.getHeight() / 2,
                        0,
                        currentImage.getHeight(),
                        currentImage.getHeight()
                );
            } else {
                currentImage = Bitmap.createBitmap(
                        currentImage,
                        0,
                        currentImage.getHeight()/2 - currentImage.getWidth()/2,
                        currentImage.getWidth(),
                        currentImage.getWidth()
                );
            }

            bitmaps[i] = Bitmap.createScaledBitmap(currentImage, GROUP_RES, GROUP_RES, true);
        }

        try {
            switch (bitmaps.length) {
                case 2:
                    Bitmap image = Bitmap.createBitmap(GROUP_RES, GROUP_RES, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(image);
                    canvas.drawBitmap(bitmaps[0], 0, 0, null);
                    canvas.drawBitmap(bitmaps[1], GROUP_RES / 2, 0, null);

                    Paint linePaint = new Paint();
                    linePaint.setStrokeWidth(1f);
                    linePaint.setColor(context.getResources().getColor(R.color.circle_outline_dark));

                    canvas.drawLine(GROUP_RES / 2, 0, GROUP_RES / 2, GROUP_RES, linePaint);
                    return image;
                case 3:
                    image = Bitmap.createBitmap(GROUP_RES, GROUP_RES, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(image);
                    canvas.drawBitmap(bitmaps[0], 0, 0, null);
                    canvas.drawBitmap(bitmaps[1], GROUP_RES / 2, 0, null);
                    canvas.drawBitmap(bitmaps[2], GROUP_RES / 2, GROUP_RES / 2, null);

                    linePaint = new Paint();
                    linePaint.setStrokeWidth(1f);
                    linePaint.setColor(context.getResources().getColor(R.color.circle_outline_dark));

                    canvas.drawLine(GROUP_RES / 2, 0, GROUP_RES / 2, GROUP_RES, linePaint);
                    canvas.drawLine(GROUP_RES / 2, GROUP_RES / 2, GROUP_RES, GROUP_RES / 2, linePaint);
                    return image;
                case 4:
                    image = Bitmap.createBitmap(GROUP_RES, GROUP_RES, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(image);
                    canvas.drawBitmap(bitmaps[0], 0, 0, null);
                    canvas.drawBitmap(bitmaps[1], GROUP_RES / 2, 0, null);
                    canvas.drawBitmap(bitmaps[2], GROUP_RES / 2, GROUP_RES / 2, null);
                    canvas.drawBitmap(bitmaps[3], 0, GROUP_RES / 2, null);

                    linePaint = new Paint();
                    linePaint.setStrokeWidth(1f);
                    linePaint.setColor(context.getResources().getColor(R.color.circle_outline_dark));

                    canvas.drawLine(GROUP_RES / 2, 0, GROUP_RES / 2, GROUP_RES, linePaint);
                    canvas.drawLine(0, GROUP_RES / 2, GROUP_RES, GROUP_RES / 2, linePaint);
                    return image;
            }
        } catch (Exception e) {
            // fall through if an exception occurs and just show the default image
        }

        return bitmaps[0];
    }

}