package com.klinker.android.twitter_l.manipulations.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Created by luke on 6/30/14.
 */
public class NotifyScrollView extends ScrollView {

    /**
     * @author Cyril Mottier
     */
    public interface OnScrollChangedListener {
        void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt);
    }

    private OnScrollChangedListener mOnScrollChangedListener;

    public NotifyScrollView(Context context) {
        super(context);
    }

    public NotifyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean interceptTouch = true;
    public void setInterceptTouch(boolean i) {
        Log.v("intercept_touch", i + "");
        interceptTouch = i;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                Log.i("VerticalScrollview", "onInterceptTouchEvent: DOWN super false" );
                super.onTouchEvent(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                if (interceptTouch) {
                    return true;
                } else {
                    return false;
                }

            case MotionEvent.ACTION_CANCEL:
                Log.i("VerticalScrollview", "onInterceptTouchEvent: CANCEL super false" );
                super.onTouchEvent(ev);
                break;

            case MotionEvent.ACTION_UP:
                Log.i("VerticalScrollview", "onInterceptTouchEvent: UP super false" );
                return false;

            default: Log.i("VerticalScrollview", "onInterceptTouchEvent: " + action ); break;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        Log.i("VerticalScrollview", "onTouchEvent. action: " + ev.getAction());
        return true;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {
            mOnScrollChangedListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    public void setOnScrollChangedListener(OnScrollChangedListener listener) {
        mOnScrollChangedListener = listener;
    }

}
