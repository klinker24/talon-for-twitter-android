package com.klinker.android.twitter_l.views.preference;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class IconPreference extends Preference{

    public IconPreference(Context context) {
        super(context);
        init();
    }

    public IconPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public void init() {

    }


    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        view.setPadding(0, Utils.toDP(3, getContext()), 0, Utils.toDP(3, getContext()));

        ImageView icon = (ImageView) view.findViewById(android.R.id.icon);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) icon.getLayoutParams();
        params.leftMargin = Utils.toDP(30, getContext());
        params.rightMargin = Utils.toDP(12, getContext());

        icon.setLayoutParams(params);

        Drawable drawable = icon.getDrawable();

        AppSettings settings = AppSettings.getInstance(getContext());
        if (drawable != null) {
            drawable.setColorFilter(settings.darkTheme ? Color.WHITE : Color.BLACK, PorterDuff.Mode.MULTIPLY);
            icon.setImageDrawable(drawable);
        }
    }
}
