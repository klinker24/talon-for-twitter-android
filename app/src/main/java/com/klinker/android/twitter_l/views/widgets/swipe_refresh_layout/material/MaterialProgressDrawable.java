//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path.FillType;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.Animation.AnimationListener;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

class MaterialProgressDrawable extends Drawable implements Animatable {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator END_CURVE_INTERPOLATOR = new MaterialProgressDrawable.EndCurveInterpolator();
    private static final Interpolator START_CURVE_INTERPOLATOR = new MaterialProgressDrawable.StartCurveInterpolator();
    private static final Interpolator EASE_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    static final int LARGE = 0;
    static final int DEFAULT = 1;
    private static final int CIRCLE_DIAMETER = 40;
    private static final float CENTER_RADIUS = 8.75F;
    private static final float STROKE_WIDTH = 2.5F;
    private static final int CIRCLE_DIAMETER_LARGE = 56;
    private static final float CENTER_RADIUS_LARGE = 12.5F;
    private static final float STROKE_WIDTH_LARGE = 3.0F;
    private final int[] COLORS = new int[]{-16777216};
    private static final int ANIMATION_DURATION = 1333;
    private static final float NUM_POINTS = 5.0F;
    private final ArrayList<Animation> mAnimators = new ArrayList();
    private final MaterialProgressDrawable.Ring mRing;
    private float mRotation;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 5;
    private static final float ARROW_OFFSET_ANGLE = 5.0F;
    private static final int ARROW_WIDTH_LARGE = 12;
    private static final int ARROW_HEIGHT_LARGE = 6;
    private static final float MAX_PROGRESS_ARC = 0.8F;
    private Resources mResources;
    private View mParent;
    private Animation mAnimation;
    private float mRotationCount;
    private double mWidth;
    private double mHeight;
    private Animation mFinishAnimation;
    private final Callback mCallback = new Callback() {
        public void invalidateDrawable(Drawable d) {
            MaterialProgressDrawable.this.invalidateSelf();
        }

        public void scheduleDrawable(Drawable d, Runnable what, long when) {
            MaterialProgressDrawable.this.scheduleSelf(what, when);
        }

        public void unscheduleDrawable(Drawable d, Runnable what) {
            MaterialProgressDrawable.this.unscheduleSelf(what);
        }
    };

    public MaterialProgressDrawable(Context context, View parent) {
        this.mParent = parent;
        this.mResources = context.getResources();
        this.mRing = new MaterialProgressDrawable.Ring(this.mCallback);
        this.mRing.setColors(this.COLORS);
        this.updateSizes(1);
        this.setupAnimators();
    }

    private void setSizeParameters(double progressCircleWidth, double progressCircleHeight, double centerRadius, double strokeWidth, float arrowWidth, float arrowHeight) {
        MaterialProgressDrawable.Ring ring = this.mRing;
        DisplayMetrics metrics = this.mResources.getDisplayMetrics();
        float screenDensity = metrics.density;
        this.mWidth = progressCircleWidth * (double)screenDensity;
        this.mHeight = progressCircleHeight * (double)screenDensity;
        ring.setStrokeWidth((float)strokeWidth * screenDensity);
        ring.setCenterRadius(centerRadius * (double)screenDensity);
        ring.setColorIndex(0);
        ring.setArrowDimensions(arrowWidth * screenDensity, arrowHeight * screenDensity);
        ring.setInsets((int)this.mWidth, (int)this.mHeight);
    }

    public void updateSizes(@MaterialProgressDrawable.ProgressDrawableSize int size) {
        if(size == 0) {
            this.setSizeParameters(56.0D, 56.0D, 12.5D, 3.0D, 12.0F, 6.0F);
        } else {
            this.setSizeParameters(40.0D, 40.0D, 8.75D, 2.5D, 10.0F, 5.0F);
        }

    }

    public void showArrow(boolean show) {
        this.mRing.setShowArrow(show);
    }

    public void setArrowScale(float scale) {
        this.mRing.setArrowScale(scale);
    }

    public void setStartEndTrim(float startAngle, float endAngle) {
        this.mRing.setStartTrim(startAngle);
        this.mRing.setEndTrim(endAngle);
    }

    public void setProgressRotation(float rotation) {
        this.mRing.setRotation(rotation);
    }

    public void setBackgroundColor(int color) {
        this.mRing.setBackgroundColor(color);
    }

    public void setColorSchemeColors(int... colors) {
        this.mRing.setColors(colors);
        this.mRing.setColorIndex(0);
    }

    public int getIntrinsicHeight() {
        return (int)this.mHeight;
    }

    public int getIntrinsicWidth() {
        return (int)this.mWidth;
    }

    public void draw(Canvas c) {
        Rect bounds = this.getBounds();
        int saveCount = c.save();
        c.rotate(this.mRotation, bounds.exactCenterX(), bounds.exactCenterY());
        this.mRing.draw(c, bounds);
        c.restoreToCount(saveCount);
    }

    public void setAlpha(int alpha) {
        this.mRing.setAlpha(alpha);
    }

    public int getAlpha() {
        return this.mRing.getAlpha();
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mRing.setColorFilter(colorFilter);
    }

    void setRotation(float rotation) {
        this.mRotation = rotation;
        this.invalidateSelf();
    }

    private float getRotation() {
        return this.mRotation;
    }

    public int getOpacity() {
        return -3;
    }

    public boolean isRunning() {
        ArrayList animators = this.mAnimators;
        int N = animators.size();

        for(int i = 0; i < N; ++i) {
            Animation animator = (Animation)animators.get(i);
            if(animator.hasStarted() && !animator.hasEnded()) {
                return true;
            }
        }

        return false;
    }

    public void start() {
        this.mAnimation.reset();
        this.mRing.storeOriginals();
        if(this.mRing.getEndTrim() != this.mRing.getStartTrim()) {
            this.mParent.startAnimation(this.mFinishAnimation);
        } else {
            this.mRing.setColorIndex(0);
            this.mRing.resetOriginals();
            this.mParent.startAnimation(this.mAnimation);
        }

    }

    public void stop() {
        this.mParent.clearAnimation();
        this.setRotation(0.0F);
        this.mRing.setShowArrow(false);
        this.mRing.setColorIndex(0);
        this.mRing.resetOriginals();
    }

    private void setupAnimators() {
        final MaterialProgressDrawable.Ring ring = this.mRing;
        Animation finishRingAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetRotation = (float)(Math.floor((double)(ring.getStartingRotation() / 0.8F)) + 1.0D);
                float startTrim = ring.getStartingStartTrim() + (ring.getStartingEndTrim() - ring.getStartingStartTrim()) * interpolatedTime;
                ring.setStartTrim(startTrim);
                float rotation = ring.getStartingRotation() + (targetRotation - ring.getStartingRotation()) * interpolatedTime;
                ring.setRotation(rotation);
                ring.setArrowScale(1.0F - interpolatedTime);
            }
        };
        finishRingAnimation.setInterpolator(EASE_INTERPOLATOR);
        finishRingAnimation.setDuration(666L);
        finishRingAnimation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                ring.goToNextColor();
                ring.storeOriginals();
                ring.setShowArrow(false);
                MaterialProgressDrawable.this.mParent.startAnimation(MaterialProgressDrawable.this.mAnimation);
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        Animation animation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float minProgressArc = (float)Math.toRadians((double)ring.getStrokeWidth() / (6.283185307179586D * ring.getCenterRadius()));
                float startingEndTrim = ring.getStartingEndTrim();
                float startingTrim = ring.getStartingStartTrim();
                float startingRotation = ring.getStartingRotation();
                float minArc = 0.8F - minProgressArc;
                float endTrim = startingEndTrim + minArc * MaterialProgressDrawable.START_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime);
                ring.setEndTrim(endTrim);
                float startTrim = startingTrim + 0.8F * MaterialProgressDrawable.END_CURVE_INTERPOLATOR.getInterpolation(interpolatedTime);
                ring.setStartTrim(startTrim);
                float rotation = startingRotation + 0.25F * interpolatedTime;
                ring.setRotation(rotation);
                float groupRotation = 144.0F * interpolatedTime + 720.0F * (MaterialProgressDrawable.this.mRotationCount / 5.0F);
                MaterialProgressDrawable.this.setRotation(groupRotation);
            }
        };
        animation.setRepeatCount(-1);
        animation.setRepeatMode(1);
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        animation.setDuration(1333L);
        animation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
                MaterialProgressDrawable.this.mRotationCount = 0.0F;
            }

            public void onAnimationEnd(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
                ring.storeOriginals();
                ring.goToNextColor();
                ring.setStartTrim(ring.getEndTrim());
                MaterialProgressDrawable.this.mRotationCount = (MaterialProgressDrawable.this.mRotationCount + 1.0F) % 5.0F;
            }
        });
        this.mFinishAnimation = finishRingAnimation;
        this.mAnimation = animation;
    }

    private static class StartCurveInterpolator extends AccelerateDecelerateInterpolator {
        private StartCurveInterpolator() {
        }

        public float getInterpolation(float input) {
            return super.getInterpolation(Math.min(1.0F, input * 2.0F));
        }
    }

    private static class EndCurveInterpolator extends AccelerateDecelerateInterpolator {
        private EndCurveInterpolator() {
        }

        public float getInterpolation(float input) {
            return super.getInterpolation(Math.max(0.0F, (input - 0.5F) * 2.0F));
        }
    }

    private static class Ring {
        private final RectF mTempBounds = new RectF();
        private final Paint mPaint = new Paint();
        private final Paint mArrowPaint = new Paint();
        private final Callback mCallback;
        private float mStartTrim = 0.0F;
        private float mEndTrim = 0.0F;
        private float mRotation = 0.0F;
        private float mStrokeWidth = 5.0F;
        private float mStrokeInset = 2.5F;
        private int[] mColors;
        private int mColorIndex;
        private float mStartingStartTrim;
        private float mStartingEndTrim;
        private float mStartingRotation;
        private boolean mShowArrow;
        private Path mArrow;
        private float mArrowScale;
        private double mRingCenterRadius;
        private int mArrowWidth;
        private int mArrowHeight;
        private int mAlpha;
        private final Paint mCirclePaint = new Paint();
        private int mBackgroundColor;

        public Ring(Callback callback) {
            this.mCallback = callback;
            this.mPaint.setStrokeCap(Cap.SQUARE);
            this.mPaint.setAntiAlias(true);
            this.mPaint.setStyle(Style.STROKE);
            this.mArrowPaint.setStyle(Style.FILL);
            this.mArrowPaint.setAntiAlias(true);
        }

        public void setBackgroundColor(int color) {
            this.mBackgroundColor = color;
        }

        public void setArrowDimensions(float width, float height) {
            this.mArrowWidth = (int)width;
            this.mArrowHeight = (int)height;
        }

        public void draw(Canvas c, Rect bounds) {
            RectF arcBounds = this.mTempBounds;
            arcBounds.set(bounds);
            arcBounds.inset(this.mStrokeInset, this.mStrokeInset);
            float startAngle = (this.mStartTrim + this.mRotation) * 360.0F;
            float endAngle = (this.mEndTrim + this.mRotation) * 360.0F;
            float sweepAngle = endAngle - startAngle;
            this.mPaint.setColor(this.mColors[this.mColorIndex]);
            c.drawArc(arcBounds, startAngle, sweepAngle, false, this.mPaint);
            this.drawTriangle(c, startAngle, sweepAngle, bounds);
            if(this.mAlpha < 255) {
                this.mCirclePaint.setColor(this.mBackgroundColor);
                this.mCirclePaint.setAlpha(255 - this.mAlpha);
                c.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), (float)(bounds.width() / 2), this.mCirclePaint);
            }

        }

        private void drawTriangle(Canvas c, float startAngle, float sweepAngle, Rect bounds) {
            if(this.mShowArrow) {
                if(this.mArrow == null) {
                    this.mArrow = new Path();
                    this.mArrow.setFillType(FillType.EVEN_ODD);
                } else {
                    this.mArrow.reset();
                }

                float inset = (float)((int)this.mStrokeInset / 2) * this.mArrowScale;
                float x = (float)(this.mRingCenterRadius * Math.cos(0.0D) + (double)bounds.exactCenterX());
                float y = (float)(this.mRingCenterRadius * Math.sin(0.0D) + (double)bounds.exactCenterY());
                this.mArrow.moveTo(0.0F, 0.0F);
                this.mArrow.lineTo((float)this.mArrowWidth * this.mArrowScale, 0.0F);
                this.mArrow.lineTo((float)this.mArrowWidth * this.mArrowScale / 2.0F, (float)this.mArrowHeight * this.mArrowScale);
                this.mArrow.offset(x - inset, y);
                this.mArrow.close();
                this.mArrowPaint.setColor(this.mColors[this.mColorIndex]);
                c.rotate(startAngle + sweepAngle - 5.0F, bounds.exactCenterX(), bounds.exactCenterY());
                c.drawPath(this.mArrow, this.mArrowPaint);
            }

        }

        public void setColors(@NonNull int[] colors) {
            this.mColors = colors;
            this.setColorIndex(0);
        }

        public void setColorIndex(int index) {
            this.mColorIndex = index;
        }

        public void goToNextColor() {
            this.mColorIndex = (this.mColorIndex + 1) % this.mColors.length;
        }

        public void setColorFilter(ColorFilter filter) {
            this.mPaint.setColorFilter(filter);
            this.invalidateSelf();
        }

        public void setAlpha(int alpha) {
            this.mAlpha = alpha;
        }

        public int getAlpha() {
            return this.mAlpha;
        }

        public void setStrokeWidth(float strokeWidth) {
            this.mStrokeWidth = strokeWidth;
            this.mPaint.setStrokeWidth(strokeWidth);
            this.invalidateSelf();
        }

        public float getStrokeWidth() {
            return this.mStrokeWidth;
        }

        public void setStartTrim(float startTrim) {
            this.mStartTrim = startTrim;
            this.invalidateSelf();
        }

        public float getStartTrim() {
            return this.mStartTrim;
        }

        public float getStartingStartTrim() {
            return this.mStartingStartTrim;
        }

        public float getStartingEndTrim() {
            return this.mStartingEndTrim;
        }

        public void setEndTrim(float endTrim) {
            this.mEndTrim = endTrim;
            this.invalidateSelf();
        }

        public float getEndTrim() {
            return this.mEndTrim;
        }

        public void setRotation(float rotation) {
            this.mRotation = rotation;
            this.invalidateSelf();
        }

        public float getRotation() {
            return this.mRotation;
        }

        public void setInsets(int width, int height) {
            float minEdge = (float)Math.min(width, height);
            float insets;
            if(this.mRingCenterRadius > 0.0D && minEdge >= 0.0F) {
                insets = (float)((double)(minEdge / 2.0F) - this.mRingCenterRadius);
            } else {
                insets = (float)Math.ceil((double)(this.mStrokeWidth / 2.0F));
            }

            this.mStrokeInset = insets;
        }

        public float getInsets() {
            return this.mStrokeInset;
        }

        public void setCenterRadius(double centerRadius) {
            this.mRingCenterRadius = centerRadius;
        }

        public double getCenterRadius() {
            return this.mRingCenterRadius;
        }

        public void setShowArrow(boolean show) {
            if(this.mShowArrow != show) {
                this.mShowArrow = show;
                this.invalidateSelf();
            }

        }

        public void setArrowScale(float scale) {
            if(scale != this.mArrowScale) {
                this.mArrowScale = scale;
                this.invalidateSelf();
            }

        }

        public float getStartingRotation() {
            return this.mStartingRotation;
        }

        public void storeOriginals() {
            this.mStartingStartTrim = this.mStartTrim;
            this.mStartingEndTrim = this.mEndTrim;
            this.mStartingRotation = this.mRotation;
        }

        public void resetOriginals() {
            this.mStartingStartTrim = 0.0F;
            this.mStartingEndTrim = 0.0F;
            this.mStartingRotation = 0.0F;
            this.setStartTrim(0.0F);
            this.setEndTrim(0.0F);
            this.setRotation(0.0F);
        }

        private void invalidateSelf() {
            this.mCallback.invalidateDrawable((Drawable)null);
        }
    }

    @Retention(RetentionPolicy.CLASS)
    @IntDef({0, 1})
    public @interface ProgressDrawableSize {
    }
}