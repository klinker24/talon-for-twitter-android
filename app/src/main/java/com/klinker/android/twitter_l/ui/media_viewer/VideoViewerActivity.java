package com.klinker.android.twitter_l.ui.media_viewer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.DetailedTweetView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetYouTubeFragment;
import com.klinker.android.twitter_l.ui.tweet_viewer.VideoFragment;
import com.klinker.android.twitter_l.utils.IOUtils;

import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;
import com.klinker.android.twitter_l.utils.WebIntentBuilder;

import java.io.File;
import java.util.Calendar;

public class VideoViewerActivity extends AppCompatActivity {

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(context);

        sharedPrefs.edit().putBoolean("from_activity", true).commit();

        super.finish();
    }

    // link string can either be a single link to a gif surfaceView, or it can be all of the links in the tweet
    // and it will find the youtube one.
    public static void startActivity(Context context, long tweetId, String gifVideo, String linkString) {

        if (gifVideo != null && VideoMatcherUtil.noInAppPlayer(gifVideo)) {
            new WebIntentBuilder(context)
                    .setUrl(gifVideo)
                    .build().start();
        } else {
            String[] otherLinks = linkString.split("  ");
            String video = null;

            if (otherLinks.length > 0 && !otherLinks[0].equals("")) {
                for (String s : otherLinks) {
                    if (s.contains("youtu")) {
                        video = s;
                        break;
                    }
                }
            }

            if (video == null) {
                video = gifVideo;
            }

            Intent viewVideo = new Intent(context, VideoViewerActivity.class);

            video = video.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4");

            viewVideo.putExtra("url", video);
            viewVideo.putExtra("tweet_id", tweetId);

            Log.v("video_url", video);

            context.startActivity(viewVideo);
        }
    }

    public Context context;
    public String url;

    private FrameLayout root;

    private ImageButton share;
    private ImageButton download;
    private ImageButton info;

    private BottomSheetLayout bottomSheet;

    private VideoFragment videoFragment;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        getSupportActionBar().hide();

        /*getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );*/

        url = getIntent().getStringExtra("url");

        if (url == null) {
            finish();
            return;
        }

        AppSettings settings = new AppSettings(context);
        Utils.setUpTheme(this, settings);

        /*if (Build.VERSION.SDK_INT > 18 && settings.uiExtras) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        setContentView(R.layout.video_view_activity);

        download = (ImageButton) findViewById(R.id.save_button);
        info = (ImageButton) findViewById(R.id.info_button);
        share = (ImageButton) findViewById(R.id.share_button);

        if (url.contains("youtu")) {
            // add a youtube fragment
            YouTubePlayerFragment fragment = TweetYouTubeFragment.getInstance(context, url);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment, fragment)
                    .commit();

            findViewById(R.id.buttons_layout).setVisibility(View.GONE);
            getSupportActionBar().hide();
        } else {
            // add a surfaceView fragment
            videoFragment = VideoFragment.getInstance(url);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment, videoFragment)
                    .commit();
        }

        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadVideo();
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
                shareVideo();
            }
        });

        final Handler sysUi = new Handler();

        /*findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sysUi.removeCallbacksAndMessages(null);
                if (sysUiShown) {
                    hideSystemUI();
                } else {
                    showSystemUI();
                }
            }
        });*/

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
            ab.setTitle("");
            ab.setIcon(transparent);
        }

        /*sysUi.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideSystemUI();
            }
        }, 6000);*/

        final long tweetId = getIntent().getLongExtra("tweet_id", 0);
        if (tweetId != 0) {
            prepareInfo(tweetId);
        } else {
            findViewById(R.id.buttons_layout).setVisibility(View.GONE);
        }

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if ((velocityY > 3000 || velocityY < -3000) &&
                        (velocityX < 7000 && velocityX > -7000)) {
                    onBackPressed();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void downloadVideo() {
        Calendar calendar = Calendar.getInstance();
        boolean afterAug1 = (calendar.get(Calendar.MONTH) >= Calendar.AUGUST &&
                calendar.get(Calendar.DAY_OF_MONTH) >= 1) ||
                calendar.get(Calendar.YEAR) >= 2017;

        if (videoFragment != null && videoFragment.getLoadedVideoLink().contains(".m3u8") && afterAug1) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/+LukeKlinker/posts/4ZTM55gKVPi"));
            startActivity(browserIntent);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(context.getResources().getString(R.string.downloading) + "...")
                                    .setContentTitle(context.getResources().getString(R.string.app_name))
                                    .setContentText(context.getResources().getString(R.string.saving_video) + "...")
                                    .setProgress(100, 100, true)
                                    .setOngoing(true);

                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());

                    Intent intent = new Intent();
                    if (videoFragment != null) {
                        Uri uri = IOUtils.saveVideo(videoFragment.getLoadedVideoLink());

                        String root = Environment.getExternalStorageDirectory().toString();
                        File myDir = new File(root + "/Talon");
                        File file = new File(myDir, uri.getLastPathSegment());

                        try {
                            uri = IOUtils.getImageContentUri(context, file);
                        } catch (Exception e) {

                        }

                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "surfaceView/*");
                    }

                    PendingIntent pending = PendingIntent.getActivity(context, 91, intent, 0);

                    mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setContentIntent(pending)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(context.getResources().getString(R.string.saved_video) + "...")
                                    .setContentTitle(context.getResources().getString(R.string.app_name))
                                    .setContentText(context.getResources().getString(R.string.saved_video) + "!");

                    mNotificationManager.notify(6, mBuilder.build());
                } catch (final Exception e) {
                    e.printStackTrace();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                new PermissionModelUtils(context).showStorageIssue(e);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(context.getResources().getString(R.string.error) + "...")
                                    .setContentTitle(context.getResources().getString(R.string.app_name))
                                    .setContentText(context.getResources().getString(R.string.error) + "...")
                                    .setProgress(0, 100, true);

                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());
                }
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return true;
        }
    }

    private void shareVideo() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");

        if (videoFragment != null) {
            share.putExtra(Intent.EXTRA_TEXT, videoFragment.getLoadedVideoLink());
        } else {
            share.putExtra(Intent.EXTRA_TEXT, url);
        }

        context.startActivity(share);
    }

    public void hideSystemUI() {
        /*sysUiShown = false;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        if (videoFragment != null && !videoFragment.isGif()) {
            // we don't want to hide the buttons
        } else {
            if (url != null && !url.contains("youtu"))
                startAlphaAnimation(findViewById(R.id.buttons_layout), 1, 0);
            startAlphaAnimation(share, 1, 0);
            startAlphaAnimation(download, 1, 0);
            startAlphaAnimation(info, 1, 0);
        }*/
    }

    boolean sysUiShown = true;
    public void showSystemUI() {
        /*sysUiShown = true;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (url != null && !url.contains("youtu"))
            startAlphaAnimation(findViewById(R.id.buttons_layout), 0, 1);
        startAlphaAnimation(share, 0, 1);
        startAlphaAnimation(download, 0, 1);
        startAlphaAnimation(info, 0, 1);*/
    }

    private DetailedTweetView tweetView;

    public void prepareInfo(final long tweetId) {
        tweetView = DetailedTweetView.create(context, tweetId);
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
                v.setVisibility(View.VISIBLE);
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //findViewById(R.id.buttons_layout).getLayoutParams().height = 0;
            findViewById(R.id.buttons_layout).setVisibility(View.GONE);
        } else if (url != null && !url.contains("youtu")) {
            findViewById(R.id.buttons_layout).setVisibility(View.VISIBLE);
        }
    }

    private GestureDetector gestureDetector;
}