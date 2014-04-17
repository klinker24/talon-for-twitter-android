package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;

import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.ImageUtils;
import com.klinker.android.twitter.utils.Utils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;


public class PreCacheService extends IntentService {

    SharedPreferences sharedPrefs;

    public PreCacheService() {
        super("PreCacheService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

        // if they want it only over wifi and they are on mobile data
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pre_cache_wifi_only", false) &&
                Utils.getConnectionStatus(this)) {
            // just quit because we don't want it to happen
            return;
        }

        BitmapLruCache mCache = App.getInstance(this).getBitmapCache();
        AppSettings settings = AppSettings.getInstance(this);
        Cursor cursor = HomeDataSource.getInstance(this).getUnreadCursor(settings.currentAccount);

        if (cursor.moveToFirst()) {
            boolean cont = true;
            do {
                String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
                String imageUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));

                CacheableBitmapDrawable wrapper = mCache.get(profilePic);
                if (wrapper == null) {

                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(profilePic).openConnection();
                        InputStream is = new BufferedInputStream(conn.getInputStream());

                        Bitmap image = decodeSampledBitmapFromResourceMemOpt(is, 500, 500);

                        if (settings.roundContactImages) {
                            image = ImageUtils.getCircle(image, this);
                        }

                        mCache.put(profilePic, image);
                    } catch (Exception e) {

                    }
                }

                if (!imageUrl.equals("")) {
                    wrapper = mCache.get(imageUrl);
                    if (wrapper == null) {
                        try {
                            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
                            InputStream is = new BufferedInputStream(conn.getInputStream());

                            Bitmap image = decodeSampledBitmapFromResourceMemOpt(is, 500, 500);

                            mCache.put(imageUrl, image);
                        } catch (Exception e) {

                        } catch (OutOfMemoryError e) {
                            // just stop I suppose
                            cont = false;
                        }
                    }
                }

            } while (cursor.moveToNext() && cont);
        }
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
}
