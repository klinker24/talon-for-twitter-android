package com.klinker.android.twitter_l.activities.media_viewer;

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
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.klinker.android.twitter_l.BuildConfig;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.utils.TimeoutThread;
import com.klinker.android.twitter_l.views.DetailedTweetView;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.activities.tweet_viewer.TweetYouTubeFragment;
import com.klinker.android.twitter_l.activities.tweet_viewer.VideoFragment;
import com.klinker.android.twitter_l.utils.IOUtils;

import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.VideoMatcherUtil;
import com.klinker.android.twitter_l.utils.WebIntentBuilder;

import java.io.File;
import java.util.Calendar;

import xyz.klinker.android.drag_dismiss.DragDismissBundleBuilder;
import xyz.klinker.android.drag_dismiss.activity.DragDismissActivity;

public class VideoViewerActivity extends DragDismissActivity {

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

            Intent viewVideo = new Intent(context, VideoViewerActivity.class);

            video = video.replace(".png", ".mp4").replace(".jpg", ".mp4").replace(".jpeg", ".mp4");

            viewVideo.putExtra("url", video);
            viewVideo.putExtra("tweet_id", tweetId);

            Log.v("video_url", video);

            new DragDismissBundleBuilder(context)
                    .setShowToolbar(false)
                    .setPrimaryColorResource(android.R.color.black)
                    .build(viewVideo);

            if (video != null) {
                context.startActivity(viewVideo);
            }
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
    protected View onCreateContent(LayoutInflater inflater, ViewGroup parent) {
        context = this;

        url = getIntent().getStringExtra("url");

        if (url == null) {
            finish();
            return new View(this);
        }

        AppSettings settings = new AppSettings(context);
        Utils.setUpTheme(this, settings);

        final View root = inflater.inflate(R.layout.video_view_activity, parent, false);

        download = (ImageButton) root.findViewById(R.id.save_button);
        info = (ImageButton) root.findViewById(R.id.info_button);
        share = (ImageButton) root.findViewById(R.id.share_button);

        if (url.contains("youtu")) {
            // add a youtube fragment
            YouTubePlayerFragment fragment = TweetYouTubeFragment.getInstance(context, url);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment, fragment)
                    .commit();

            root.findViewById(R.id.buttons_layout).setVisibility(View.GONE);
            getSupportActionBar().hide();
        } else {
            // add a surfaceView fragment
            videoFragment = VideoFragment.getInstance(url);
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment, videoFragment)
                    .commit();
        }

        bottomSheet = (BottomSheetLayout) root.findViewById(R.id.bottomsheet);

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

        android.support.v7.app.ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ColorDrawable transparent = new ColorDrawable(getResources().getColor(android.R.color.transparent));
            ab.setBackgroundDrawable(transparent);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
            ab.setTitle("");
            ab.setIcon(transparent);
        }

        final long tweetId = getIntent().getLongExtra("tweet_id", 0);
        if (tweetId != 0) {
            prepareInfo(tweetId);
        } else {
            root.findViewById(R.id.buttons_layout).setVisibility(View.GONE);
        }

        return root;
    }

    private void downloadVideo() {
        if (videoFragment != null && videoFragment.getLoadedVideoLink().contains(".m3u8")) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/+LukeKlinker/posts/4ZTM55gKVPi"));
            startActivity(browserIntent);
            return;
        }

        new TimeoutThread(new Runnable() {
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
                            uri = FileProvider.getUriForFile(context,
                                    BuildConfig.APPLICATION_ID + ".provider", file);
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