package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class TalonPhotoViewAttacher extends PhotoViewAttacher {

    public TalonPhotoViewAttacher(ImageView imageView) {
        super(imageView);
    }

    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        try {
            return super.onTouch(v, ev);
        } catch (Throwable t) {
            return false;
        }
    }
}
