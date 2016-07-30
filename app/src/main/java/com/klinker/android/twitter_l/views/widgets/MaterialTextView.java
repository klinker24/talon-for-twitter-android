package com.klinker.android.twitter_l.views.widgets;

import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.klinker.android.twitter_l.settings.AppSettings;

public class MaterialTextView extends FontPrefTextView {
    private int FOUR_DIP;
    private int TEXT_SIZE;
    private int PREF_SCALER;

    private float lineHeightMultiplierHint = 1f;
    private float lineHeightHint = 0f;
    private int unalignedTopPadding = 0;

    public MaterialTextView(Context context) {
        this(context, null);
    }

    public MaterialTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public MaterialTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        AppSettings settings = AppSettings.getInstance(getContext());

        FOUR_DIP = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());

        TEXT_SIZE = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, settings.textSize, getResources().getDisplayMetrics());

        PREF_SCALER = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()) * settings.lineSpacingScalar;

        lineHeightMultiplierHint = 1f;
        lineHeightHint = TEXT_SIZE + PREF_SCALER;
        unalignedTopPadding = getPaddingTop();

        setIncludeFontPadding(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElegantTextHeight(false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        recomputeLineHeight();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int height = getMeasuredHeight();
        final int gridOverhang = height % FOUR_DIP;
        if (gridOverhang != 0) {
            final int addition = FOUR_DIP - gridOverhang;
            super.setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                    getPaddingBottom() + addition);
            setMeasuredDimension(getMeasuredWidth(), height + addition);
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        if (unalignedTopPadding != top) {
            unalignedTopPadding = top;
            recomputeLineHeight();
        }
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        if (unalignedTopPadding != top) {
            unalignedTopPadding = top;
            recomputeLineHeight();
        }
    }

    public float getLineHeightMultiplierHint() {
        return lineHeightMultiplierHint;
    }

    public void setLineHeightMultiplierHint(float lineHeightMultiplierHint) {
        this.lineHeightMultiplierHint = lineHeightMultiplierHint;
        recomputeLineHeight();
    }

    public float getLineHeightHint() {
        return lineHeightHint;
    }

    public void setLineHeightHint(float lineHeightHint) {
        this.lineHeightHint = lineHeightHint;
        recomputeLineHeight();
    }

    private void recomputeLineHeight() {
        // ensure that the first line's baselines sits on 4dp grid by setting the top padding
        final Paint.FontMetricsInt fm = getPaint().getFontMetricsInt();
        final int gridAlignedTopPadding = (int) (FOUR_DIP * (float)
                Math.ceil((unalignedTopPadding + Math.abs(fm.ascent)) / FOUR_DIP)
                - Math.ceil(Math.abs(fm.ascent)));
        super.setPadding(
                getPaddingLeft(), gridAlignedTopPadding, getPaddingRight(), getPaddingBottom());

        // ensures line height is a multiple of 4dp
        final int fontHeight = Math.abs(fm.ascent - fm.descent) + fm.leading;
        final float desiredLineHeight = (lineHeightHint > 0)
                ? lineHeightHint
                : lineHeightMultiplierHint * fontHeight;

        final int baselineAlignedLineHeight =
                (int) (FOUR_DIP * (float) Math.ceil(desiredLineHeight / FOUR_DIP));
        setLineSpacing(baselineAlignedLineHeight - fontHeight, 1f);
    }
}