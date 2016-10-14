package com.klinker.android.twitter_l.utils;
/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;


public class ImageUtils {

    private static final long MAX_PICTURE_UPLOAD_SIZE = 1024 * 1024 * 3;

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }

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

            try {
                bitmaps[i] = Bitmap.createScaledBitmap(currentImage, GROUP_RES, GROUP_RES, true);
            } catch (Exception e) {

            }
        }

        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(Utils.toDP(1, context));
        linePaint.setColor(context.getResources().getColor(android.R.color.white));

        Paint background = new Paint();
        background.setColor(Color.WHITE);

        try {
            switch (bitmaps.length) {
                case 2:
                    Bitmap image = Bitmap.createBitmap(GROUP_RES, GROUP_RES, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(image);
                    canvas.drawBitmap(bitmaps[0], 0, 0, background);
                    canvas.drawBitmap(bitmaps[1], GROUP_RES / 2, 0, background);

                    canvas.drawLine(GROUP_RES / 2, 0, GROUP_RES / 2, GROUP_RES, linePaint);
                    return image;
                case 3:
                    image = Bitmap.createBitmap(GROUP_RES, GROUP_RES, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(image);
                    canvas.drawBitmap(bitmaps[0], 0, 0, background);
                    canvas.drawBitmap(bitmaps[1], GROUP_RES / 2, 0, background);
                    canvas.drawBitmap(bitmaps[2], GROUP_RES / 2, GROUP_RES / 2, background);

                    canvas.drawLine(GROUP_RES / 2, 0, GROUP_RES / 2, GROUP_RES, linePaint);
                    canvas.drawLine(GROUP_RES / 2, GROUP_RES / 2, GROUP_RES, GROUP_RES / 2, linePaint);
                    return image;
                case 4:
                    image = Bitmap.createBitmap(GROUP_RES, GROUP_RES, Bitmap.Config.ARGB_8888);
                    canvas = new Canvas(image);
                    canvas.drawBitmap(bitmaps[0], 0, 0, background);
                    canvas.drawBitmap(bitmaps[1], GROUP_RES / 2, 0, background);
                    canvas.drawBitmap(bitmaps[2], 0, GROUP_RES / 2, background);
                    canvas.drawBitmap(bitmaps[3], GROUP_RES / 2, GROUP_RES / 2, background);

                    canvas.drawLine(GROUP_RES / 2, 0, GROUP_RES / 2, GROUP_RES, linePaint);
                    canvas.drawLine(0, GROUP_RES / 2, GROUP_RES, GROUP_RES / 2, linePaint);
                    return image;
            }
        } catch (Exception e) {
            // fall through if an exception occurs and just show the default image
        }

        return bitmaps[0];
    }


    /**
     * Scales a bitmap file to a lower resolution so that it can be sent over MMS. Most carriers
     * have a 1 MB limit, so we'll scale to under that. This method will create a new file in the
     * application memory.
     */
    public static File scaleToSend(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int arraySize = 0;
        int len;

        try {
            // convert the Uri to a byte array that we can manipulate
            while ((len = input.read(buffer)) > -1) {
                if (len != 0) {
                    if (arraySize + len > byteArr.length) {
                        byte[] newbuf = new byte[(arraySize + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, arraySize);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, arraySize, len);
                    arraySize += len;
                }
            }

            try {
                input.close();
            } catch(Exception e) { }

            // with inJustDecodeBounds, we are telling the system just to get the resolution
            // of the image and not to decode anything else. This resolution
            // is used to calculate the in sample size
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options);
            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;

            String fileName = ((int) (Math.random() * Integer.MAX_VALUE)) + ".jpg";

            // start generating bitmaps and checking the size against the max size
            Bitmap scaled = generateBitmap(byteArr, arraySize, srcWidth, srcHeight, 2000);
            File file = createFileFromBitmap(context, fileName, scaled);

            int maxResolution = 1500;
            while (maxResolution > 0 && file.length() > MAX_PICTURE_UPLOAD_SIZE) {
                scaled.recycle();

                scaled = generateBitmap(byteArr, arraySize, srcWidth, srcHeight, maxResolution);
                file = createFileFromBitmap(context, fileName, scaled);
                maxResolution -= 250;
            }

            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap generateBitmap(byte[] byteArr, int arraySize, int srcWidth, int srcHeight, int maxSize) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // in sample size reduces the size of the image by this factor of 2
        options.inSampleSize = calculateInSampleSize(srcHeight, srcWidth, maxSize);

        // these options set up the image coloring
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inDither = true;

        // these options set it up with the actual dimensions that you are looking for
        options.inDensity = srcWidth;
        options.inTargetDensity = maxSize * options.inSampleSize;

        // now we actually decode the image to these dimensions
        return BitmapFactory.decodeByteArray(byteArr, 0, arraySize, options);
    }

    private static int calculateInSampleSize(int currentHeight, int currentWidth, int maxSize) {
        int scale = 1;

        if (currentHeight > maxSize || currentWidth > maxSize) {
            scale = (int) Math.pow(2, (int) Math.ceil(Math.log(maxSize /
                    (double) Math.max(currentHeight, currentWidth)) / Math.log(0.5)));
        }

        return scale;
    }

    private static File createFileFromBitmap(Context context, String name, Bitmap bitmap) {
        FileOutputStream out = null;
        File file = new File(context.getFilesDir(), name);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            Log.e("Scale to Send", "failed to write output stream", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e("Scale to Send", "failed to close output stream", e);
            }
        }

        return file;
    }
}