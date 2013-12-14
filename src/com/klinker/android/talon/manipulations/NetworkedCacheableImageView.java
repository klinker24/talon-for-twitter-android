package com.klinker.android.talon.manipulations;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.klinker.android.talon.utils.App;
import com.klinker.android.talon.utils.ImageUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import uk.co.senab.bitmapcache.CacheableImageView;

/**
 * Simple extension of CacheableImageView which allows downloading of Images of the Internet.
 *
 * This code isn't production quality, but works well enough for this sample.s
 *
 * @author Chris Banes
 */
public class NetworkedCacheableImageView extends CacheableImageView {

    public static final int BLUR = 1;
    public static final int CIRCLE = 2;

    public interface OnImageLoadedListener {
        void onImageLoaded(CacheableBitmapDrawable result);
    }

    private static class ImageUrlAsyncTask
            extends AsyncTask<String, Void, CacheableBitmapDrawable> {

        private final BitmapLruCache mCache;

        private final WeakReference<ImageView> mImageViewRef;
        private final OnImageLoadedListener mListener;

        private final BitmapFactory.Options mDecodeOpts;

        private int transform;
        private Context context;

        ImageUrlAsyncTask(Context context, ImageView imageView, BitmapLruCache cache,
                          BitmapFactory.Options decodeOpts, OnImageLoadedListener listener) {
            this.context = context;
            mCache = cache;
            mImageViewRef = new WeakReference<ImageView>(imageView);
            mListener = listener;
            mDecodeOpts = decodeOpts;
            transform = 0;
        }

        ImageUrlAsyncTask(Context context, ImageView imageView, BitmapLruCache cache,
                          BitmapFactory.Options decodeOpts, OnImageLoadedListener listener, int transform) {
            this.context = context;
            mCache = cache;
            mImageViewRef = new WeakReference<ImageView>(imageView);
            mListener = listener;
            mDecodeOpts = decodeOpts;
            this.transform = transform;
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
                CacheableBitmapDrawable result = mCache.get(url, mDecodeOpts);

                if (null == result) {
                    Log.d("ImageUrlAsyncTask", "Downloading: " + url);

                    // The bitmap isn't cached so download from the web
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    InputStream is = new BufferedInputStream(conn.getInputStream());

                    // Add to cache
                    result = mCache.put(url, is, mDecodeOpts);
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

            ImageView iv = mImageViewRef.get();
            if (null != iv) {
                if (transform == 0)
                    iv.setImageDrawable(result);
                else if (transform == CIRCLE)
                    iv.setImageBitmap(ImageUtils.getCircle(result.getBitmap(), context));
                else if (transform == BLUR)
                    iv.setImageBitmap(ImageUtils.blur(result.getBitmap()));
            }

            if (null != mListener) {
                mListener.onImageLoaded(result);
            }
        }
    }

    private final BitmapLruCache mCache;

    private ImageUrlAsyncTask mCurrentTask;

    public NetworkedCacheableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCache = App.getInstance(context).getBitmapCache();
    }

    /**
     * Loads the Bitmap.
     *
     * @param url      - URL of image
     * @param fullSize - Whether the image should be kept at the original size
     * @return true if the bitmap was found in the cache
     */
    public boolean loadImage(String url, final boolean fullSize, OnImageLoadedListener listener) {
        // First check whether there's already a task running, if so cancel it
        if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }

        // Check to see if the memory cache already has the bitmap. We can
        // safely do
        // this on the main thread.
        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper) {
            // The cache has it, so just display it
            setImageDrawable(wrapper);
            return true;
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            setImageDrawable(null);

            BitmapFactory.Options decodeOpts = null;

            if (!fullSize) {
                //decodeOpts = new BitmapFactory.Options();
                //decodeOpts.inSampleSize = 2;
            }

            mCurrentTask = new ImageUrlAsyncTask(getContext(), this, mCache, decodeOpts, listener, 0);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SDK11.executeOnThreadPool(mCurrentTask, url);
                } else {
                    mCurrentTask.execute(url);
                }
            } catch (RejectedExecutionException e) {
                // This shouldn't happen, but might.
            }

            return false;
        }
    }


    public boolean loadImage(String url, final boolean fullSize, OnImageLoadedListener listener, int transform) {
        // First check whether there's already a task running, if so cancel it
        if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }

        // Check to see if the memory cache already has the bitmap. We can
        // safely do
        // this on the main thread.
        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper) {
            // The cache has it, so just display it
            if (transform == CIRCLE) {
                setImageBitmap(ImageUtils.getCircle(wrapper.getBitmap(), getContext()));
            } else { //transform is blur
                setImageBitmap(ImageUtils.blur(wrapper.getBitmap()));
            }
            return true;
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            setImageDrawable(null);

            BitmapFactory.Options decodeOpts = null;

            if (!fullSize) {
                //decodeOpts = new BitmapFactory.Options();
                //decodeOpts.inSampleSize = 2;
            }

            mCurrentTask = new ImageUrlAsyncTask(getContext(), this, mCache, decodeOpts, listener, transform);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    SDK11.executeOnThreadPool(mCurrentTask, url);
                } else {
                    mCurrentTask.execute(url);
                }
            } catch (RejectedExecutionException e) {
                // This shouldn't happen, but might.
            }

            return false;
        }
    }
}