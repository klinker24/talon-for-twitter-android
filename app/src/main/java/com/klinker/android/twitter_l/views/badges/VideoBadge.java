package com.klinker.android.twitter_l.views.badges;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class VideoBadge  extends Drawable {

    private static final String VIDEO = "VIDEO";
    private static final int TEXT_SIZE = 15;    // sp
    private static final int PADDING = 4;       // dp
    private static final int CORNER_RADIUS = 2; // dp
    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int STROKE_COLOR = Color.DKGRAY;
    private static final String TYPEFACE = "sans-serif-black";
    private static final int TYPEFACE_STYLE = Typeface.NORMAL;
    private Bitmap bitmap;
    private int width;
    private int height;
    private final Paint paint;
    private final long duration;

    public VideoBadge(Context context, long duration) {
        this.duration = duration;
        String text = duration > 0 ? VIDEO + " - " + getDuration() : VIDEO;

//        if (bitmap == null) {
            final DisplayMetrics dm = context.getResources().getDisplayMetrics();
            final float density = dm.density;
            final float scaledDensity = dm.scaledDensity;
            TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint
                    .SUBPIXEL_TEXT_FLAG);
            textPaint.setTypeface(Typeface.create(TYPEFACE, TYPEFACE_STYLE));
            textPaint.setTextSize(TEXT_SIZE * scaledDensity);

            final float padding = PADDING * density;
            final float cornerRadius = CORNER_RADIUS * density;
            final Rect textBounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
            height = (int) (padding + textBounds.height() + padding);
            width = (int) (padding + textBounds.width() + padding);
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setHasAlpha(true);
            final Canvas canvas = new Canvas(bitmap);
            Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            backgroundPaint.setColor(BACKGROUND_COLOR);

            if (Build.VERSION.SDK_INT >= 21) {
                canvas.drawRoundRect(0, 0, width, height, cornerRadius, cornerRadius,
                        backgroundPaint);

                /*backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                backgroundPaint.setColor(STROKE_COLOR);
                backgroundPaint.setStyle(Paint.Style.STROKE);
                backgroundPaint.setStrokeWidth(Utils.toDP(1, context));
                canvas.drawRoundRect(0, 0, width, height, cornerRadius, cornerRadius,
                        backgroundPaint);*/
            } else {
                canvas.drawRect(0, 0, width, height, backgroundPaint);

                /*backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                backgroundPaint.setColor(Color.BLACK);
                backgroundPaint.setStyle(Paint.Style.STROKE);
                backgroundPaint.setStrokeWidth(Utils.toDP(1, context));
                canvas.drawRect(0, 0, width, height, backgroundPaint);*/
            }

            // punch out the word 'GIF', leaving transparency
            textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawText(text, padding, height - padding, textPaint);

            /*textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            textPaint.setTypeface(Typeface.create(TYPEFACE, TYPEFACE_STYLE));
            textPaint.setTextSize(TEXT_SIZE * scaledDensity);
            textPaint.getTextBounds(VIDEO, 0, VIDEO.length(), textBounds);
            textPaint.setStyle(Paint.Style.STROKE);
            textPaint.setColor(STROKE_COLOR);
            textPaint.setStrokeWidth(Utils.toDP(1, context));
            canvas.drawText(VIDEO, padding, height - padding, textPaint);*/
//        }
        paint = new Paint();
    }

    @Override
    public int getIntrinsicWidth() {
        return width;
    }

    @Override
    public int getIntrinsicHeight() {
        return height;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(bitmap, getBounds().left, getBounds().top, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        // ignored
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    private String getDuration() {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }
}