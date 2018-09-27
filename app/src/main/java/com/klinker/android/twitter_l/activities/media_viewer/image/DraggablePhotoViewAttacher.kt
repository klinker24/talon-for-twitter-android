package com.klinker.android.twitter_l.activities.media_viewer.image

import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import uk.co.senab.photoview.PhotoViewAttacher
import androidx.core.view.GestureDetectorCompat
import androidx.appcompat.app.AppCompatActivity

class DraggablePhotoViewAttacher(private val activity: AppCompatActivity, imageView: ImageView) : PhotoViewAttacher(imageView) {

    private val dragController = DragController(activity, imageView)

    private val gestureDetector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(activity, object : OnSwipeListener() {
            override fun onSwipe(direction: Direction): Boolean {
                return direction == Direction.UP || direction == Direction.DOWN
            }
        })
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return if (scale == 1f && event.pointerCount == 1 && (gestureDetector.onTouchEvent(event) || dragController.isDragging)) {
            dragController.onTouch(event)
        } else {
            dragController.trackTouch(event)
            super.onTouch(v, event)
        }
    }

    override fun onGlobalLayout() {
        try {
            super.onGlobalLayout()
        } catch (e: Exception) {
        }
    }

    override fun onFling(startX: Float, startY: Float, velocityX: Float, velocityY: Float) {
        try {
            super.onFling(startX, startY, velocityX, velocityY)
        } catch (e: Exception) {

        }
    }

    override fun onDrag(dx: Float, dy: Float) {
        try {
            super.onDrag(dx, dy)
        } catch (e: Exception) {

        }
    }
}
