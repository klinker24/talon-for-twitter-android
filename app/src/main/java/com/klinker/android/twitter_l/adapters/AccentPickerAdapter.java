package com.klinker.android.twitter_l.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.data.ThemeColor;
import com.klinker.android.twitter_l.manipulations.widgets.ColorPreviewButton;
import com.klinker.android.twitter_l.settings.AppSettings;

import java.util.List;

public class AccentPickerAdapter extends ArrayAdapter<ThemeColor> {

    private List<ThemeColor> colors;
    private int currentColor;
    private View.OnClickListener itemClickedListener;

    public AccentPickerAdapter(Context context, List<ThemeColor> colors, View.OnClickListener itemClickedListener) {
        super(context, android.R.layout.simple_list_item_1, colors);
        this.colors = colors;
        this.currentColor = AppSettings.getInstance(context).themeColors.accentColor;
        this.itemClickedListener = itemClickedListener;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        ThemeColor color = colors.get(position);
        final FrameLayout frame = getFrameLayout();
        final ColorPreviewButton button = getColorPreviewButton();
        button.setInnerColor(color.accentColor);
        button.setOuterColor(color.accentColorLight);
        frame.addView(button);

        if (color.accentColor == currentColor) {
            ImageView checked = new ImageView(getContext());
            checked.setImageResource(R.drawable.ic_checked);
            checked.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            frame.addView(checked);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setTag(position);
                itemClickedListener.onClick(button);
            }
        });

        return frame;
    }

    protected FrameLayout getFrameLayout() {
        return new FrameLayout(getContext());
    }

    protected ColorPreviewButton getColorPreviewButton() {
        return new ColorPreviewButton(getContext());
    }
}
