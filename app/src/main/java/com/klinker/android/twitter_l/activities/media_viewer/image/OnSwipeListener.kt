package com.klinker.android.twitter_l.activities.media_viewer.image

import android.view.GestureDetector
import android.view.MotionEvent


open class OnSwipeListener : GestureDetector.SimpleOnGestureListener() {

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || e2 == null) {
            return false
        }

        // Grab two events located on the plane at e1=(x1, y1) and e2=(x2, y2)
        // Let e1 be the initial event
        // e2 can be located at 4 different positions, consider the following diagram
        // (Assume that lines are separated by 90 degrees.)
        //
        //
        //         \ A  /
        //          \  /
        //       D   e1   B
        //          /  \
        //         / C  \
        //
        // So if (x2,y2) falls in region:
        //  A => it's an UP swipe
        //  B => it's a RIGHT swipe
        //  C => it's a DOWN swipe
        //  D => it's a LEFT swipe
        //

        val x1 = e1.x
        val y1 = e1.y

        val x2 = e2.x
        val y2 = e2.y

        val direction = getDirection(x1, y1, x2, y2)
        return onSwipe(direction)
    }

    /** Override this method. The Direction enum will tell you how the user swiped.  */
    open fun onSwipe(direction: Direction): Boolean {
        return false
    }

    /**
     * Given two points in the plane p1=(x1, x2) and p2=(y1, y1), this method
     * returns the direction that an arrow pointing from p1 to p2 would have.
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the direction
     */
    fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): Direction {
        val angle = getAngle(x1, y1, x2, y2)
        return Direction[angle]
    }

    /**
     *
     * Finds the angle between two points in the plane (x1,y1) and (x2, y2)
     * The angle is measured with 0/360 being the X-axis to the right, angles
     * increase counter clockwise.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the angle between two points
     */
    fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {

        val rad = Math.atan2((y1 - y2).toDouble(), (x2 - x1).toDouble()) + Math.PI
        return (rad * 180 / Math.PI + 180) % 360
    }


    enum class Direction {
        up,
        down,
        left,
        right;


        companion object {

            /**
             * Returns a direction given an angle.
             * Directions are defined as follows:
             *
             * Up: [45, 135]
             * Right: [0,45] and [315, 360]
             * Down: [225, 315]
             * Left: [135, 225]
             *
             * @param angle an angle from 0 to 360 - e
             * @return the direction of an angle
             */
            operator fun get(angle: Double): Direction {
                return if (inRange(angle, 45f, 135f)) {
                    up
                } else if (inRange(angle, 0f, 45f) || inRange(angle, 315f, 360f)) {
                    right
                } else if (inRange(angle, 225f, 315f)) {
                    down
                } else {
                    left
                }

            }

            /**
             * @param angle an angle
             * @param init the initial bound
             * @param end the final bound
             * @return returns true if the given angle is in the interval [init, end).
             */
            private fun inRange(angle: Double, init: Float, end: Float): Boolean {
                return angle >= init && angle < end
            }
        }
    }
}