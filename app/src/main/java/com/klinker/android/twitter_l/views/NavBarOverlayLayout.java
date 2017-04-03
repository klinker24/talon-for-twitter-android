package com.klinker.android.twitter_l.views;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.utils.Utils;

public class NavBarOverlayLayout extends LinearLayout {

    // some default constants for initializing the ActionButton
    private Drawable background;
    protected LinearLayout content;

    private View dim;

    // set up default values
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

        width = screenWidth;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            boolean isTablet = getResources().getBoolean(R.bool.isTablet);

            if (isLandscape && !isTablet) {
                height = 0;
            } else {
                height = Utils.hasNavBar(context) ? Utils.getNavBarHeight(context) : 0;
            }
        } else {
            height = Utils.hasNavBar(context) ? Utils.getNavBarHeight(context) : 0;
        }
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

        setAlpha(1f);

        // set the correct width and height for ActionButton
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.BOTTOM;
        this.setLayoutParams(params);

        if (parent == null) {
            // get the current content FrameLayout and add ActionButton to the top
            parent = (FrameLayout) activity.findViewById(android.R.id.content);
        }


        try {
            parent.addView(this);
        } catch (Exception e) {

        }
    }

    public void hide() {
        final Activity activity = (Activity) getContext();

        setAlpha(0f);

        if (parent == null) {
            // get the current content FrameLayout and add ActionButton to the top
            parent = (FrameLayout) activity.findViewById(android.R.id.content);
        }


        try {
            parent.addView(this);
        } catch (Exception e) {

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
