package com.klinker.android.twitter.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.SDK11;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;


public class ImageUtils {

    public static Bitmap getCircle(Bitmap currentImage, Context context) {
        int scale = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 96, context.getResources().getDisplayMetrics());

        Bitmap bitmap = currentImage;
        Bitmap output = Bitmap.createBitmap(scale,
                scale, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, scale,
                scale);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(scale / 2,
                scale / 2, (scale / 2) - (scale / 25), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        try {
            canvas.drawBitmap(bitmap, null, rect, paint);
        } catch (Exception e) {
            // bitmap is null i guess

        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.contact_picture_border));

        try {
            TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circle_border});
            int resource = a.getResourceId(0, 0);
            a.recycle();
            paint.setColor(context.getResources().getColor(resource));
        } catch (Exception e) {
            paint.setColor(context.getResources().getColor(R.color.circle_outline_dark));
        }

        canvas.drawCircle(scale / 2,
                scale / 2, (scale / 2) - (scale / 25), paint);

        return output;
    }

    public static Bitmap notificationResize(Context context, Bitmap currentImage) {
        int scale = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics());

        Bitmap bitmap = currentImage;
        Bitmap output = Bitmap.createBitmap(scale,
                scale, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, scale,
                scale);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawBitmap(bitmap, null, rect, paint);
        return output;
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

    static ImageUrlAsyncTask mCurrentTask;

    public static void loadImage(Context context, ImageView iv, String url, BitmapLruCache mCache) {
        // First check whether there's already a task running, if so cancel it
        /*if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }*/

        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper) {
            // The cache has it, so just display it
            iv.setImageDrawable(wrapper);
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            iv.setImageDrawable(null);

            mCurrentTask = new ImageUrlAsyncTask(context, iv, mCache, false);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SDK11.executeOnThreadPool(mCurrentTask, url);
                } else {
                    mCurrentTask.execute(url);
                }
            } catch (RejectedExecutionException e) {
                // This shouldn't happen, but might.
            }

        }
    }

    private static class ImageUrlAsyncTask
            extends AsyncTask<String, Void, CacheableBitmapDrawable> {

        private final BitmapLruCache mCache;
        private Context context;
        private final WeakReference<ImageView> mImageViewRef;
        private ImageView iv;
        private boolean thumbnail;

        ImageUrlAsyncTask(Context context, ImageView imageView, BitmapLruCache cache, boolean thumbnail) {
            this.context = context;
            mCache = cache;
            mImageViewRef = new WeakReference<ImageView>(imageView);
            iv = imageView;
            this.thumbnail = thumbnail;
        }

        @Override
        protected CacheableBitmapDrawable doInBackground(String... params) {
            try {
                // Return early if the ImageView has disappeared.
                if (null == mImageViewRef.get()) {
                    return null;
                }
                final String url = params[0];

                // Now we're not on the main thread we can check all caches
                CacheableBitmapDrawable result;

                result = mCache.get(url, null);

                if (null == result) {
                    Log.d("ImageUrlAsyncTask", "Downloading: " + url);

                    // The bitmap isn't cached so download from the web
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    InputStream is = new BufferedInputStream(conn.getInputStream());

                    //Bitmap b = BitmapFactory.decodeStream(is);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, options);

                    int size = calculateInSampleSize(options, 300, 300);
                    Log.v("caching_images", size +"");

                    options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = size;

                    HttpURLConnection conn2 = (HttpURLConnection) new URL(url).openConnection();
                    InputStream is2 = new BufferedInputStream(conn2.getInputStream());
                    Bitmap b = BitmapFactory.decodeStream(is2, null, options);

                    // Add to cache
                    result = mCache.put(url, b);

                } else {
                    Log.d("ImageUrlAsyncTask", "Got from Cache: " + url);
                }

                return result;

            } catch (IOException e) {
                Log.e("ImageUrlAsyncTask", e.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(CacheableBitmapDrawable result) {
            super.onPostExecute(result);

            try {
                ImageView iv = mImageViewRef.get();

                if (null != iv) {
                    iv.setImageDrawable(result);
                }

            } catch (Exception e) {

            }
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

    public static void loadCircleImage(Context context, ImageView iv, String url, BitmapLruCache mCache) {
        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper) {
            // The cache has it, so just display it
            iv.setImageDrawable(wrapper);
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            iv.setImageDrawable(null);

            ImageUrlCircleAsyncTask mCurrentTask = new ImageUrlCircleAsyncTask(context, iv, mCache, false);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SDK11.executeOnThreadPool(mCurrentTask, url);
                } else {
                    mCurrentTask.execute(url);
                }
            } catch (RejectedExecutionException e) {
                // This shouldn't happen, but might.
            }

        }
    }

    private static class ImageUrlCircleAsyncTask
            extends AsyncTask<String, Void, CacheableBitmapDrawable> {

        private final BitmapLruCache mCache;
        private Context context;
        private final WeakReference<ImageView> mImageViewRef;
        private ImageView iv;
        private boolean thumbnail;

        ImageUrlCircleAsyncTask(Context context, ImageView imageView, BitmapLruCache cache, boolean thumbnail) {
            this.context = context;
            mCache = cache;
            mImageViewRef = new WeakReference<ImageView>(imageView);
            iv = imageView;
            this.thumbnail = thumbnail;
        }

        @Override
        protected CacheableBitmapDrawable doInBackground(String... params) {
            try {
                // Return early if the ImageView has disappeared.
                if (null == mImageViewRef.get()) {
                    return null;
                }
                final String url = params[0];

                // Now we're not on the main thread we can check all caches
                CacheableBitmapDrawable result;

                result = mCache.get(url, null);

                if (null == result) {
                    Log.d("ImageUrlAsyncTask", "Downloading: " + url);

                    // The bitmap isn't cached so download from the web
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    InputStream is = new BufferedInputStream(conn.getInputStream());

                    Bitmap b = BitmapFactory.decodeStream(is);
                    b = getCircle(b, context);

                    if (thumbnail) {
                        b = ImageUtils.overlayPlay(b, context);
                    }

                    // Add to cache
                    result = mCache.put(url, b);

                } else {
                    Log.d("ImageUrlAsyncTask", "Got from Cache: " + url);
                }

                return result;

            } catch (IOException e) {
                Log.e("ImageUrlAsyncTask", e.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(CacheableBitmapDrawable result) {
            super.onPostExecute(result);

            try {
                ImageView iv = mImageViewRef.get();

                if (null != iv) {
                    iv.setImageDrawable(result);
                }

            } catch (Exception e) {

            }
        }
    }
}