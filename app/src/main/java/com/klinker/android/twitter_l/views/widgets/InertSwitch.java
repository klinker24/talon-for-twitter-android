package com.klinker.android.twitter_l.views.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Switch;

// CheckBox that does not react to any user event in order to let the container handle them.
public class InertSwitch extends Switch {

    @SuppressWarnings("unused")
    public InertSwitch(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    public InertSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressWarnings("unused")
    public InertSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        // Make the checkbox not respond to any user event
        return false;
    }
}