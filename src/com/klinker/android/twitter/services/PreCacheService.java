package com.klinker.android.twitter.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.klinker.android.twitter.data.App;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.ImageUtils;

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
        BitmapLruCache mCache = App.getInstance(this).getBitmapCache();
        AppSettings settings = AppSettings.getInstance(this);
        Cursor cursor = HomeDataSource.getInstance(this).getUnreadCursor(settings.currentAccount);

        if (cursor.moveToFirst()) {
            do {
                String profilePic = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PRO_PIC));
                String imageUrl = cursor.getString(cursor.getColumnIndex(HomeSQLiteHelper.COLUMN_PIC_URL));

                CacheableBitmapDrawable wrapper = mCache.get(profilePic);
                if (wrapper == null) {

                    try {
                        URL mUrl = new URL(profilePic);

                        Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());
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
                            URL mUrl = new URL(imageUrl);
                            Bitmap image = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());

                            mCache.put(profilePic, image);
                        } catch (Exception e) {

                        }
                    }
                }

            } while (cursor.moveToNext());
        }
    }
}
