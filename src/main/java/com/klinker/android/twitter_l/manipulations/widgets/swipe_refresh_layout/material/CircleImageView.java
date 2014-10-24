//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.klinker.android.twitter_l.manipulations.widgets.swipe_refresh_layout.material;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build.VERSION;
import android.support.v4.view.ViewCompat;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

class CircleImageView extends ImageView {
    private static final int KEY_SHADOW_COLOR = 503316480;
    private static final int FILL_SHADOW_COLOR = 1023410176;
    private static final float X_OFFSET = 0.0F;
    private static final float Y_OFFSET = 1.75F;
    private static final float SHADOW_RADIUS = 3.5F;
    private static final int SHADOW_ELEVATION = 0;
    private AnimationListener mListener;
    private int mShadowRadius;

    public CircleImageView(Context context, int color, float radius) {
        super(context);
        float density = this.getContext().getResources().getDisplayMetrics().density;
        int diameter = (int)(radius * density * 2.0F);
        int shadowYOffset = (int)(density * Y_OFFSET);
        int shadowXOffset = (int)(density * X_OFFSET);
        this.mShadowRadius = (int)(density * SHADOW_RADIUS);
        ShapeDrawable circle;
        if(this.elevationSupported()) {
            circle = new ShapeDrawable(new OvalShape());
            ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
        } else {
            CircleImageView.OvalShadow oval = new CircleImageView.OvalShadow(this.mShadowRadius, diameter);
            circle = new ShapeDrawable(oval);
            ViewCompat.setLayerType(this, 1, circle.getPaint());
            circle.getPaint().setShadowLayer((float)this.mShadowRadius, (float)shadowXOffset, (float)shadowYOffset, 503316480);
            int padding = this.mShadowRadius;
            this.setPadding(padding, padding, padding, padding);
        }

        circle.getPaint().setColor(color);
        this.setBackgroundDrawable(circle);
    }

    private boolean elevationSupported() {
        return VERSION.SDK_INT >= 21;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(!this.elevationSupported()) {
            this.setMeasuredDimension(this.getMeasuredWidth() + this.mShadowRadius * 2, this.getMeasuredHeight() + this.mShadowRadius * 2);
        }

    }

    public void setAnimationListener(AnimationListener listener) {
        this.mListener = listener;
    }

    public void onAnimationStart() {
        super.onAnimationStart();
        if(this.mListener != null) {
            this.mListener.onAnimationStart(this.getAnimation());
        }

    }

    public void onAnimationEnd() {
        super.onAnimationEnd();
        if(this.mListener != null) {
            this.mListener.onAnimationEnd(this.getAnimation());
        }

    }

    public void setBackgroundColor(int colorRes) {
        if(this.getBackground() instanceof ShapeDrawable) {
            Resources res = this.getResources();
            ((ShapeDrawable)this.getBackground()).getPaint().setColor(res.getColor(colorRes));
        }

    }

    private class OvalShadow extends OvalShape {
        private RadialGradient mRadialGradient;
        private int mShadowRadius;
        private Paint mShadowPaint = new Paint();
        private int mCircleDiameter;

        public OvalShadow(int shadowRadius, int circleDiameter) {
            this.mShadowRadius = shadowRadius;
            this.mCircleDiameter = circleDiameter;
            this.mRadialGradient = new RadialGradient((float)(this.mCircleDiameter / 2), (float)(this.mCircleDiameter / 2), (float)this.mShadowRadius, new int[]{1023410176, 0}, (float[])null, TileMode.CLAMP);
            this.mShadowPaint.setShader(this.mRadialGradient);
        }

        public void draw(Canvas canvas, Paint paint) {
            int viewWidth = CircleImageView.this.getWidth();
            int viewHeight = CircleImageView.this.getHeight();
            canvas.drawCircle((float)(viewWidth / 2), (float)(viewHeight / 2), (float)(this.mCircleDiameter / 2 + this.mShadowRadius), this.mShadowPaint);
            canvas.drawCircle((float)(viewWidth / 2), (float)(viewHeight / 2), (float)(this.mCircleDiameter / 2), paint);
        }
    }
}
