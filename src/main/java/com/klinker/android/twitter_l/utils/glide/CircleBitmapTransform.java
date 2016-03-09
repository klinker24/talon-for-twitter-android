package com.klinker.android.twitter_l.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.klinker.android.twitter_l.utils.ImageUtils;

public class CircleBitmapTransform extends BitmapTransformation {
    public CircleBitmapTransform(Context context) {
        super(context);
    }

    @Override
    protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        return ImageUtils.getCircleBitmap(toTransform);
    }

    @Override
    public String getId() {
        return null;
    }
}
