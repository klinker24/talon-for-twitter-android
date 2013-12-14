package com.klinker.android.talon.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.AsyncTask;

import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;

import java.io.File;
import java.net.URL;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;


public class ImageUtils {

    public static void loadTwitterBackgroundBlurred(Context context, String url, NetworkedCacheableImageView iv) {

        new GetBlurred(context, url, iv).execute();
    }

    public static void loadTwitterBackground(Context context, String url, NetworkedCacheableImageView iv) {
        new GetBackground(context, url, iv).execute();
    }

    public static void loadCircleImage(Context context, String url, NetworkedCacheableImageView iv) {

        new GetCircle(context, url, iv).execute();

    }

    public static Bitmap getCircle(Bitmap currentImage) {
        Bitmap bitmap = currentImage;
        Bitmap output = Bitmap.createBitmap(currentImage.getWidth(),
                currentImage.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, currentImage.getWidth(),
                currentImage.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(currentImage.getWidth() / 2,
                currentImage.getHeight() / 2, (currentImage.getWidth() / 2) - (currentImage.getWidth() / 25), paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }

    public static Bitmap blur(Bitmap sentBitmap) {

        int radius = 8;

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

    static class GetCircle extends AsyncTask<String, Void, Bitmap> {

        private Context context;
        private String url;
        private NetworkedCacheableImageView iv;

        public GetCircle(Context context, String url, NetworkedCacheableImageView iv) {
            this.context = context;
            this.url = url;
            this.iv = iv;
        }

        protected Bitmap doInBackground(String... urls) {

            final BitmapLruCache mCache = App.getInstance(context).getBitmapCache();

            CacheableBitmapDrawable wrapper = mCache.get(url);

            if (wrapper == null) {

                try {
                    URL mUrl = new URL(url);

                    Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                    image = getCircle(image);

                    wrapper = mCache.put(url, image);

                    return image;
                } catch (Exception e) {

                }
            }

            return null;
        }

        protected void onPostExecute(Bitmap image) {
            if (image != null) {
                iv.setImageBitmap(image);
            }
        }
    }

    static class GetBlurred extends AsyncTask<String, Void, Bitmap> {

        private Context context;
        private String url;
        private NetworkedCacheableImageView iv;

        public GetBlurred(Context context, String url, NetworkedCacheableImageView iv) {
            this.context = context;
            this.url = url;
            this.iv = iv;
        }

        protected Bitmap doInBackground(String... urls) {

            final BitmapLruCache mCache = App.getInstance(context).getBitmapCache();

            CacheableBitmapDrawable wrapper = mCache.get(url);

            if (wrapper == null) {

                try {
                    URL mUrl = new URL(url);

                    Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                    image = blur(image);

                    wrapper = mCache.put(url, image);

                    return image;
                } catch (Exception e) {

                }
            }

            return null;
        }

        protected void onPostExecute(Bitmap image) {
            if (image != null) {
                iv.setImageBitmap(image);
            }
        }
    }

    static class GetBackground extends AsyncTask<String, Void, Bitmap> {

        private Context context;
        private String url;
        private NetworkedCacheableImageView iv;

        public GetBackground(Context context, String url, NetworkedCacheableImageView iv) {
            this.context = context;
            this.url = url;
            this.iv = iv;
        }

        protected Bitmap doInBackground(String... urls) {

            final BitmapLruCache mCache = App.getInstance(context).getBitmapCache();

            CacheableBitmapDrawable wrapper = mCache.get(url);

            if (wrapper == null) {

                try {
                    URL mUrl = new URL(url);

                    Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());

                    wrapper = mCache.put(url, image);

                    return image;
                } catch (Exception e) {

                }
            }

            return null;
        }

        protected void onPostExecute(Bitmap image) {
            if (image != null) {
                iv.setImageBitmap(image);
            }
        }
    }
}
