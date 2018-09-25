package com.klinker.android.twitter_l.activities.media_viewer;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activities.tweet_viewer.VideoFragment;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.IOUtils;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;
import com.klinker.android.twitter_l.utils.WebIntentBuilder;
import com.klinker.android.twitter_l.views.DetailedTweetView;

import java.io.File;

import xyz.klinker.android.drag_dismiss.DragDismissIntentBuilder;
import xyz.klinker.android.drag_dismiss.activity.DragDismissActivity;

public class VideoViewerActivity extends DragDismissActivity {

    public static boolean IS_RUNNING = false;

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
                    if (s.contains("youtu") && (gifVideo == null || gifVideo.isEmpty() || gifVideo.equals("no gif surfaceView"))) {
                        video = s;
                        break;
                    }
                }
            }

            if (video == null) {
                video = gifVideo;
            }

            if (video == null) {
                video = "";
            }

            video = video.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4");

            Log.v("video_url", video);

            Intent viewVideo;

            if (video.contains("youtu")) {
                viewVideo = new Intent(context, YouTubeViewerActivity.class);
            } else {
                viewVideo = new Intent(context, VideoViewerActivity.class);
            }

            viewVideo.putExtra("url", video);
            viewVideo.putExtra("tweet_id", tweetId);

            new DragDismissIntentBuilder(context)
                    .setDragElasticity(DragDismissIntentBuilder.DragElasticity.LARGE)
                    .setPrimaryColorResource(android.R.color.black)
                    .setShouldScrollToolbar(false)
                    .setFullscreenOnTablets(true)
                    .setShowToolbar(false)
                    .setDrawUnderStatusBar(true)
                    .build(viewVideo);

            context.startActivity(viewVideo);
        }
    }

    public Context context;
    public String url;

    private BottomSheetLayout bottomSheet;
    private VideoFragment videoFragment;

    @Override
    public View onCreateContent(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        context = this;

        url = getIntent().getStringExtra("url");

        if (url == null) {
            finish();
            return new View(context);
        }

        findViewById(R.id.dragdismiss_status_bar).setVisibility(View.GONE);

        AppSettings settings = new AppSettings(context);
        Utils.setUpTheme(this, settings);

        final View root = inflater.inflate(R.layout.video_view_activity, parent, false);
        prepareToolbar(root);

        videoFragment = VideoFragment.getInstance(url);

        // add a surfaceView fragment
        if (!IS_RUNNING) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment, videoFragment)
                    .commit();
            IS_RUNNING = true;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                IS_RUNNING = false;
            }
        }, 3000);

        bottomSheet = (BottomSheetLayout) root.findViewById(R.id.bottomsheet);

        final long tweetId = getIntent().getLongExtra("tweet_id", 0);
        if (tweetId != 0) {
            prepareInfo(tweetId);
        }

        return root;
    }

    private void prepareToolbar(View root) {
        Toolbar toolbar = (Toolbar) root.findViewById(R.id.toolbar);
        toolbar.setTitle("");
        ((FrameLayout.LayoutParams) toolbar.getLayoutParams()).topMargin = Utils.getStatusBarHeight(this);

        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.getNavigationIcon().setTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_image_viewer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final long tweetId = getIntent().getLongExtra("tweet_id", 0);
        if (tweetId == 0) {
            menu.getItem(2).setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_info:
                showInfo();
                break;
            case R.id.menu_save:
                downloadVideo();
                break;
            case R.id.menu_share:
                shareVideo();
                break;
            case android.R.id.home:
                onBackPressed();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadVideo() {
        final String videoLink = videoFragment.getArguments().getString("url");
        if (videoFragment != null && videoLink != null && videoLink.contains(".m3u8")) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/+LukeKlinker/posts/4ZTM55gKVPi"));
            startActivity(browserIntent);
            return;
        }

        new TimeoutThread(new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context, NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL)
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
                    if (videoLink != null) {
                        Uri uri = IOUtils.saveVideo(videoLink);

                        String root = Environment.getExternalStorageDirectory().toString();
                        File myDir = new File(root + "/Talon");
                        File file = new File(myDir, uri.getLastPathSegment());

                        try {
                            uri = FileProvider.getUriForFile(context,
                                    BuildConfig.APPLICATION_ID + ".provider", file);
                        } catch (Exception e) {

                        }

                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "surfaceView/*");
                    }

                    PendingIntent pending = PendingIntent.getActivity(context, 91, intent, 0);

                    mBuilder =
                            new NotificationCompat.Builder(context, NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL)
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
                            new NotificationCompat.Builder(context, NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL)
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

    private void shareVideo() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");

        if (videoFragment != null) {
            share.putExtra(Intent.EXTRA_TEXT, videoFragment.getLoadedVideoLink());
        } else {
            share.putExtra(Intent.EXTRA_TEXT, url);
        }

        context.startActivity(Intent.createChooser(share, getString(R.string.menu_share) + ": "));
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
}