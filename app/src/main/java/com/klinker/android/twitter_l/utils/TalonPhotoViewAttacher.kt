package com.klinker.android.twitter_l.utils

import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import uk.co.senab.photoview.PhotoViewAttacher
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.activities.media_viewer.image.OnSwipeListener


class TalonPhotoViewAttacher(private val activity: AppCompatActivity, imageView: ImageView) : PhotoViewAttacher(imageView) {

    var toolbar: Toolbar? = null

    private var xCoOrdinate: Float = 0.toFloat()
    private var yCoOrdinate:Float = 0.toFloat()
    private val screenCenterX: Double
    private val screenCenterY: Double
    private val maxHypo: Double
    private var alpha: Int = 0

    private val view: View

    init {
        val display = activity.resources.displayMetrics
        screenCenterX = (display.widthPixels / 2).toDouble()
        screenCenterY = (display.heightPixels / 2).toDouble()
        maxHypo = Math.hypot(screenCenterX, screenCenterY)

        view = activity.findViewById<View>(R.id.background)
        view.background.alpha = 255
    }

    private var isAnimating = false

    private val gestureDetector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(activity, object : OnSwipeListener() {
            override fun onSwipe(direction: Direction): Boolean {
                return direction == Direction.down || direction == Direction.up
            }
        })
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (scale == 1f && event.pointerCount == 1 && (gestureDetector.onTouchEvent(event) || isAnimating)) {
            isAnimating = true

            val centerYPos = imageView.y + imageView.height / 2
            val centerXPos = imageView.x + imageView.width / 2
            val a = screenCenterX - centerXPos
            val b = screenCenterY - centerYPos
            val hypo = Math.hypot(a, b)

            /**
             * change alpha of background of layout
             */
            alpha = (hypo * 255).toInt() / maxHypo.toInt()
            if (alpha < 255) {
                view.background.alpha = 255 - alpha

                val scaledAlpha = ((255f - alpha.toFloat()) / 255f)
                toolbar?.alpha = if (scaledAlpha < .65f) 0f else scaledAlpha
            }

            val scale = if (1 - (hypo / maxHypo).toFloat() < .7) .7f else 1 - (hypo / maxHypo).toFloat()
            imageView.scaleX = scale
            imageView.scaleY = scale

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    xCoOrdinate = imageView.x - event.rawX
                    yCoOrdinate = imageView.y - event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    imageView.animate().x(event.rawX + xCoOrdinate).y(event.rawY + yCoOrdinate).setDuration(0).start()
                }
                MotionEvent.ACTION_UP -> {
                    isAnimating = false

                    if (alpha > 70) {
                        activity.supportFinishAfterTransition()
                        return false
                    } else {
                        imageView.animate().x(0f).y(screenCenterY.toFloat() - imageView.height / 2).setDuration(100).start()
                        imageView.scaleX = 1f
                        imageView.scaleY = 1f

                        view.background.alpha = 255
                        toolbar?.animate()?.alpha(1.0f)?.setDuration(200)?.start()
                    }
                    return false
                }
                else -> return false
            }

            return true
        } else {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    xCoOrdinate = imageView.x - event.rawX
                    yCoOrdinate = imageView.y - event.rawY
                }
            }

            return super.onTouch(v, event)
        }
    }

    override fun onGlobalLayout() {
        try {
            super.onGlobalLayout()
        } catch (e: Exception) {
        }
    }
}
