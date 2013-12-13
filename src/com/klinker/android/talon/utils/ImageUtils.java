package com.klinker.android.talon.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;

import java.io.File;
import java.net.URL;

import twitter4j.Status;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;


public class ImageUtils {

    public static void loadTwitterBackgroundBlurred(Context context, String url, NetworkedCacheableImageView iv) {

        new GetBlurred(context, url, iv).execute();
    }

    public static void loadCircleImage(Context context, String url, NetworkedCacheableImageView iv) {

        new GetCircle(context, url, iv).execute();

    }

    private static Bitmap getCircle(Bitmap currentImage) {
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

    public static Bitmap blur(Bitmap image, Context context) {

        RenderScript rs = RenderScript.create(context);
        Bitmap blurred = image;
        Allocation input = Allocation.createFromBitmap(rs, image, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        Allocation output = Allocation.createTyped(rs, input.getType());
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setRadius((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, context.getResources().getDisplayMetrics()));
        script.setInput(input);
        script.forEach(output);
        output.copyTo(blurred);

        return blurred;
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
            File cacheDir = new File(context.getCacheDir(), "talon");
            cacheDir.mkdirs();

            BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
            builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
            builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir);

            final BitmapLruCache mCache = builder.build();

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
            File cacheDir = new File(context.getCacheDir(), "talon");
            cacheDir.mkdirs();

            BitmapLruCache.Builder builder = new BitmapLruCache.Builder();
            builder.setMemoryCacheEnabled(true).setMemoryCacheMaxSizeUsingHeapSize();
            builder.setDiskCacheEnabled(true).setDiskCacheLocation(cacheDir);

            final BitmapLruCache mCache = builder.build();

            CacheableBitmapDrawable wrapper = mCache.get(url);

            if (wrapper == null) {

                try {
                    URL mUrl = new URL(url);

                    Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
                    image = blur(image, context);

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
