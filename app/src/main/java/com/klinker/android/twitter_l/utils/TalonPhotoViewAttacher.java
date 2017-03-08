package com.klinker.android.twitter_l.utils;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import uk.co.senab.photoview.PhotoViewAttacher;
import android.os.Build;

import com.klinker.android.twitter_l.activities.media_viewer.PhotoPagerActivity;

public class TalonPhotoViewAttacher extends PhotoViewAttacher {

    private Activity activity;

    public TalonPhotoViewAttacher(Activity activity, ImageView imageView) {
        super(imageView);
        this.activity = activity;
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
    public void onGlobalLayout() {
        try { super.onGlobalLayout(); } catch (Exception e) { }
    }
    
    @Override
    public void onFling(float startX, float startY, float velocityX, float velocityY) {
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT || (activity != null && activity instanceof PhotoPagerActivity)) &&
                (velocityY > 3000 || velocityY < -3000) &&
                (velocityX < 7000 && velocityX > -7000)) {
            ((Activity) getImageView().getContext()).onBackPressed();
        } else {
            super.onFling(startX, startY, velocityX, velocityY);
        }
    }
}
