package com.klinker.android.twitter_l.views.widgets.swipe_refresh_layout.material;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Handler;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import com.klinker.android.twitter_l.utils.SystemBarVisibility;

public class MaterialSwipeRefreshLayout extends ViewGroup {
    public static final int LARGE = 0;
    public static final int DEFAULT = 1;
    private static final String LOG_TAG = MaterialSwipeRefreshLayout.class.getSimpleName();
    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = 76;
    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_DIAMETER_LARGE = 56;
    private static final float DECELERATE_INTERPOLATION_FACTOR = 2.0F;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = 0.5F;
    private static final float MAX_PROGRESS_ANGLE = 0.8F;
    private static final int SCALE_DOWN_DURATION = 150;
    private static final int ALPHA_ANIMATION_DURATION = 300;
    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;
    private static final int ANIMATE_TO_START_DURATION = 200;
    private static final int CIRCLE_BG_LIGHT = -328966;
    private static final int DEFAULT_CIRCLE_TARGET = 64;
    private View mTarget;
    private MaterialSwipeRefreshLayout.OnRefreshListener mListener;
    private boolean mRefreshing;
    private int mTouchSlop;
    private float mTotalDragDistance;
    private int mMediumAnimationDuration;
    private int mCurrentTargetOffsetTop;
    private boolean mOriginalOffsetCalculated;
    private float mInitialMotionY;
    private boolean mIsBeingDragged;
    private int mActivePointerId;
    private boolean mScale;
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{16842766};
    private CircleImageView mCircleView;
    private int mCircleViewIndex;
    protected int mFrom;
    private float mStartingScale;
    protected int mOriginalOffsetTop;
    private MaterialProgressDrawable mProgress;
    private Animation mScaleAnimation;
    private Animation mScaleDownAnimation;
    private Animation mAlphaStartAnimation;
    private Animation mAlphaMaxAnimation;
    private Animation mScaleDownToStartAnimation;
    private float mSpinnerFinalOffset;
    private boolean mNotify;
    private int mCircleWidth;
    private int mCircleHeight;
    private boolean mUsingCustomStart;
    private AnimationListener mRefreshListener;
    private final Animation mAnimateToCorrectPosition;
    private final Animation mAnimateToStartPosition;

    private void setColorViewAlpha(int targetAlpha) {
        this.mCircleView.getBackground().setAlpha(targetAlpha);
        this.mProgress.setAlpha(targetAlpha);
    }

    public void setProgressViewOffset(boolean scale, int start, int end) {
        this.mScale = scale;
        this.mCircleView.setVisibility(8);
        this.mOriginalOffsetTop = this.mCurrentTargetOffsetTop = start;
        this.mSpinnerFinalOffset = (float)end;
        this.mUsingCustomStart = true;
        this.mCircleView.invalidate();
    }

    SystemBarVisibility watcher;

    public void setBarVisibilityWatcher(SystemBarVisibility watcher) {
        this.watcher = watcher;
    }

    public void setProgressViewEndTarget(boolean scale, int end) {
        this.mSpinnerFinalOffset = (float)end;
        this.mScale = scale;
        this.mCircleView.invalidate();
    }

    public void setSize(int size) {
        if(size == 0 || size == 1) {
            DisplayMetrics metrics = this.getResources().getDisplayMetrics();
            if(size == 0) {
                this.mCircleHeight = this.mCircleWidth = (int)(56.0F * metrics.density);
            } else {
                this.mCircleHeight = this.mCircleWidth = (int)(40.0F * metrics.density);
            }

            this.mCircleView.setImageDrawable((Drawable)null);
            this.mProgress.updateSizes(size);
            this.mCircleView.setImageDrawable(this.mProgress);
        }
    }

    public MaterialSwipeRefreshLayout(Context context) {
        this(context, (AttributeSet)null);
    }

    public MaterialSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRefreshing = false;
        this.mTotalDragDistance = -1.0F;
        this.mOriginalOffsetCalculated = false;
        this.mActivePointerId = -1;
        this.mCircleViewIndex = -1;
        this.mRefreshListener = new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                if(MaterialSwipeRefreshLayout.this.mRefreshing) {
                    MaterialSwipeRefreshLayout.this.mProgress.setAlpha(255);
                    MaterialSwipeRefreshLayout.this.mProgress.start();
                    if(MaterialSwipeRefreshLayout.this.mNotify && MaterialSwipeRefreshLayout.this.mListener != null) {
                        MaterialSwipeRefreshLayout.this.mListener.onRefresh();
                    }
                } else {
                    MaterialSwipeRefreshLayout.this.mProgress.stop();
                    MaterialSwipeRefreshLayout.this.mCircleView.setVisibility(8);
                    MaterialSwipeRefreshLayout.this.setColorViewAlpha(255);
                    if(MaterialSwipeRefreshLayout.this.mScale) {
                        MaterialSwipeRefreshLayout.this.setAnimationProgress(0.0F);
                    } else {
                        MaterialSwipeRefreshLayout.this.setTargetOffsetTopAndBottom(MaterialSwipeRefreshLayout.this.mOriginalOffsetTop - MaterialSwipeRefreshLayout.this.mCurrentTargetOffsetTop, true);
                    }
                }

                MaterialSwipeRefreshLayout.this.mCurrentTargetOffsetTop = MaterialSwipeRefreshLayout.this.mCircleView.getTop();
            }
        };
        this.mAnimateToCorrectPosition = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                boolean targetTop = false;
                boolean endTarget = false;
                int endTarget1;
                if(!MaterialSwipeRefreshLayout.this.mUsingCustomStart) {
                    endTarget1 = (int)(MaterialSwipeRefreshLayout.this.mSpinnerFinalOffset - (float)Math.abs(MaterialSwipeRefreshLayout.this.mOriginalOffsetTop));
                } else {
                    endTarget1 = (int) MaterialSwipeRefreshLayout.this.mSpinnerFinalOffset;
                }

                int targetTop1 = MaterialSwipeRefreshLayout.this.mFrom + (int)((float)(endTarget1 - MaterialSwipeRefreshLayout.this.mFrom) * interpolatedTime);
                int offset = targetTop1 - MaterialSwipeRefreshLayout.this.mCircleView.getTop();
                MaterialSwipeRefreshLayout.this.setTargetOffsetTopAndBottom(offset, false);
            }
        };
        this.mAnimateToStartPosition = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                MaterialSwipeRefreshLayout.this.moveToStart(interpolatedTime);
            }
        };
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        int i = 17694721;
        this.mMediumAnimationDuration = this.getResources().getInteger(i);
        this.setWillNotDraw(false);
        this.mDecelerateInterpolator = new DecelerateInterpolator(2.0F);
        TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        this.setEnabled(a.getBoolean(0, true));
        a.recycle();
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        this.mCircleWidth = (int)(40.0F * metrics.density);
        this.mCircleHeight = (int)(40.0F * metrics.density);
        this.createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        this.mSpinnerFinalOffset = 64.0F * metrics.density;
        this.mTotalDragDistance = this.mSpinnerFinalOffset;
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        return this.mCircleViewIndex < 0?i:(i == childCount - 1?this.mCircleViewIndex:(i >= this.mCircleViewIndex?i + 1:i));
    }

    private void createProgressView() {
        this.mCircleView = new CircleImageView(this.getContext(), -328966, 20.0F);
        this.mProgress = new MaterialProgressDrawable(this.getContext(), this);
        this.mProgress.setBackgroundColor(-328966);
        this.mCircleView.setImageDrawable(this.mProgress);
        this.mCircleView.setVisibility(8);
        this.addView(this.mCircleView);
    }

    public void setOnRefreshListener(MaterialSwipeRefreshLayout.OnRefreshListener listener) {
        this.mListener = listener;
    }

    private boolean isAlphaUsedForScale() {
        return VERSION.SDK_INT < 11;
    }

    public void setRefreshing(boolean refreshing) {
        if(refreshing && this.mRefreshing != refreshing) {
            this.mRefreshing = refreshing;
            boolean endTarget = false;
            int endTarget1;
            if(!this.mUsingCustomStart) {
                endTarget1 = (int)(this.mSpinnerFinalOffset + (float)this.mOriginalOffsetTop);
            } else {
                endTarget1 = (int)this.mSpinnerFinalOffset;
            }

            this.setTargetOffsetTopAndBottom(endTarget1 - this.mCurrentTargetOffsetTop, true);
            this.mNotify = false;
            this.startScaleUpAnimation(this.mRefreshListener);
        } else {
            this.setRefreshing(refreshing, false);
        }

    }

    private void startScaleUpAnimation(AnimationListener listener) {
        this.mCircleView.setVisibility(0);
        if(VERSION.SDK_INT >= 11) {
            this.mProgress.setAlpha(255);
        }

        this.mScaleAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                MaterialSwipeRefreshLayout.this.setAnimationProgress(interpolatedTime);
            }
        };
        this.mScaleAnimation.setDuration((long)this.mMediumAnimationDuration);
        if(listener != null) {
            this.mCircleView.setAnimationListener(listener);
        }

        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mScaleAnimation);
    }

    private void setAnimationProgress(float progress) {
        if(this.isAlphaUsedForScale()) {
            this.setColorViewAlpha((int)(progress * 255.0F));
        } else {
            ViewCompat.setScaleX(this.mCircleView, progress);
            ViewCompat.setScaleY(this.mCircleView, progress);
        }

    }

    private void setRefreshing(boolean refreshing, boolean notify) {
        if(this.mRefreshing != refreshing) {
            this.mNotify = notify;
            this.ensureTarget();
            this.mRefreshing = refreshing;
            if(this.mRefreshing) {
                this.animateOffsetToCorrectPosition(this.mCurrentTargetOffsetTop, this.mRefreshListener);
            } else {
                this.startScaleDownAnimation(this.mRefreshListener);
            }
        }

    }

    private void startScaleDownAnimation(AnimationListener listener) {
        this.mScaleDownAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                MaterialSwipeRefreshLayout.this.setAnimationProgress(1.0F - interpolatedTime);
            }
        };
        this.mScaleDownAnimation.setDuration(150L);
        this.mCircleView.setAnimationListener(listener);
        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mScaleDownAnimation);

        // sometimes this thing gets stuck..
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mProgress.stop();
            }
        }, 160l);
    }

    private void startProgressAlphaStartAnimation() {
        this.mAlphaStartAnimation = this.startAlphaAnimation(this.mProgress.getAlpha(), 76);
    }

    private void startProgressAlphaMaxAnimation() {
        this.mAlphaMaxAnimation = this.startAlphaAnimation(this.mProgress.getAlpha(), 255);
    }

    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        if(this.mScale && this.isAlphaUsedForScale()) {
            return null;
        } else {
            Animation alpha = new Animation() {
                public void applyTransformation(float interpolatedTime, Transformation t) {
                    MaterialSwipeRefreshLayout.this.mProgress.setAlpha((int)((float)startingAlpha + (float)(endingAlpha - startingAlpha) * interpolatedTime));
                }
            };
            alpha.setDuration(300L);
            this.mCircleView.setAnimationListener((AnimationListener)null);
            this.mCircleView.clearAnimation();
            this.mCircleView.startAnimation(alpha);
            return alpha;
        }
    }

    public void setProgressBackgroundColor(int colorRes) {
        this.mCircleView.setBackgroundColor(colorRes);
        this.mProgress.setBackgroundColor(this.getResources().getColor(colorRes));
    }

    /** @deprecated */
    @Deprecated
    public void setColorScheme(int... colors) {
        this.setColorSchemeResources(colors);
    }

    public void setColorSchemeResources(int... colorResIds) {
        Resources res = this.getResources();
        int[] colorRes = new int[colorResIds.length];

        for(int i = 0; i < colorResIds.length; ++i) {
            colorRes[i] = res.getColor(colorResIds[i]);
        }

        this.setColorSchemeColors(colorRes);
    }

    public void setColorSchemeColors(int... colors) {
        this.ensureTarget();
        this.mProgress.setColorSchemeColors(colors);
    }

    public boolean isRefreshing() {
        return this.mRefreshing;
    }

    private void ensureTarget() {
        if(this.mTarget == null) {
            for(int i = 0; i < this.getChildCount(); ++i) {
                View child = this.getChildAt(i);
                if(!child.equals(this.mCircleView)) {
                    this.mTarget = child;
                    break;
                }
            }
        }

    }

    public void setDistanceToTriggerSync(int distance) {
        this.mTotalDragDistance = (float)distance;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = this.getMeasuredWidth();
        int height = this.getMeasuredHeight();
        if(this.getChildCount() != 0) {
            if(this.mTarget == null) {
                this.ensureTarget();
            }

            if(this.mTarget != null) {
                View child = this.mTarget;
                int childLeft = this.getPaddingLeft();
                int childTop = this.getPaddingTop();
                int childWidth = width - this.getPaddingLeft() - this.getPaddingRight();
                int childHeight = height - this.getPaddingTop() - this.getPaddingBottom();
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                int circleWidth = this.mCircleView.getMeasuredWidth();
                int circleHeight = this.mCircleView.getMeasuredHeight();
                this.mCircleView.layout(width / 2 - circleWidth / 2, this.mCurrentTargetOffsetTop, width / 2 + circleWidth / 2, this.mCurrentTargetOffsetTop + circleHeight);
            }
        }
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(this.mTarget == null) {
            this.ensureTarget();
        }

        if(this.mTarget != null) {
            this.mTarget.measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth() - this.getPaddingLeft() - this.getPaddingRight(), 1073741824), MeasureSpec.makeMeasureSpec(this.getMeasuredHeight() - this.getPaddingTop() - this.getPaddingBottom(), 1073741824));
            this.mCircleView.measure(MeasureSpec.makeMeasureSpec(this.mCircleWidth, 1073741824), MeasureSpec.makeMeasureSpec(this.mCircleHeight, 1073741824));
            if(!this.mUsingCustomStart && !this.mOriginalOffsetCalculated) {
                this.mOriginalOffsetCalculated = true;
                this.mCurrentTargetOffsetTop = this.mOriginalOffsetTop = -this.mCircleView.getMeasuredHeight();
            }

            this.mCircleViewIndex = -1;

            for(int index = 0; index < this.getChildCount(); ++index) {
                if(this.getChildAt(index) == this.mCircleView) {
                    this.mCircleViewIndex = index;
                    break;
                }
            }

        }
    }

    public boolean canChildScrollUp() {
        if(VERSION.SDK_INT < 14) {
            if(!(this.mTarget instanceof AbsListView)) {
                return this.mTarget.getScrollY() > 0;
            } else {
                AbsListView absListView = (AbsListView)this.mTarget;
                return absListView.getChildCount() > 0 && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            }
        } else {
            return ViewCompat.canScrollVertically(this.mTarget, -1);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        this.ensureTarget();
        int action = MotionEventCompat.getActionMasked(ev);
        if(this.mReturningToStart && action == 0) {
            this.mReturningToStart = false;
        }

        if(this.isEnabled() && !this.mReturningToStart && !this.canChildScrollUp() && !this.mRefreshing) {
            switch(action) {
                case 0:
                    this.setTargetOffsetTopAndBottom(this.mOriginalOffsetTop - this.mCircleView.getTop(), true);
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                    this.mIsBeingDragged = false;
                    float initialMotionY = this.getMotionEventY(ev, this.mActivePointerId);
                    if(initialMotionY == -1.0F) {
                        return false;
                    }

                    this.mInitialMotionY = initialMotionY;
                case 2:
                    if(this.mActivePointerId == -1) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but don\'t have an active pointer id.");
                        return false;
                    }

                    float y = this.getMotionEventY(ev, this.mActivePointerId);
                    if(y == -1.0F) {
                        return false;
                    }

                    float yDiff = y - this.mInitialMotionY;
                    if(yDiff > (float)this.mTouchSlop && !this.mIsBeingDragged) {
                        this.mIsBeingDragged = true;
                        this.mProgress.setAlpha(76);
                    }
                    break;
                case 1:
                case 3:
                    this.mIsBeingDragged = false;
                    this.mActivePointerId = -1;
                case 4:
                case 5:
                default:
                    break;
                case 6:
                    this.onSecondaryPointerUp(ev);
            }

            return this.mIsBeingDragged;
        } else {
            return false;
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        return index < 0?-1.0F:MotionEventCompat.getY(ev, index);
    }

    public void requestDisallowInterceptTouchEvent(boolean b) {
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int action = MotionEventCompat.getActionMasked(ev);
        if(this.mReturningToStart && action == 0) {
            this.mReturningToStart = false;
        }

        if(this.isEnabled() && !this.mReturningToStart && !this.canChildScrollUp()) {
            int pointerIndex;
            float y;
            float overscrollTop;
            switch(action) {
                case 0:
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                    this.mIsBeingDragged = false;
                    break;
                case 1:
                case 3:
                    if(this.mActivePointerId == -1) {
                        if(action == 1) {
                            Log.e(LOG_TAG, "Got ACTION_UP event but don\'t have an active pointer id.");
                        }

                        return false;
                    }

                    pointerIndex = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                    y = MotionEventCompat.getY(ev, pointerIndex);
                    overscrollTop = (y - this.mInitialMotionY) * 0.5F;
                    this.mIsBeingDragged = false;
                    if(overscrollTop > this.mTotalDragDistance) {
                        this.setRefreshing(true, true);
                    } else {
                        this.mRefreshing = false;
                        this.mProgress.setStartEndTrim(0.0F, 0.0F);
                        AnimationListener listener1 = null;
                        if(!this.mScale) {
                            listener1 = new AnimationListener() {
                                public void onAnimationStart(Animation animation) {
                                }

                                public void onAnimationEnd(Animation animation) {
                                    if(!MaterialSwipeRefreshLayout.this.mScale) {
                                        MaterialSwipeRefreshLayout.this.startScaleDownAnimation((AnimationListener)null);
                                    }

                                }

                                public void onAnimationRepeat(Animation animation) {
                                }
                            };
                        }

                        this.animateOffsetToStartPosition(this.mCurrentTargetOffsetTop, listener1);
                        this.mProgress.showArrow(false);
                    }

                    this.mActivePointerId = -1;
                    return false;
                case 2:
                    pointerIndex = MotionEventCompat.findPointerIndex(ev, this.mActivePointerId);
                    if(pointerIndex < 0) {
                        Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                        return false;
                    }

                    y = MotionEventCompat.getY(ev, pointerIndex);
                    overscrollTop = (y - this.mInitialMotionY) * 0.5F;
                    if(this.mIsBeingDragged) {
                        this.mProgress.showArrow(true);
                        float listener = overscrollTop / this.mTotalDragDistance;
                        if(listener < 0.0F) {
                            return false;
                        }

                        float dragPercent = Math.min(1.0F, Math.abs(listener));
                        float adjustedPercent = (float)Math.max((double)dragPercent - 0.4D, 0.0D) * 5.0F / 3.0F;
                        float extraOS = Math.abs(overscrollTop) - this.mTotalDragDistance;
                        float slingshotDist = this.mUsingCustomStart?this.mSpinnerFinalOffset - (float)this.mOriginalOffsetTop:this.mSpinnerFinalOffset;
                        float tensionSlingshotPercent = Math.max(0.0F, Math.min(extraOS, slingshotDist * 2.0F) / slingshotDist);
                        float tensionPercent = (float)((double)(tensionSlingshotPercent / 4.0F) - Math.pow((double)(tensionSlingshotPercent / 4.0F), 2.0D)) * 2.0F;
                        float extraMove = slingshotDist * tensionPercent * 2.0F;
                        int targetY = this.mOriginalOffsetTop + (int)(slingshotDist * dragPercent + extraMove);
                        if(this.mCircleView.getVisibility() != 0) {
                            this.mCircleView.setVisibility(0);
                        }

                        if(!this.mScale) {
                            ViewCompat.setScaleX(this.mCircleView, 1.0F);
                            ViewCompat.setScaleY(this.mCircleView, 1.0F);
                        }

                        float rotation;
                        if(overscrollTop < this.mTotalDragDistance) {
                            if(this.mScale) {
                                this.setAnimationProgress(overscrollTop / this.mTotalDragDistance);
                            }

                            if(this.mProgress.getAlpha() > 76 && !this.isAnimationRunning(this.mAlphaStartAnimation)) {
                                this.startProgressAlphaStartAnimation();
                            }

                            rotation = adjustedPercent * 0.8F;
                            this.mProgress.setStartEndTrim(0.0F, Math.min(0.8F, rotation));
                            this.mProgress.setArrowScale(Math.min(1.0F, adjustedPercent));
                        } else if(this.mProgress.getAlpha() < 255 && !this.isAnimationRunning(this.mAlphaMaxAnimation)) {
                            this.startProgressAlphaMaxAnimation();
                        }

                        rotation = (-0.25F + 0.4F * adjustedPercent + tensionPercent * 2.0F) * 0.5F;
                        this.mProgress.setProgressRotation(rotation);
                        this.setTargetOffsetTopAndBottom(targetY - this.mCurrentTargetOffsetTop, true);
                    }
                case 4:
                default:
                    break;
                case 5:
                    pointerIndex = MotionEventCompat.getActionIndex(ev);
                    this.mActivePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
                    break;
                case 6:
                    this.onSecondaryPointerUp(ev);
            }

            return true;
        } else {
            return false;
        }
    }

    private void animateOffsetToCorrectPosition(int from, AnimationListener listener) {
        this.mFrom = from;
        this.mAnimateToCorrectPosition.reset();
        this.mAnimateToCorrectPosition.setDuration(200L);
        this.mAnimateToCorrectPosition.setInterpolator(this.mDecelerateInterpolator);
        if(listener != null) {
            this.mCircleView.setAnimationListener(listener);
        }

        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, AnimationListener listener) {
        if(this.mScale) {
            this.startScaleDownReturnToStartAnimation(from, listener);
        } else {
            this.mFrom = from;
            this.mAnimateToStartPosition.reset();
            this.mAnimateToStartPosition.setDuration(200L);
            this.mAnimateToStartPosition.setInterpolator(this.mDecelerateInterpolator);
            if(listener != null) {
                this.mCircleView.setAnimationListener(listener);
            }

            this.mCircleView.clearAnimation();
            this.mCircleView.startAnimation(this.mAnimateToStartPosition);
        }

    }

    private void moveToStart(float interpolatedTime) {
        boolean targetTop = false;
        int targetTop1 = this.mFrom + (int)((float)(this.mOriginalOffsetTop - this.mFrom) * interpolatedTime);
        int offset = targetTop1 - this.mCircleView.getTop();
        this.setTargetOffsetTopAndBottom(offset, false);
    }

    private void startScaleDownReturnToStartAnimation(int from, AnimationListener listener) {
        this.mFrom = from;
        if(this.isAlphaUsedForScale()) {
            this.mStartingScale = (float)this.mProgress.getAlpha();
        } else {
            this.mStartingScale = ViewCompat.getScaleX(this.mCircleView);
        }

        this.mScaleDownToStartAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = MaterialSwipeRefreshLayout.this.mStartingScale + -MaterialSwipeRefreshLayout.this.mStartingScale * interpolatedTime;
                MaterialSwipeRefreshLayout.this.setAnimationProgress(targetScale);
                MaterialSwipeRefreshLayout.this.moveToStart(interpolatedTime);
            }
        };
        this.mScaleDownToStartAnimation.setDuration(150L);
        if(listener != null) {
            this.mCircleView.setAnimationListener(listener);
        }

        this.mCircleView.clearAnimation();
        this.mCircleView.startAnimation(this.mScaleDownToStartAnimation);
    }

    private void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        if (watcher != null) {
            watcher.showBars();
        }

        this.mCircleView.bringToFront();
        this.mCircleView.offsetTopAndBottom(offset);
        this.mCurrentTargetOffsetTop = this.mCircleView.getTop();
        if(requiresUpdate && VERSION.SDK_INT < 11) {
            this.invalidate();
        }

    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = MotionEventCompat.getActionIndex(ev);
        int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if(pointerId == this.mActivePointerId) {
            int newPointerIndex = pointerIndex == 0?1:0;
            this.mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }

    }

    public interface OnRefreshListener {
        void onRefresh();
    }
}
