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

    @Override
    public void setScale(float scale) {
        super.setScale(scale);
    }

    @Override
    public void onFling(float startX, float startY, float velocityX, float velocityY) {
        if ((velocityY > 3000 || velocityY < -3000) &&
                (velocityX < 7000 && velocityX > -7000)) {
            ((Activity) getImageView().getContext()).onBackPressed();
        } else {
            super.onFling(startX, startY, velocityX, velocityY);
        }
    }
}
