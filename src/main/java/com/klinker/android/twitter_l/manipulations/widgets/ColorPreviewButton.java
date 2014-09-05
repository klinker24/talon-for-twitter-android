package com.klinker.android.twitter_l.manipulations.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class ColorPreviewButton extends View {

    private static final String TAG = "ColorPreviewButton";
    private static final int DEFAULT_INNER_COLOR = 0xffff9800;
    private static final int DEFAULT_OUTER_COLOR = 0xff3f51b5;
    private static final int DEFAULT_INNER_SIZE = 30;
    private static final int DEFAULT_OUTER_SIZE = 36;
    private static final int DEFAULT_SIZE = 72;

    private float innerSize;
    private float currentOuterSize;
    private float maxOuterSize;
    private float size;

    private Paint innerPaint;
    private Paint outerPaint;

    private ShowThread shower;
    private HideThread hider;

    private OnClickListener onClickListener;

    public ColorPreviewButton(Context context) {
        this(context, null);
    }

    public ColorPreviewButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPreviewButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        size = toPx(DEFAULT_SIZE);
        innerSize = toPx(DEFAULT_INNER_SIZE);
        currentOuterSize = innerSize;
        maxOuterSize = toPx(DEFAULT_OUTER_SIZE);

        setMinimumHeight((int) size);
        setMinimumWidth((int) size);

        innerPaint = new Paint();
        innerPaint.setAntiAlias(true);
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setColor(DEFAULT_INNER_COLOR);

        outerPaint = new Paint();
        outerPaint.setAntiAlias(true);
        outerPaint.setStyle(Paint.Style.FILL);
        outerPaint.setColor(DEFAULT_OUTER_COLOR);

        shower = new ShowThread(this);
        hider = new HideThread(this);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawCircle(size / 2, size / 2, currentOuterSize, outerPaint);
        canvas.drawCircle(size / 2, size / 2, innerSize, innerPaint);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, w, oldW, oldH);
        size = w;
        maxOuterSize = size / 2;
        innerSize = size / 2 - toPx(6);
        currentOuterSize = innerSize;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = this.getMeasuredWidth();
        setMeasuredDimension(w, w);
    }

    public void setInnerColor(int color) {
        this.innerPaint.setColor(color);
    }

    public void setOuterColor(int color) {
        this.outerPaint.setColor(color);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            shower = new ShowThread(this);
            shower.setRunning(true);
            hider.setRunning(false);
            shower.start();
            return true;
        } else if (e.getAction() == MotionEvent.ACTION_CANCEL || e.getAction() == MotionEvent.ACTION_UP) {
            hider = new HideThread(this);
            shower.setRunning(false);
            hider.setRunning(true);
            hider.start();

            if (e.getAction() == MotionEvent.ACTION_UP) {
                if (onClickListener != null) {
                    onClickListener.onClick(this);
                }
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        this.onClickListener = listener;
    }

    private float toPx(int dp) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    public abstract class AnimThread extends Thread {
        public View view;
        private boolean running = false;

        public AnimThread(View v) {
            super();
            view = v;
        }

        public AnimThread setRunning(boolean run) {
            running = run;
            return this;
        }

        private final static int MAX_FPS = 60;
        private final static int MAX_FRAME_SKIPS = 5;
        private final static int FRAME_PERIOD = 1000 / MAX_FPS;

        @Override
        public void run() {
            long beginTime;
            long timeDiff;
            int sleepTime;
            int framesSkipped;

            while (running) {
                try {
                    beginTime = System.currentTimeMillis();
                    framesSkipped = 0;

                    updateView();
                    view.postInvalidate();

                    timeDiff = System.currentTimeMillis() - beginTime;
                    sleepTime = (int) (FRAME_PERIOD - timeDiff);

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                        }
                    }

                    while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                        boolean updating = updateView();
                        sleepTime += FRAME_PERIOD;
                        framesSkipped++;

                        if (!updating) {
                            try {
                                setRunning(false);
                                join();
                                stop();
                            } catch (Exception e) {
                            }
                        }
                    }
                } finally {
                    view.postInvalidate();
                }
            }
        }

        public abstract boolean updateView();
    }

    public class ShowThread extends AnimThread {

        public ShowThread(View v) {
            super(v);
        }

        public boolean updateView() {
            if (currentOuterSize < maxOuterSize) {
                currentOuterSize += 1;
                return true;
            }

            return false;
        }
    }

    public class HideThread extends AnimThread {

        public HideThread(View v) {
            super(v);
        }

        public boolean updateView() {
            if (currentOuterSize > innerSize) {
                currentOuterSize -= 1;
                return true;
            }
            return false;
        }
    }
}
