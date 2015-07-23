package com.klinker.android.twitter_l.ui.setup.material_login;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.klinker.android.twitter_l.R;


public class MaterialLogin extends MaterialLVLActivity {

    public interface Callback {
        void onDone();
    }

    private ViewPager pager;
    private ImageView nextButton;

    @Override
    public void init(Bundle bundle) {
        super.init(bundle);

        addSlides();

        setVibrate(true);
        setVibrateIntensity(30);
        setOffScreenPageLimit(4);

        pager = (ViewPager) findViewById(R.id.view_pager);
        nextButton = (ImageView) findViewById(R.id.next);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    // hacky to disable paging
                    pager.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return true;
                        }
                    });

                    nextButton.setVisibility(View.INVISIBLE);

                    loginFragment.start(new Callback() {
                        @Override
                        public void onDone() {
                            nextButton.performClick();
                        }
                    });
                } else if (position == 2) {

                    nextButton.setVisibility(View.INVISIBLE);

                    downloadFragment.start(new Callback() {
                        @Override
                        public void onDone() {
                            nextButton.performClick();
                        }
                    });
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    public void onDonePressed() {
        finish();
    }

    private AppIntroFragment welcomeFragment;
    private LoginFragment loginFragment;
    private DownloadFragment downloadFragment;
    private FinishedFragment finishedFragment;

    private void addSlides() {
        welcomeFragment = AppIntroFragment.newInstance(getString(R.string.first_welcome), getString(R.string.first_info), R.mipmap.ic_launcher, Color.parseColor("#5C6BC0"));
        loginFragment = LoginFragment.getInstance();
        downloadFragment = DownloadFragment.getInstance();
        finishedFragment = FinishedFragment.getInstance();

        addSlide(welcomeFragment);
        addSlide(loginFragment);
        addSlide(downloadFragment);
        addSlide(finishedFragment);

        //addSlide(AppIntroFragment.newInstance("Welcome to Talon.", "Let's get you logged into the app", R.mipmap.ic_launcher, Color.parseColor("#00BCD4")));
        //addSlide(AppIntroFragment.newInstance("Welcome to Talon.", "Let's get you logged into the app", R.mipmap.ic_launcher, Color.parseColor("#4CAF50")));
    }

    @Override
    public void onBackPressed() {
        // we don't want them to back out of the activity
    }
}
