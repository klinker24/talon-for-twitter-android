package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;

import android.widget.TextView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.data.sq_lite.InteractionsSQLiteHelper;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.utils.ImageUtils;
import com.klinker.android.twitter_l.utils.SDK11;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.RejectedExecutionException;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;

/**
 * Created by luke on 1/7/14.
 */
public class InteractionsCursorAdapter extends CursorAdapter {

    public Context context;
    public Cursor cursor;
    public LayoutInflater inflater;
    public AppSettings settings;

    public int layout;
    public Resources res;
    public BitmapLruCache mCache;
    public int border;
    public ColorDrawable color;
    public ColorDrawable transparent;

    public Handler mHandler;

    public static class ViewHolder {
        public TextView title;
        public TextView text;
        public NetworkedCacheableImageView picture;
        public LinearLayout background;
        public String check;
    }

    public InteractionsCursorAdapter(Context context, Cursor cursor) {

        super(context, cursor, 0);

        this.context = context;
        this.cursor = cursor;
        this.inflater = LayoutInflater.from(context);

        settings = AppSettings.getInstance(context);

        mHandler = new Handler();

        setUpLayout();
    }

    public void setUpLayout() {
        layout = R.layout.interaction;

        TypedArray b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.circleBorder});

        border = b.getResourceId(0, 0);
        b.recycle();

        mCache = App.getInstance(context).getBitmapCache();

        b = context.getTheme().obtainStyledAttributes(new int[]{R.attr.message_color});
        color = new ColorDrawable(context.getResources().getColor(b.getResourceId(0, 0)));
        b.recycle();

        transparent = new ColorDrawable(context.getResources().getColor(android.R.color.transparent));
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View v = null;
        final ViewHolder holder = new ViewHolder();

        v = inflater.inflate(layout, viewGroup, false);

        holder.title = (TextView) v.findViewById(R.id.title);
        holder.text = (TextView) v.findViewById(R.id.text);
        holder.background = (LinearLayout) v.findViewById(R.id.background);
        holder.picture = (NetworkedCacheableImageView) v.findViewById(R.id.picture);

        holder.picture.setClipToOutline(true);

        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        final String title = cursor.getString(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_TITLE));
        final String text = cursor.getString(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_TEXT));
        final String url = cursor.getString(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_PRO_PIC));
        final int unread = cursor.getInt(cursor.getColumnIndex(InteractionsSQLiteHelper.COLUMN_UNREAD));
        holder.check = title;

        holder.title.setText(Html.fromHtml(title));

        if(!text.equals("")) {
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(text);
        } else {
            holder.text.setVisibility(View.GONE);
        }


        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (holder.check.equals(title)) {
                    loadImage(context, holder, url, mCache, title);
                }
            }
        }, 500);

        // set the background color
        if (unread == 1) {
            //holder.background.setBackgroundDrawable(color);
            holder.background.setBackgroundDrawable(transparent);
        } else {
            holder.background.setBackgroundDrawable(transparent);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (!cursor.moveToPosition(cursor.getCount() - 1 - position)) {
            throw new IllegalStateException("couldn't move cursor to position " + position);
        }

        View v;
        if (convertView == null) {

            v = newView(context, cursor, parent);

        } else {
            v = convertView;

            final ViewHolder holder = (ViewHolder) v.getTag();

            holder.picture.setImageDrawable(context.getResources().getDrawable(border));
        }

        bindView(v, context, cursor);

        return v;
    }

    // used to place images on the timeline
    public static ImageUrlAsyncTask mCurrentTask;

    public void loadImage(Context context, final ViewHolder holder, final String url, BitmapLruCache mCache, final String title) {
        // First check whether there's already a task running, if so cancel it
        /*if (null != mCurrentTask) {
            mCurrentTask.cancel(true);
        }*/

        if (url == null) {
            return;
        }

        BitmapDrawable wrapper = mCache.getFromMemoryCache(url);

        if (null != wrapper && holder.picture.getVisibility() != View.GONE) {
            // The cache has it, so just display it
            holder.picture.setImageDrawable(wrapper);
            Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

            holder.picture.startAnimation(fadeInAnimation);
        } else {
            // Memory Cache doesn't have the URL, do threaded request...
            holder.picture.setImageDrawable(null);

            mCurrentTask = new ImageUrlAsyncTask(context, holder, mCache, title);

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

        private BitmapLruCache mCache;
        private Context context;
        private ViewHolder holder;
        private String title;

        ImageUrlAsyncTask(Context context, ViewHolder holder, BitmapLruCache cache, String title) {
            this.context = context;
            mCache = cache;
            this.holder = holder;
            this.title = title;
        }

        @Override
        protected CacheableBitmapDrawable doInBackground(String... params) {
            try {
                if (!holder.check.equals(title)) {
                    return null;
                }
                final String url = params[0];

                // Now we're not on the main thread we can check all caches
                CacheableBitmapDrawable result;

                result = mCache.get(url, null);

                if (null == result) {

                    // The bitmap isn't cached so download from the web
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    InputStream is = new BufferedInputStream(conn.getInputStream());

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;

                    Bitmap b = decodeSampledBitmapFromResourceMemOpt(is, 500, 500);

                    // Add to cache
                    if (b != null) {
                        try {
                            result = mCache.put(url, b);
                        } catch (Exception e) {
                            result = null;
                        }
                    }

                    try {
                        is.close();
                    } catch (Exception e) {

                    }
                    try {
                        conn.disconnect();
                    } catch (Exception e) {

                    }

                }

                return result;

            } catch (IOException e) {
                Log.e("ImageUrlAsyncTask", e.toString());
            } catch (OutOfMemoryError e) {
                Log.v("ImageUrlAsyncTask", "Out of memory error here");
            }

            return null;
        }

        public Bitmap decodeSampledBitmapFromResourceMemOpt(
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

                options.inSampleSize = calculateInSampleSize(options, reqWidth,
                        reqHeight);
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

        @Override
        protected void onPostExecute(CacheableBitmapDrawable result) {
            super.onPostExecute(result);

            try {
                if (result != null && holder.check.equals(title)) {
                    holder.picture.setImageDrawable(result);
                    Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);

                    holder.picture.startAnimation(fadeInAnimation);
                }

            } catch (Exception e) {

            }
        }
    }

}
