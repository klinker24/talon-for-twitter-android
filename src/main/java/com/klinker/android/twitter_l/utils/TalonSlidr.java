package com.klinker.android.twitter_l.utils;

import android.animation.ArgbEvaluator;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.R;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrPosition;
import com.r0adkll.slidr.widget.SliderPanel;

public class TalonSlidr extends Slidr {

    /**
     * Attach a slider mechanism to an activity based on the passed {@link com.r0adkll.slidr.model.SlidrConfig}
     *
     * @param activity      the activity to attach the slider to
     * @return              a {@link com.r0adkll.slidr.model.SlidrInterface} that allows
     *                      the user to lock/unlock the sliding mechanism for whatever purpose.
     */
    public static SlidrInterface attach(final Activity activity){

        final SlidrConfig config = new SlidrConfig.Builder()
                .position(SlidrPosition.HORIZONTAL)
                .build();

        // Hijack the decorview
        ViewGroup decorView = (ViewGroup)activity.getWindow().getDecorView();
        View oldScreen = decorView.getChildAt(0);
        decorView.removeViewAt(0);

        // Setup the slider panel and attach it to the decor
        final SliderPanel panel = new SliderPanel(activity, oldScreen, config);
        panel.setId(R.id.slidable_panel);
        oldScreen.setId(R.id.slidable_content);
        panel.addView(oldScreen);
        decorView.addView(panel, 0);

        // Set the panel slide listener for when it becomes closed or opened
        panel.setOnPanelSlideListener(new SliderPanel.OnPanelSlideListener() {

            private final ArgbEvaluator mEvaluator = new ArgbEvaluator();

            @Override
            public void onStateChanged(int state) {
                if(config.getListener() != null){
                    config.getListener().onSlideStateChanged(state);
                }
            }

            @Override
            public void onClosed() {
                if(config.getListener() != null){
                    config.getListener().onSlideClosed();
                }

                activity.onBackPressed();
                activity.overridePendingTransition(0, 0);
            }

            @Override
            public void onOpened() {
                if(config.getListener() != null){
                    config.getListener().onSlideOpened();
                }
            }

            @Override
            public void onSlideChange(float percent) {
                // Interpolate the statusbar color
                // TODO: Add support for KitKat
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                        config.areStatusBarColorsValid()){

                    int newColor = (int) mEvaluator.evaluate(percent, config.getPrimaryColor(),
                            config.getSecondaryColor());

                    activity.getWindow().setStatusBarColor(newColor);
                }

                if(config.getListener() != null){
                    config.getListener().onSlideChange(percent);
                }
            }
        });

        // Setup the lock interface
        SlidrInterface slidrInterface = new SlidrInterface() {
            @Override
            public void lock() {
                panel.lock();
            }

            @Override
            public void unlock() {
                panel.unlock();
            }
        };

        // Return the lock interface
        return slidrInterface;

    }

}
