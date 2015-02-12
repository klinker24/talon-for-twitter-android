package com.klinker.android.twitter_l.manipulations.photo_viewer;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.*;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.PhotoPagerAdapter;

public class PhotoPagerActivity extends Activity {

    String url = null;

    PhotoPagerAdapter adapter;
    ViewPager pager;

    Handler sysVis;

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

        setContentView(R.layout.photo_pager_activity);

        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new PhotoPagerAdapter(getFragmentManager(), this, urlList);

        pager.setAdapter(adapter);
        pager.setCurrentItem(startPage);

        ab = getActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setDisplayShowHomeEnabled(false);
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
    }

    ActionBar ab;

    public void setCurrentPageTitle(int page) {
        page = page + 1;
        if (ab != null) {
            ab.setTitle(page + " of " + adapter.getCount());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.photo_viewer, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_save_image:
                ((PhotoFragment)adapter.getItem(pager.getCurrentItem())).saveImage();
                return true;

            case R.id.menu_share_image:
                ((PhotoFragment)adapter.getItem(pager.getCurrentItem())).shareImage();
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
        sysVis.removeCallbacksAndMessages(null);

        sysUiShown = false;

        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } catch (Exception e) {
            // fragment not attached to activity
        }
    }

    public boolean sysUiShown = true;

    public void showSystemUI() {
        sysVis.removeCallbacksAndMessages(null);

        sysUiShown = true;

        try {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        } catch (Exception e) {
            // fragment not attached to activity
        }
    }
}
