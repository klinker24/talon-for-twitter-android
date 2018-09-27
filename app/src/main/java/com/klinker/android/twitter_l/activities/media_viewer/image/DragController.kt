package com.klinker.android.twitter_l.activities.media_viewer.image

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MotionEvent
import android.view.View
import com.klinker.android.twitter_l.R

class DragController(private val activity: AppCompatActivity, private val draggableView: View) {

    private val toolbar: Toolbar by lazy { activity.findViewById<View>(R.id.toolbar) as Toolbar }
    private val countLayout: View? by lazy { activity.findViewById<View>(R.id.show_info) }
    private val background: View by lazy { activity.findViewById<View>(R.id.background) }

    private val screenCenterX: Double
    private val screenCenterY: Double
    private val maxHypo: Double

    private var currentX: Float = 0.toFloat()
    private var currentY:Float = 0.toFloat()

    internal var isDragging = false

    init {
        val display = activity.resources.displayMetrics
        screenCenterX = (display.widthPixels / 2).toDouble()
        screenCenterY = (display.heightPixels / 2).toDouble()
        maxHypo = Math.hypot(screenCenterX, screenCenterY)

        background.background.alpha = 255
    }

    fun trackTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentX = draggableView.x - event.rawX
                currentY = draggableView.y - event.rawY
            }
        }
    }

    fun onTouch(event: MotionEvent): Boolean {
        isDragging = true

        val centerYPos = draggableView.y + draggableView.height / 2
        val centerXPos = draggableView.x + draggableView.width / 2
        val hypo = Math.hypot(screenCenterX - centerXPos, screenCenterY - centerYPos)

        adjustAlpha(hypo)
        adjustScale(hypo)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> trackTouch(event)
            MotionEvent.ACTION_MOVE -> draggableView.animate().x(event.rawX + currentX).y(event.rawY + currentY).setDuration(0).start()
            MotionEvent.ACTION_UP -> {
                isDragging = false

                if (background.background.alpha < 100) {
                    activity.supportFinishAfterTransition()
                } else {
                    resetImage()
                }

                return false
            }
            else -> return false
        }

        return true
    }

    private fun adjustAlpha(hypo: Double) {
        val alpha = ((hypo * 255).toInt() / maxHypo.toInt()) * 2
        val scaledAlpha = background.background.alpha.toFloat() / 255f

        if (alpha < 255) {
            background.background.alpha = 255 - alpha
            toolbar.alpha = if (scaledAlpha < .45f) 0f else scaledAlpha

            if (countLayout?.visibility == View.VISIBLE) {
                countLayout?.alpha = if (scaledAlpha < .45f) 0f else scaledAlpha
            }
        }
    }

    private fun adjustScale(hypo: Double) {
        val scale = if (1 - (hypo / maxHypo).toFloat() < .7) .7f
                    else 1 - (hypo / maxHypo).toFloat()

        draggableView.scaleX = scale
        draggableView.scaleY = scale
    }

    private fun resetImage() {
        draggableView.animate()
                .x(0f).y(screenCenterY.toFloat() - draggableView.height / 2)
                .scaleX(1f).scaleY(1f)
                .setDuration(100)
                .start()

        background.background.alpha = 255
        toolbar.animate().alpha(1.0f).setDuration(150).start()
    }
}