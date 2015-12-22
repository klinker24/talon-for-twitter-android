package com.klinker.android.twitter_l.manipulations.photo_viewer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Transition;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.widget.Toast;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.DetailedTweetView;
import com.klinker.android.twitter_l.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter_l.manipulations.widgets.HoloTextView;
import com.klinker.android.twitter_l.manipulations.widgets.NetworkedCacheableImageView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.tweet_viewer.TweetYouTubeFragment;
import com.klinker.android.twitter_l.ui.tweet_viewer.VideoFragment;
import com.klinker.android.twitter_l.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.Utils;
import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import uk.co.senab.photoview.PhotoViewAttacher;

public class VideoViewerActivity extends AppCompatActivity {

    // link string can either be a single link to a gif video, or it can be all of the links in the tweet
    // and it will find the youtube one.
    public static void startActivity(Context context, long tweetId, String gifVideo, String linkString) {

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

        viewVideo.putExtra("url", video);
        viewVideo.putExtra("tweet_id", tweetId);

        context.startActivity(viewVideo);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        try {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {
            e.printStackTrace();
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        url = getIntent().getStringExtra("url");

        if (url == null) {
            finish();
            return;
        }

        AppSettings settings = new AppSettings(context);
        Utils.setUpTheme(this, settings);

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
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

            ((View)download.getParent().getParent()).setVisibility(View.GONE);
            getSupportActionBar().hide();
        } else {
            // add a video fragment
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

        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sysUi.removeCallbacksAndMessages(null);
                if (sysUiShown) {
                    hideSystemUI();
                } else {
                    showSystemUI();
                }
            }
        });

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
            ab.setTitle("");
            ab.setIcon(transparent);
        }

        sysUi.postDelayed(new Runnable() {
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
    }

    private void downloadVideo() {
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

                    Uri uri = IOUtils.saveVideo(videoFragment.getLoadedVideoLink());
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "video/*");

                    PendingIntent pending = PendingIntent.getActivity(context, 91, intent, 0);

                    mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setContentIntent(pending)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(context.getResources().getString(R.string.saved_video) + "...")
                                    .setContentTitle(context.getResources().getString(R.string.app_name))
                                    .setContentText(context.getResources().getString(R.string.saved_video) + "!");

                    mNotificationManager.notify(6, mBuilder.build());
                } catch (Exception e) {
                    e.printStackTrace();

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                new PermissionModelUtils(context).showStorageIssue();
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
        share.putExtra(Intent.EXTRA_TEXT, videoFragment.getLoadedVideoLink());

        context.startActivity(share);
    }

    private void hideSystemUI() {
        sysUiShown = false;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        startAlphaAnimation(share, 1, 0);
        startAlphaAnimation(download, 1, 0);
        startAlphaAnimation(info, 1, 0);
    }

    boolean sysUiShown = true;
    private void showSystemUI() {
        sysUiShown = true;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        startAlphaAnimation(share, 0, 1);
        startAlphaAnimation(download, 0, 1);
        startAlphaAnimation(info, 0, 1);
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
}