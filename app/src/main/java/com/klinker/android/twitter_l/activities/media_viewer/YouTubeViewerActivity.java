package com.klinker.android.twitter_l.activities.media_viewer;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetYouTubeFragment;
import com.klinker.android.twitter_l.activities.tweet_viewer.VideoFragment;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.views.DetailedTweetView;
import com.klinker.android.twitter_l.views.NavBarOverlayLayout;

import java.io.File;

public class YouTubeViewerActivity extends AppCompatActivity {

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        sharedPrefs.edit().putBoolean("from_activity", true).commit();

        super.finish();
    }
    
    public Context context;
    public String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        context = this;

        url = getIntent().getStringExtra("url");

        if (url == null) {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        AppSettings settings = new AppSettings(context);
        Utils.setUpTheme(this, settings);

        setContentView(R.layout.video_view_activity);

        YouTubePlayerFragment fragment = TweetYouTubeFragment.getInstance(context, url);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment, fragment)
                .commit();

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
            ab.setTitle("");
            ab.setIcon(transparent);
            ab.hide();
        }

        findViewById(R.id.fragment).setPadding(0,0,0,0);
        new NavBarOverlayLayout(this).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.buttons_layout).setVisibility(View.GONE);
        } else if (url != null && !url.contains("youtu")) {
            findViewById(R.id.buttons_layout).setVisibility(View.VISIBLE);
        }
    }
}
