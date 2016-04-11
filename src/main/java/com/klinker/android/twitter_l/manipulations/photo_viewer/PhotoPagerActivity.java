package com.klinker.android.twitter_l.manipulations.photo_viewer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.*;
import android.widget.ImageButton;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.PhotoPagerAdapter;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.DetailedTweetView;
import com.klinker.android.twitter_l.manipulations.widgets.HackyViewPager;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

public class PhotoPagerActivity extends AppCompatActivity {

    public static void startActivity(Context context, long tweetId, String links, int startPage) {
        startActivity(context, tweetId, links, startPage, false);
    }

    public static void startActivity(Context context, long tweetId, String links, int startPage, boolean hideInfo) {
        Intent viewImage = new Intent(context, PhotoPagerActivity.class);

        viewImage.putExtra("url", links);
        viewImage.putExtra("start_page", startPage);
        viewImage.putExtra("tweet_id", tweetId);
        viewImage.putExtra("hide_info", hideInfo);

        context.startActivity(viewImage);
    }

    String url = null;

    PhotoPagerAdapter adapter;
    HackyViewPager pager;

    ImageButton download;
    ImageButton share;
    ImageButton info;
    View gradient;

    BottomSheetLayout bottomSheet;

    Handler sysVis;

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        sharedPrefs.edit().putBoolean("from_activity", true).commit();

        super.finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) { }

        if (Build.VERSION.SDK_INT > 18) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        url = getIntent().getStringExtra("url");
        int startPage = getIntent().getIntExtra("start_page", 0);

        String[] urlList = url.split(" ");

        if (TextUtils.isEmpty(url)) {
            finish();
            return;
        }

        for (int i = 0; i < urlList.length; i++) {
            if (urlList[i].contains("imgur")) {
                urlList[i] = urlList[i].replace("t.jpg", ".jpg");
            }

            if (url.contains("insta")) {
                url = url.substring(0, url.length() - 1) + "l";
            }
        }

        Utils.setUpTweetTheme(this, AppSettings.getInstance(this));
        setContentView(R.layout.photo_pager_activity);

        gradient = findViewById(R.id.buttons_layout);
        download = (ImageButton) findViewById(R.id.save_button);
        info = (ImageButton) findViewById(R.id.info_button);
        share = (ImageButton) findViewById(R.id.share_button);

        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottom_sheet);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PhotoFragment) adapter.getItem(pager.getCurrentItem())).saveImage();
            }
        });

        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfo();
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((PhotoFragment) adapter.getItem(pager.getCurrentItem())).shareImage();
            }
        });

        pager = (HackyViewPager) findViewById(R.id.pager);
        adapter = new PhotoPagerAdapter(getSupportFragmentManager(), this, urlList);

        pager.setAdapter(adapter);
        pager.setCurrentItem(startPage);

        ab = getSupportActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
            ab.setTitle("");
            ab.setIcon(transparent);
        }

        setCurrentPageTitle(startPage);

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setCurrentPageTitle(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        sysVis = new Handler();
        sysVis.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideSystemUI();
            }
        }, 6000);

        final long tweetId = getIntent().getLongExtra("tweet_id", 0);
        if (tweetId != 0) {
            prepareInfo(tweetId);
        } else {
            ((View)info.getParent()).setVisibility(View.GONE);
        }

        if (getIntent().getBooleanExtra("hide_info", false)) {
            ((View)info.getParent()).setVisibility(View.GONE);
        }
    }

    android.support.v7.app.ActionBar ab;

    public void setCurrentPageTitle(int page) {
        page = page + 1;
        if (ab != null) {
            ab.setTitle(page + " of " + adapter.getCount());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        overridePendingTransition(0, 0);
        finish();

        final Intent restart = new Intent(this, PhotoPagerActivity.class);
        restart.putExtra("url", url);
        restart.putExtra("config_changed", true);
        restart.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        // we have to delay it just a little bit so that it isn't consumed by the timeline changing orientation
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(restart);
                overridePendingTransition(0, 0);
            }
        }, 250);
    }


    public void hideSystemUI() {
        sysUiShown = false;

        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);

            startAlphaAnimation(gradient, 1, 0);
            startAlphaAnimation(share, 1, 0);
            startAlphaAnimation(download, 1, 0);
            startAlphaAnimation(info, 1, 0);
        } catch (Exception e) {

        }
    }

    boolean sysUiShown = true;
    public void showSystemUI() {
        sysUiShown = true;

        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            startAlphaAnimation(gradient, 0, 1);
            startAlphaAnimation(share, 0, 1);
            startAlphaAnimation(download, 0, 1);
            startAlphaAnimation(info, 0, 1);
        } catch (Exception e) {

        }
    }

    private DetailedTweetView tweetView;

    public void prepareInfo(final long tweetId) {
        tweetView = DetailedTweetView.create(this, tweetId);
        tweetView.setShouldShowImage(false);
    }

    public void showInfo() {
        View v = tweetView.getView();
        AppSettings settings = AppSettings.getInstance(this);
        if (settings.darkTheme || settings.blackTheme) {
            v.setBackgroundResource(R.color.dark_background);
        } else {
            v.setBackgroundResource(R.color.white);
        }

        bottomSheet.showWithSheetView(v);
    }

    private void startAlphaAnimation(final View v, float start, final float finish) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(v, View.ALPHA, start, finish);
        alpha.setDuration(350);
        alpha.setInterpolator(TimeLineCursorAdapter.ANIMATION_INTERPOLATOR);
        alpha.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (v.getVisibility() == View.GONE) {
                    v.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (finish == 0) {
                    v.setEnabled(false);
                } else {
                    v.setEnabled(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
        alpha.start();
    }
}
