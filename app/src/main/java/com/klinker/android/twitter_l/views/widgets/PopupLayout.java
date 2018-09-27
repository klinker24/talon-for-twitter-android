package com.klinker.android.twitter_l.views.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import androidx.cardview.widget.CardView;
import android.util.TypedValue;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.views.popups.WebPopupLayout;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public abstract class PopupLayout extends CardView {

    // some default constants for initializing the ActionButton
    public static final int DEFAULT_DISTANCE_FROM_TOP = 50;
    public static final int DEFAULT_DISTANCE_FROM_LEFT = 10;
    public static final int DEFAULT_SLIDE_ANIMATION_TIME = 150;
    public static final int DEFAULT_FADE_ANIMATION_TIME = 200;
    public static final int DEFAULT_WIDTH = 50;
    public static final int DEFAULT_HEIGHT = 50;
    public static final int DEFAULT_COLOR = 0xFFCC0000;
    public static final int DEFAULT_COLOR_SELECTED = 0xFFD94B4B;

    public static final int LONG_ANIMATION_TIME = 125;
    public static final int SHORT_ANIMATION_TIME = 75;

    public static final int REAL_ANIMATION_TIME = 200;

    private static float FINAL_TITLE_ALPHA = .73f;
    private static float FINAL_DIM_ALPHA = .3f;
    
    public long fadeAnimationTime = DEFAULT_FADE_ANIMATION_TIME;
    public long longAnimationTime = LONG_ANIMATION_TIME;
    public long shortAnimationTime = SHORT_ANIMATION_TIME;
    public long realAnimationTime = REAL_ANIMATION_TIME;
    
    private static Interpolator INTERPOLATOR;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            INTERPOLATOR = new PathInterpolator(.1f,.1f,.2f,1f);
        } else {
            INTERPOLATOR = new DecelerateInterpolator();
        }
    }

    private Drawable background;
    private TextView title;
    private View titleDivider;
    protected LinearLayout content;

    private View dim;

    // set up default values
    private int distanceFromTop = DEFAULT_DISTANCE_FROM_TOP;
    private int distanceFromLeft = DEFAULT_DISTANCE_FROM_LEFT;
    protected int width = DEFAULT_WIDTH;
    protected int height = DEFAULT_HEIGHT;
    private int screenWidth;
    private int screenHeight;
    private boolean isShowing = false;
    private ViewGroup parent = null;

    protected boolean dontShow = false;
    protected boolean centerInScreen = false;

    protected boolean windowed = false;

    public abstract View setMainLayout();

    // default constructor
    public PopupLayout(Context context) {
        super(context);

        if (AppSettings.getInstance(context).darkTheme) {
            FINAL_TITLE_ALPHA = 1.0f;
            FINAL_DIM_ALPHA = .5f;

            if (AppSettings.getInstance(context).blackTheme) {
                FINAL_DIM_ALPHA = .6f;
            }
        } else {
            FINAL_DIM_ALPHA = .3f;
            FINAL_TITLE_ALPHA = .75f;
        }

        setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
        screenWidth = size.x;

        int padding = Utils.toDP(16, context);

        LinearLayout root = new LinearLayout(context);
        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootParams.weight = 1;
        root.setLayoutParams(rootParams);

        root.setOrientation(LinearLayout.VERTICAL);

        title = new TextView(context);
        title.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        title.setPadding(padding, padding, padding, padding);
        if (AppSettings.getInstance(context).darkTheme) {
            title.setTextColor(context.getResources().getColor(R.color.dark_text));
        } else {
            title.setTextColor(context.getResources().getColor(R.color.light_text));
        }
        title.setTypeface(null, Typeface.BOLD);
        title.setAlpha(FINAL_TITLE_ALPHA);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setText(context.getResources().getString(R.string.retweets));

        /*titleDivider = new View(context);
        titleDivider.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.toDP(1, context)));
        titleDivider.setBackgroundColor(AppSettings.getInstance(context).themeColors.primaryColor);*/

        content = new LinearLayout(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        params.weight = 1;
        content.setLayoutParams(params);

        addView(root);

        root.addView(title);
        //root.addView(titleDivider);
        root.addView(content);

        View main = setMainLayout();
        if (main != null) {
            try {
                content.addView(main);
            } catch (Exception e) {
                dontShow = true;
            }

            width = main.getWidth();
            height = main.getHeight();
        }

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{R.attr.windowBackground});
        int background = a.getResourceId(0, 0);
        a.recycle();


        AppSettings settings = AppSettings.getInstance(context);
        if (settings.darkTheme) {
            if (settings.blackTheme) {
                setCardBackgroundColor(Color.BLACK);
            } else {
                setCardBackgroundColor(getResources().getColor(R.color.dark_background));
            }
        } else {
            setCardBackgroundColor(Color.WHITE);
        }

        setCardElevation(Utils.toDP(3, context));
        setRadius(Utils.toDP(3, context));

        dim = ((Activity) context).getLayoutInflater().inflate(R.layout.dim, null, false);

        dim.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                hide();
                return true;
            }
        });

    }

    // 0 - 1
    public void setAnimationScale(float scale) {
        this.fadeAnimationTime = (long) (fadeAnimationTime * scale);
        this.longAnimationTime = (long) (longAnimationTime * scale);
        this.shortAnimationTime = (long) (shortAnimationTime * scale);
        this.realAnimationTime = (long) (realAnimationTime * scale);
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
        int navBarSize = Utils.getNavBarHeight(getContext());

        // set the height to 95% of the screen height
        setHeightByPercent(1.0f);
        height -= statusBarSize + navBarSize;

        // makes sure it is centered and below the
        float usableHeight = screenHeight - statusBarSize - navBarSize;
        setDistanceFromTop((int) (statusBarSize + Utils.toDP(24, getContext()) + ((usableHeight - height) / 2)));
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

        int startX;
        int startY;

        if (getContext().getResources().getBoolean(R.bool.isRTL)) {
            startX = fromLeft - (int) (viewWidth / 2.0);
            startY = fromTop - (int) (viewHeight / 2.0);
        } else {
            startX = fromLeft + (int) (viewWidth / 2.0);
            startY = fromTop + (int) (viewHeight / 2.0);
        }

        startX = startX - (width / 2);
        startY = startY - (height / 2);

        // check these against the layout bounds now
        if (startX < 0) { // if it is off the left side
            startX = 10;
        } else if (startX > screenWidth - width) { // if it is off the right side
            startX = screenWidth - width - 10;
        }

        int statusActionBar = Utils.getStatusBarHeight(getContext());
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

    private boolean showTitle = true;
    /**
     * Set whether we should show the question or not
     *
     * @param show boolean to show it or not
     */
    public void showTitle(boolean show) {
        if (show) {
            if (title.getVisibility() != View.VISIBLE) {
                title.setVisibility(View.VISIBLE);
            }
        } else {
            if (title.getVisibility() != View.GONE) {
                title.setVisibility(View.GONE);
            }
        }
        showTitle = show;
    }

    /**
     * Sets the text of the question
     * @param text
     */
    public void setTitle(String text) {
        title.setText(text);
    }

    public void setCenterInScreen() {
        setDistanceFromLeft(screenWidth/2 - width/2);
        setDistanceFromTop(screenHeight/2 - height/2);
    }

    int animStartLeft = -1;
    int animStartTop = -1;
    public void setExpansionPointForAnim(View v) {

        // center of the view
        int[] location = new int[2];
        v.getLocationOnScreen(location);

        int fromLeft = location[0];
        int fromTop = location[1];
        int viewWidth = v.getWidth();
        int viewHeight = v.getHeight();

        animStartLeft = fromLeft + (int) (viewWidth / 2.0);
        animStartTop = fromTop + (int) (viewHeight / 2.0);
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

        if (isShowing) {
            return;
        }
        isShowing = true;

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

        if (animStartTop == -1) {
            // we haven't specified a view to start from
            setTranslationX(distanceFromLeft);
            setTranslationY(distanceFromTop);

            ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ALPHA, 0.0f, 1.0f);
            animator.setDuration(fadeAnimationTime);
            animator.start();
        } else {
            title.setVisibility(View.GONE);
            //titleDivider.setVisibility(View.GONE);
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
            widthExpander.setDuration(realAnimationTime); // was long
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
            heightExpander.setDuration(realAnimationTime); // was short
            heightExpander.setInterpolator(INTERPOLATOR);

            xTranslation.setDuration(realAnimationTime); // was long
            xTranslation.setInterpolator(INTERPOLATOR);

            yTranslation.setDuration(realAnimationTime); // was short
            yTranslation.setInterpolator(INTERPOLATOR);

            alpha.setDuration(realAnimationTime); // was long + short
            alpha.setInterpolator(INTERPOLATOR);

            alpha.start();
            xTranslation.start();
            widthExpander.start();
            yTranslation.start();
            heightExpander.start();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    //yTranslation.start();
                    //heightExpander.start();
                }
            }, longAnimationTime);

            if (showTitle) {
                // show the actual content of the popup
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // show the content
                        title.setVisibility(View.VISIBLE);

                        ObjectAnimator animator = ObjectAnimator.ofFloat(title, View.ALPHA, 0.0f, FINAL_TITLE_ALPHA);
                        animator.setDuration(fadeAnimationTime);
                        animator.start();
                    }
                }, realAnimationTime);

            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // show the content
                    content.setVisibility(View.VISIBLE);
                    ObjectAnimator animator = ObjectAnimator.ofFloat(content, View.ALPHA, 0.0f, 1.0f);
                    animator.setDuration(fadeAnimationTime);
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
            }, realAnimationTime + 200); // was long + short + 60
        }

        ObjectAnimator dimAnimator = ObjectAnimator.ofFloat(dim, View.ALPHA, 0.0f, FINAL_DIM_ALPHA);
        dimAnimator.setDuration(fadeAnimationTime); // was long + short
        dimAnimator.start();

    }

    /**
     * Animates the ActionButton off of the screen. Animation will go from its current position and
     * down until it is no longer being shown to the user.
     */
    public void hide() {


        if (!isShowing) {
            return;
        }

        isShowing = false;

        if (true /*animStartTop == -1*/) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ALPHA, 1.0f, 0.0f);
            animator.setDuration(fadeAnimationTime);
            animator.start();
        } else {
            if (showTitle) {
                title.setVisibility(View.INVISIBLE);

                ObjectAnimator animator = ObjectAnimator.ofFloat(title, View.ALPHA, FINAL_TITLE_ALPHA, 0.0f);
                animator.setDuration(fadeAnimationTime); // was long
                animator.start();

                //titleDivider.setVisibility(View.INVISIBLE);

                /*animator = ObjectAnimator.ofFloat(titleDivider, View.ALPHA, 1.0f, 0.0f);
                animator.setDuration(fadeAnimationTime); // was long
                animator.start();*/
            }

            content.setVisibility(View.INVISIBLE);

            ObjectAnimator animator = ObjectAnimator.ofFloat(content, View.ALPHA, 1.0f, 0.0f);
            animator.setDuration(fadeAnimationTime); // was long
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    content.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationCancel(Animator animation) { }

                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            animator.start();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    ObjectAnimator alpha = ObjectAnimator.ofFloat(PopupLayout.this, View.ALPHA, 1.0f, 0f);
                    final ObjectAnimator xTranslation = ObjectAnimator.ofFloat(PopupLayout.this, View.TRANSLATION_X, distanceFromLeft, animStartLeft);
                    ObjectAnimator yTranslation = ObjectAnimator.ofFloat(PopupLayout.this, View.TRANSLATION_Y, distanceFromTop, animStartTop);
                    final ValueAnimator widthExpander = ValueAnimator.ofInt(width, 0);
                    ValueAnimator heightExpander = ValueAnimator.ofInt(height, Utils.toDP(5, getContext()));

                    widthExpander.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                            int val = (Integer) valueAnimator.getAnimatedValue();
                            ViewGroup.LayoutParams layoutParams = getLayoutParams();
                            layoutParams.width = val;
                            setLayoutParams(layoutParams);
                        }
                    });
                    widthExpander.setDuration(realAnimationTime); // was long
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
                    heightExpander.setDuration(realAnimationTime); // was short
                    heightExpander.setInterpolator(INTERPOLATOR);

                    xTranslation.setDuration(realAnimationTime); // was long
                    xTranslation.setInterpolator(INTERPOLATOR);

                    yTranslation.setDuration(realAnimationTime); // was short
                    yTranslation.setInterpolator(INTERPOLATOR);

                    alpha.setDuration(realAnimationTime); // was long + short
                    alpha.setInterpolator(INTERPOLATOR);

                    alpha.start();
                    yTranslation.start();
                    heightExpander.start();
                    xTranslation.start();
                    widthExpander.start();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //xTranslation.start();
                            //widthExpander.start();
                        }
                    }, shortAnimationTime);
                }
            }, fadeAnimationTime + 100);
        }

        ObjectAnimator dimAnimator = ObjectAnimator.ofFloat(dim, View.ALPHA, FINAL_DIM_ALPHA, 0.0f);

        dimAnimator.setDuration(fadeAnimationTime); // was long + short
        //dimAnimator.setStartDelay(fadeAnimationTime);
        dimAnimator.start();

        // After animation has finished, remove the ActionButton from the content frame
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                parent.removeView(PopupLayout.this);
                parent.removeView(dim);
            }
        }, realAnimationTime + 100);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        return true;
    }
}
