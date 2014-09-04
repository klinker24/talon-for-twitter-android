package com.klinker.android.twitter_l.manipulations;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.manipulations.widgets.PopupLayout;

/**
 * Created by lucasklinker on 7/26/14.
 */
public class WebPopupLayout extends PopupLayout {

    private View webView;

    public WebPopupLayout(Context context, View webView) {
        super(context);

        this.webView = webView;

        showTitle(false);
        setFullScreen();

        try {
            addView(webView);
        } catch (Exception e) {
            dontShow = true;
        }
    }

    public WebPopupLayout(Context context, View webView, boolean windowed) {
        super(context, windowed);

        this.webView = webView;

        showTitle(false);
        setFullScreen();

        try {
            addView(webView);
        } catch (Exception e) {
            dontShow = true;
        }
    }

    @Override
    public View setMainLayout() {
        return null;
    }
}
