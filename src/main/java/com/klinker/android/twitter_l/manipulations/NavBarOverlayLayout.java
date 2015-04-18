package com.klinker.android.twitter_l.manipulations;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.disklrucache.Util;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class NavBarOverlayLayout extends LinearLayout {

    // some default constants for initializing the ActionButton
    private Drawable background;
    protected LinearLayout content;

    private View dim;

    // set up default values
    private int distanceFromTop;
    private int distanceFromLeft;
    protected int width;
    protected int height;
    private int screenWidth;
    private int screenHeight;
    private boolean isShowing = false;
    private ViewGroup parent = null;

    public NavBarOverlayLayout(Context context) {
        super(context);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
        screenWidth = size.x;

        background = new ColorDrawable(Color.BLACK);
        setBackground(background);

        setOrientation(VERTICAL);

        distanceFromTop = screenHeight;
        distanceFromLeft = 0;
        width = screenWidth;
        height = Utils.hasNavBar(context) ? Utils.getNavBarHeight(context) : 0;
    }

    /**
     * Tells whether or not the button is currently showing on the screen.
     *
     * @return true if ActionButton is showing, false otherwise
     */
    public boolean isShowing() {
        return isShowing;
    }

    /**
     * Animates the ActionButton onto the screen so that the user may interact.
     * Animation occurs from the bottom of the screen, moving up until it reaches the
     * appropriate distance from the bottom.
     */
    public void show() {
        final Activity activity = (Activity) getContext();

        // set the correct width and height for ActionButton
        ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        this.setLayoutParams(params);

        if (parent == null) {
            // get the current content FrameLayout and add ActionButton to the top
            parent = (FrameLayout) activity.findViewById(android.R.id.content);
        }


        try {
            parent.addView(this);
        } catch (Exception e) {

        }

        setTranslationY(distanceFromTop);
        setTranslationX(distanceFromLeft);

        /*if (animStartTop == -1) {
            // we haven't specified a view to start from
            setTranslationX(distanceFromLeft);
            setTranslationY(distanceFromTop);

            ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f, 1.0f);
            animator.setDuration(DEFAULT_FADE_ANIMATION_TIME);
            animator.start();
        } else {
            title.setVisibility(View.GONE);
            titleDivider.setVisibility(View.GONE);
            content.setVisibility(View.GONE);

            setTranslationX(animStartLeft);
            setTranslationY(animStartTop);

            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.width = 0;
            layoutParams.height = Utils.toDP(5, getContext());
            setLayoutParams(layoutParams);

            ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f, 1.0f);
            ObjectAnimator xTranslation = ObjectAnimator.ofFloat(this, View.TRANSLATION_X, animStartLeft, distanceFromLeft);
            final ObjectAnimator yTranslation = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, animStartTop, distanceFromTop);
            ValueAnimator widthExpander = ValueAnimator.ofInt(0, width);
            final ValueAnimator heightExpander = ValueAnimator.ofInt(0, height);

            widthExpander.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.width = val;
                    setLayoutParams(layoutParams);
                }
            });
            widthExpander.setDuration(LONG_ANIMATION_TIME);
            widthExpander.setInterpolator(INTERPOLATOR);

            heightExpander.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    int val = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = getLayoutParams();
                    layoutParams.height = val;
                    setLayoutParams(layoutParams);
                }
            });
            heightExpander.setDuration(SHORT_ANIMATION_TIME);
            heightExpander.setInterpolator(INTERPOLATOR);

            xTranslation.setDuration(LONG_ANIMATION_TIME);
            xTranslation.setInterpolator(INTERPOLATOR);

            yTranslation.setDuration(SHORT_ANIMATION_TIME);
            yTranslation.setInterpolator(INTERPOLATOR);

            alpha.setDuration(LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME);
            alpha.setInterpolator(INTERPOLATOR);

            alpha.start();
            xTranslation.start();
            widthExpander.start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    yTranslation.start();
                    heightExpander.start();
                }
            }, LONG_ANIMATION_TIME);

            if (showTitle) {
                // show the actual content of the popup
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // show the content
                        title.setVisibility(View.VISIBLE);

                        ObjectAnimator animator = ObjectAnimator.ofFloat(title, View.ALPHA, 0.0f, 1.0f);
                        animator.setDuration(LONG_ANIMATION_TIME);
                        animator.start();
                    }
                }, LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // show the content
                        titleDivider.setVisibility(View.VISIBLE);

                        ObjectAnimator animator = ObjectAnimator.ofFloat(titleDivider, View.ALPHA, 0.0f, 1.0f);
                        animator.setDuration(LONG_ANIMATION_TIME);
                        animator.start();
                    }
                }, LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME + 30);
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // show the content
                    content.setVisibility(View.VISIBLE);
                    ObjectAnimator animator = ObjectAnimator.ofFloat(content, View.ALPHA, 0.0f, 1.0f);
                    animator.setDuration(LONG_ANIMATION_TIME);
                    animator.start();
                    if (PopupLayout.this instanceof WebPopupLayout) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                content.setVisibility(View.GONE);
                                content.setVisibility(View.VISIBLE);
                            }
                        }, 200);
                    }
                }
            }, LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME + 60);
        }

        ObjectAnimator dimAnimator = ObjectAnimator.ofFloat(dim, View.ALPHA, 0.0f, .6f);
        dimAnimator.setDuration(LONG_ANIMATION_TIME + SHORT_ANIMATION_TIME);
        dimAnimator.start();
*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
