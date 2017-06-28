package com.klinker.android.twitter_l.activities.setup.material_login;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.github.paolorotolo.appintro.AppIntro2;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.MainActivity;
import com.klinker.android.twitter_l.utils.AnalyticsHelper;
import com.klinker.android.twitter_l.utils.ServiceUtils;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;


public class MaterialLogin extends AppIntro2 {

    // CHANGE THIS TO UPDATE THE KEY VERSION
    public static final int KEY_VERSION = 4;

    public interface Callback {
        void onDone();
    }

    private ViewPager pager;
    private ImageView nextButton;
    private ImageView skipButton;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        AnalyticsHelper.startLogin(this);

        SharedPreferences sharedPrefs = AppSettings.getInstance(this).sharedPrefs;

        final int currAccount = sharedPrefs.getInt("current_account", 1);
        sharedPrefs.edit().putInt("key_version_" + currAccount, KEY_VERSION).apply();

        addSlides();

        setVibrate(true);
        setImmersive(true);
        showStatusBar(false);
        showSkipButton(false);
        setVibrateIntensity(30);
        setOffScreenPageLimit(4);

        pager = (ViewPager) findViewById(R.id.view_pager);
        nextButton = (ImageView) findViewById(R.id.next);

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int getRtlPage(int currentPage) {
                boolean isRtl = getResources().getBoolean(R.bool.isRTL);
                if (isRtl && currentPage == 1) {
                    return 2;
                } else if (isRtl && currentPage == 2) {
                    return 1;
                } else {
                    return currentPage;
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (position == getRtlPage(1)) {
                    // hacky to disable paging
                    pager.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return true;
                        }
                    });

                    nextButton.setVisibility(View.INVISIBLE);
                    AnalyticsHelper.loginToTwitter(MaterialLogin.this);

                    loginFragment.start(new Callback() {
                        @Override
                        public void onDone() {
                            nextButton.performClick();
                            AnalyticsHelper.finishLoginToTwitter(MaterialLogin.this);
                        }
                    });
                } else if (position == getRtlPage(2)) {
                    nextButton.setVisibility(View.INVISIBLE);

                    downloadFragment.start(new Callback() {
                        @Override
                        public void onDone() {
                            nextButton.performClick();
                            AnalyticsHelper.downloadTweets(MaterialLogin.this);
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
        followUsers();
        startTimeline();

        AnalyticsHelper.finishLogin(this);
    }

    private ImageFragment welcomeFragment;
    private LoginFragment loginFragment;
    private DownloadFragment downloadFragment;
    private ImageFragment finishedFragment;

    private void addSlides() {
        welcomeFragment = ImageFragment.newInstance(getString(R.string.first_welcome), getString(R.string.first_info), "https://raw.githubusercontent.com/klinker24/Talon-for-Twitter/master/Other/Icon/talon-adaptive.png", Color.parseColor("#5C6BC0"));
        loginFragment = LoginFragment.getInstance();
        downloadFragment = DownloadFragment.getInstance();
        finishedFragment = ImageFragment.newInstance(getString(R.string.third_welcome), getString(R.string.follow_me_description), "https://g.twimg.com/Twitter_logo_white.png", Color.parseColor("#4CAF50"));

        addSlide(welcomeFragment);
        addSlide(loginFragment);
        addSlide(downloadFragment);
        addSlide(finishedFragment);
    }

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(this);

        sharedPrefs.edit().putBoolean("version_3_2", false).commit();

        super.finish();
    }
    @Override
    public void onBackPressed() {
        // we don't want them to back out of the activity
        if (pager.getCurrentItem() == 3) {
            // final page
            startTimeline();
        }
    }

    public void restartLogin() {
        new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.login_error))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent restart = new Intent(MaterialLogin.this, MaterialLogin.class);
                        finish();
                        AppSettings.invalidate();
                        startActivity(restart);
                    }
                })
                .create()
                .show();
    }

    private void startTimeline() {
        Context context = this;

        ServiceUtils.rescheduleAllServices(context);

        finish();

        Intent timeline = new Intent(context, MainActivity.class);
        timeline.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        timeline.putExtra("tutorial", true);
        AppSettings.invalidate();
        startActivity(timeline);
    }

    private void followUsers() {
        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                Twitter twitter = Utils.getTwitter(MaterialLogin.this, null);

                try { twitter.createFriendship("TalonAndroid"); } catch (Exception e) { }
                try { twitter.createFriendship("lukeklinker"); } catch (Exception e) { }
                try { twitter.createFriendship("KlinkerApps"); } catch (Exception e) { }
            }
        }).start();
    }
}
