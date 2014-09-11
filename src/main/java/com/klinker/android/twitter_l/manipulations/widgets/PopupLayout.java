package com.klinker.android.twitter_l.manipulations.widgets;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jakewharton.disklrucache.Util;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

/**
 * Created by luke on 7/2/14.
 */
public abstract class PopupLayout extends LinearLayout {

    // some default constants for initializing the ActionButton
    public static final int DEFAULT_DISTANCE_FROM_TOP = 50;
    public static final int DEFAULT_DISTANCE_FROM_LEFT = 10;
    public static final int DEFAULT_SLIDE_ANIMATION_TIME = 150;
    public static final int DEFAULT_FADE_ANIMATION_TIME = 300;
    public static final int DEFAULT_WIDTH = 50;
    public static final int DEFAULT_HEIGHT = 50;
    public static final int DEFAULT_COLOR = 0xFFCC0000;
    public static final int DEFAULT_COLOR_SELECTED = 0xFFD94B4B;

    private Drawable background;
    private TextView title;
    private View titleDivider;

    private View dim;

    // set up default values
    private int distanceFromTop = DEFAULT_DISTANCE_FROM_TOP;
    private int distanceFromLeft = DEFAULT_DISTANCE_FROM_LEFT;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private int screenWidth;
    private int screenHeight;
    private boolean isShowing = false;
    private ViewGroup parent = null;

    protected boolean dontShow = false;
    protected boolean centerInScreen = false;

    public abstract View setMainLayout();

    // default constructor
    public PopupLayout(Context context) {
        super(context);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
        screenWidth = size.x;

        background = context.getDrawable(R.drawable.popup_background);
        setBackground(background);
        setClipToOutline(true);

        setElevation(3);
        setPadding(10,10,10,10);
        setOrientation(VERTICAL);

        title = new TextView(context);
        title.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int fiveDP = Utils.toDP(7, context);
        title.setPadding(fiveDP, fiveDP, fiveDP, fiveDP);
        title.setTextColor(AppSettings.getInstance(context).themeColors.primaryColor);
        title.setAllCaps(true);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setText(context.getResources().getString(R.string.retweets));

        titleDivider = new View(context);
        titleDivider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context)));
        titleDivider.setBackgroundColor(AppSettings.getInstance(context).themeColors.primaryColor);

        addView(title);
        addView(titleDivider);

        View main = setMainLayout();
        if (main != null) {
            try {
                addView(main);
            } catch (Exception e) {
                dontShow = true;
            }
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int background = a.getResourceId(0, 0);
        a.recycle();

        setBackgroundResource(background);

        dim = ((Activity) context).getLayoutInflater().inflate(R.layout.dim, null, false);
    }

    // default constructor
    public PopupLayout(Context context, boolean windowed) {
        super(context);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
        screenWidth = size.x;

        if (windowed) {
            if (screenHeight > screenWidth) {
                screenHeight = (int) (screenHeight * .68);
                screenWidth = (int) (screenWidth * .85);
            } else {
                screenHeight = (int) (screenHeight * .8);
                screenWidth = (int) (screenWidth * .6);
            }
        }

        background = context.getDrawable(R.drawable.popup_background);
        setBackground(background);
        setClipToOutline(true);

        setElevation(3);
        setPadding(10,10,10,10);
        setOrientation(VERTICAL);

        title = new TextView(context);
        title.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int fiveDP = Utils.toDP(7, context);
        title.setPadding(fiveDP, fiveDP, fiveDP, fiveDP);
        title.setTextColor(AppSettings.getInstance(context).themeColors.primaryColor);
        title.setAllCaps(true);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setText(context.getResources().getString(R.string.retweets));

        titleDivider = new View(context);
        titleDivider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context)));
        titleDivider.setBackgroundColor(AppSettings.getInstance(context).themeColors.primaryColor);

        addView(title);
        addView(titleDivider);

        View main = setMainLayout();
        if (main != null) {
            try {
                addView(main);
            } catch (Exception e) {
                dontShow = true;
            }
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int background = a.getResourceId(0, 0);
        a.recycle();

        setBackgroundResource(background);

        dim = ((Activity) context).getLayoutInflater().inflate(R.layout.dim, null, false);
    }

    /**
     * Sets how far away from the top of the screen the button should be displayed.
     * Distance should be the value in PX
     *
     * @param distance the distance from the top in px
     */
    public void setDistanceFromTop(int distance) {
        this.distanceFromTop = distance;
    }

    /**
     * Sets how far away from the left side of the screen the button should be displayed.
     * Distance should be the value in PX
     *
     * @param distance the distance from the left in px
     */
    public void setDistanceFromLeft(int distance) {
        this.distanceFromLeft = distance;
    }

    /**
     * Sets the width of the button. Distance should be the value in DP, it will be
     * converted to the appropriate pixel value
     *
     * @param width the width of the circle in px
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the button. Distance should be the value in DP, it will be
     * converted to the appropriate pixel value
     *
     * @param height the height of the circle in px
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Sets the width of the window according to the screen width
     *
     * @param percent as a decimal, (0.0 - 1.0)
     */
    public void setWidthByPercent(float percent) {
        this.width = (int) (screenWidth * percent);
    }

    /**
     * Sets the height of the window according to the screen height
     *
     * @param percent as a decimal (0.0 - 1.0)
     */
    public void setHeightByPercent(float percent) {
        this.height = (int) (screenHeight * percent);
    }

    /**
     * Sets the view that the buttons should be attached to
     *
     * @param parent the parent view to attach buttons to
     */
    public void setParent(ViewGroup parent) {
        this.parent = parent;
    }

    /**
     * Sets the window to just inside the full screen size
     */
    public void setFullScreen() {
        // sets it to 95% of the screen width
        setWidthByPercent(.95f);

        // uses that width to set the left offset
        float offsetLeft = (screenWidth - width) / 2;
        setDistanceFromLeft((int) offsetLeft);

        int statusBarSize = Utils.getStatusBarHeight(getContext());
        int actionBarSize = Utils.getActionBarHeight(getContext());
        int navBarSize = Utils.getNavBarHeight(getContext());

        // set the height to 95% of the screen height
        setHeightByPercent(1.0f);
        height -= statusBarSize + actionBarSize + navBarSize;

        // makes sure it is centered and below the
        float usableHeight = screenHeight - statusBarSize - actionBarSize - navBarSize;
        setDistanceFromTop((int) (statusBarSize + actionBarSize + 10 + ((usableHeight - height) / 2)));
    }

    /**
     * Places the popup layout on top of the given view
     * Will have the centers of the layouts in the same place
     *
     * @param under view that the popup should come above
     */
    public void setOnTopOfView(View under) {
        int[] location = new int[2];
        under.getLocationOnScreen(location);

        int fromLeft = location[0];
        int fromTop = location[1];
        int viewWidth = under.getWidth();
        int viewHeight = under.getHeight();

        int startX = fromLeft + (int) (viewWidth / 2.0);
        int startY = fromTop + (int) (viewHeight / 2.0);

        startX = startX - (width / 2);
        startY = startY - (height / 2);

        // check these against the layout bounds now
        if (startX < 0) { // if it is off the left side
            startX = 10;
        } else if (startX > screenWidth - width) { // if it is off the right side
            startX = screenWidth - width - 10;
        }

        int statusActionBar = Utils.getStatusBarHeight(getContext()) + Utils.getActionBarHeight(getContext());
        if (startY < statusActionBar) { // if it is above the status bar and action bar
            startY = statusActionBar + 10;
        } else if (Utils.hasNavBar(getContext()) &&
                startY + height > screenHeight - Utils.getNavBarHeight(getContext())) {
            // if there is a nav bar and the popup is underneath it
            startY = screenHeight - height - Utils.getNavBarHeight(getContext()) - 10;
        } else if (!Utils.hasNavBar(getContext()) && startY + height > screenHeight) {
            startY = screenHeight - height - 10;
        }

        setDistanceFromLeft(startX);
        setDistanceFromTop(startY);
    }

    /**
     * Set whether we should show the title or not
     *
     * @param show boolean to show it or not
     */
    public void showTitle(boolean show) {
        if (show) {
            if (title.getVisibility() != View.VISIBLE) {
                title.setVisibility(View.VISIBLE);
                titleDivider.setVisibility(View.VISIBLE);
            }
        } else {
            if (title.getVisibility() != View.GONE) {
                title.setVisibility(View.GONE);
                titleDivider.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Sets the text of the title
     * @param text
     */
    public void setTitle(String text) {
        title.setText(text);
    }

    public void setCenterInScreen() {
        setDistanceFromLeft(screenWidth/2 - width/2);
        setDistanceFromTop(screenHeight/2 - height/2);
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
        if (dontShow) {
            return;
        }

        final Activity activity = (Activity) getContext();

        // set the correct width and height for ActionButton
        ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        this.setLayoutParams(params);

        if (parent == null) {
            // get the current content FrameLayout and add ActionButton to the top
            parent = (FrameLayout) activity.findViewById(android.R.id.content);
        }

        try {
            parent.addView(dim);
        } catch (Exception e) {

        }

        try {
            parent.addView(this);
        } catch (Exception e) {

        }

        // get the size of the screen so we know where to animate from and to
        float frameWidth = parent.getWidth();
        float frameHeight = parent.getHeight();

        // perform the animation with an object animator, default to sliding up from bottom
        setTranslationX(distanceFromLeft);
        ObjectAnimator animator = null;

        setTranslationY(distanceFromTop);
        animator = ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f, 1.0f);
        animator.setDuration(DEFAULT_FADE_ANIMATION_TIME);

        if (animator != null) {
            animator.start();
        }

        ObjectAnimator dimAnimator = null;

        dimAnimator = ObjectAnimator.ofFloat(dim, View.ALPHA, 0.0f, .6f);
        dimAnimator.setDuration(DEFAULT_FADE_ANIMATION_TIME);

        if (dimAnimator != null) {
            dimAnimator.start();
        }

        isShowing = true;
    }

    /**
     * Animates the ActionButton off of the screen. Animation will go from its current position and
     * down until it is no longer being shown to the user.
     */
    public void hide() {

        // get size of screen
        float frameWidth = parent.getWidth();
        float frameHeight = parent.getHeight();

        // perform animation
        setTranslationX(distanceFromLeft);
        ObjectAnimator animator = null;
        int animTime = 0;

        animator = ObjectAnimator.ofFloat(this, View.ALPHA, 1.0f, 0.0f);
        animTime = DEFAULT_FADE_ANIMATION_TIME;

        if (animator != null) {
            animator.setDuration(animTime);
            animator.start();
        }

        ObjectAnimator dimAnimator = ObjectAnimator.ofFloat(dim, View.ALPHA, .6f, 0.0f);
        animTime = DEFAULT_FADE_ANIMATION_TIME;

        if (dimAnimator != null) {
            dimAnimator.setDuration(animTime);
            dimAnimator.start();
        }

        // After animation has finished, remove the ActionButton from the content frame
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                parent.removeView(PopupLayout.this);
                parent.removeView(dim);
            }
        }, animTime);

        isShowing = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        Log.i("VerticalScrollview", "onTouchEvent. action: " + ev.getAction());
        return true;
    }
}
