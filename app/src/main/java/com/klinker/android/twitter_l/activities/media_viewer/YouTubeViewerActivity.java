package com.klinker.android.twitter_l.activities.media_viewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.google.android.youtube.player.YouTubePlayerFragment;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetYouTubeFragment;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

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
            getWindow().setNavigationBarColor(Color.BLACK);
        }

        AppSettings settings = new AppSettings(context);
        Utils.setUpTheme(this, settings);

        setContentView(R.layout.video_view_activity);

        YouTubePlayerFragment fragment = TweetYouTubeFragment.getInstance(context, url);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment, fragment)
                .commit();

        androidx.appcompat.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
            ab.setTitle("");
            ab.setIcon(transparent);
            ab.hide();
        }

        findViewById(R.id.toolbar).setVisibility(View.GONE);
        findViewById(R.id.fragment).setPadding(0,0,0,0);
    }
}
