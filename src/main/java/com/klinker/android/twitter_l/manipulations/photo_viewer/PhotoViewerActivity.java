package com.klinker.android.twitter_l.manipulations.photo_viewer;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.*;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.flipboard.bottomsheet.BottomSheetLayout;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.TimeLineCursorAdapter;
import com.klinker.android.twitter_l.data.DetailedTweetView;
import com.klinker.android.twitter_l.manipulations.widgets.FullScreenImageView;
import com.klinker.android.twitter_l.manipulations.widgets.HoloEditText;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import com.klinker.android.twitter_l.utils.PermissionModelUtils;
import com.klinker.android.twitter_l.utils.Utils;
import com.klinker.android.twitter_l.utils.api_helper.TwitterDMPicHelper;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoViewerActivity extends AppCompatActivity {

    // image view is not null if you want the shared transition
    public static void startActivity(Context context, long tweetId, String link, ImageView imageView) {
        Intent viewImage = new Intent(context, PhotoViewerActivity.class);

        viewImage.putExtra("url", link);
        viewImage.putExtra("tweet_id", tweetId);

        if (imageView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            viewImage.putExtra("shared_trans", true);
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(((Activity)context), imageView, "image");

            context.startActivity(viewImage, options.toBundle());
        } else {
            context.startActivity(viewImage);
        }
    }

    public Context context;
    public HoloEditText text;
    public ListView list;
    public String url;
    public FullScreenImageView picture;
    public TalonPhotoViewAttacher mAttacher;

    private ImageButton share;
    private ImageButton download;
    private ImageButton info;

    private BottomSheetLayout bottomSheet;

    private boolean didTransition = false;

    @Override
    public void finish() {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
        // this is used in the onStart() for the home fragment to tell whether or not it should refresh
        // tweetmarker. Since coming out of this will only call onResume(), it isn't needed.
        //sharedPrefs.edit().putBoolean("from_activity", true).commit();

        super.finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        try {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } catch (Exception e) {
            e.printStackTrace();
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        if (getIntent().getBooleanExtra("share_trans", false)) {
            // todo: take this out? it doesn't actually do anything it seems...
            Utils.setSharedContentTransition(this);
        }

        url = getIntent().getStringExtra("url");

        if (url == null) {
            finish();
            return;
        }

        // get higher quality twitpic and imgur pictures

        if (url.contains("imgur")) {
            url = url.replace("t.jpg", ".jpg");
        }

        boolean fromCache = getIntent().getBooleanExtra("from_cache", true);
        boolean doRestart = getIntent().getBooleanExtra("restart", true);
        final boolean fromLauncher = getIntent().getBooleanExtra("from_launcher", false);

        AppSettings settings = new AppSettings(context);

        Utils.setUpTweetTheme(this, settings);

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        setContentView(R.layout.photo_dialog_layout);

        download = (ImageButton) findViewById(R.id.save_button);
        info = (ImageButton) findViewById(R.id.info_button);
        share = (ImageButton) findViewById(R.id.share_button);

        bottomSheet = (BottomSheetLayout) findViewById(R.id.bottomsheet);

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadImage();
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
                shareImage();
            }
        });

        if (!doRestart || getIntent().getBooleanExtra("config_changed", false)) {
            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }

        if (url == null) {
            finish();
            return;
        }

        if (url.contains("insta")) {
            url = url.substring(0, url.length() - 1) + "l";
        }

        picture = (FullScreenImageView) findViewById(R.id.picture);
        picture.setDisplayType(FullScreenImageView.DisplayType.FIT_TO_SCREEN);

        if (getIntent().getBooleanExtra("shared_trans", false)) {
            picture.setPadding(0,0,0,0);
        }

        PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && powerManager.isPowerSaveMode()) {
            picture.setTransitionName("invalidate");
        }

        // without this, glide didn't work very well, the transition was super jumpy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            supportPostponeEnterTransition();

        final Handler sysUi = new Handler();

        Glide.with(this).load(url).dontAnimate().listener(new RequestListener<String, GlideDrawable>() {
            @Override
            public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                // without this, glide didn't work very well, the transition was super jumpy
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !didTransition) {
                    supportStartPostponedEnterTransition();
                    didTransition = true;
                }

                LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
                spinner.setVisibility(View.GONE);

                mAttacher = new TalonPhotoViewAttacher(picture);
                mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
                    @Override
                    public void onViewTap(View view, float x, float y) {
                        if (sysUiShown) {
                            hideSystemUI();
                        } else {
                            showSystemUI();
                        }
                    }
                });


                return false;
            }
        }).diskCacheStrategy(DiskCacheStrategy.ALL).into(picture);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // without this, glide didn't work very well, the transition was super jumpy
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !didTransition) {
                    supportStartPostponedEnterTransition();
                    didTransition = true;
                }
            }
        }, 500);

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

        showDialogAboutSwiping();

        final long tweetId = getIntent().getLongExtra("tweet_id", 0);
        if (tweetId != 0) {
            prepareInfo(tweetId);
        } else {
            ((View)info.getParent()).setVisibility(View.GONE);
        }
    }

    public void downloadImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                try {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(getResources().getString(R.string.downloading) + "...")
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText(getResources().getString(R.string.saving_picture) + "...")
                                    .setProgress(100, 100, true);

                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());

                    Bitmap bitmap;

                    if (url.contains("ton.twitter.com") || url.contains("twitter.com/messages/")) {
                        // it is a direct message picture
                        TwitterDMPicHelper helper = new TwitterDMPicHelper();
                        bitmap = helper.getDMPicture(url, Utils.getTwitter(context, AppSettings.getInstance(context)), context);
                    } else {
                        URL mUrl = new URL(url);

                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        InputStream is = new BufferedInputStream(conn.getInputStream());

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = false;

                        bitmap = decodeSampledBitmapFromResourceMemOpt(is, 600, 600);
                    }

                    Random generator = new Random();
                    int n = 1000000;
                    n = generator.nextInt(n);
                    String fname = "Image-" + n;

                    Uri uri = IOUtils.saveImage(bitmap, fname, context);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "image/*");

                    PendingIntent pending = PendingIntent.getActivity(context, 91, intent, 0);

                    mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setContentIntent(pending)
                                    .setSmallIcon(R.drawable.ic_stat_icon)
                                    .setTicker(getResources().getString(R.string.saved_picture) + "...")
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText(getResources().getString(R.string.saved_picture) + "!");

                    mNotificationManager.notify(6, mBuilder.build());
                } catch (final Exception e) {
                    ((Activity) context).runOnUiThread(new Runnable() {
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
                                    .setTicker(getResources().getString(R.string.error) + "...")
                                    .setContentTitle(getResources().getString(R.string.app_name))
                                    .setContentText(getResources().getString(R.string.error) + "...")
                                    .setProgress(0, 100, true);

                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(6, mBuilder.build());
                }
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (mAttacher != null) {
            mAttacher.cleanup();
        }

        super.onBackPressed();
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

    private void shareImage() {
        // get the bitmap
        if (picture == null) {
            return;
        }

        if (picture.getDrawable() == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap bitmap = Glide.with(PhotoViewerActivity.this)
                            .load(url)
                            .asBitmap()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // create the intent
                            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                            sharingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            sharingIntent.setType("image/*");

                            // add the bitmap uri to the intent
                            Uri uri = getImageUri(PhotoViewerActivity.this, bitmap);
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);

                            // start the chooser
                            startActivity(Intent.createChooser(sharingIntent, getString(R.string.menu_share) + ": "));
                        }
                    });
                } catch (Exception e) {

                }
            }
        }).start();
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory() + "/talon/image_to_share.jpg");
        try {
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());

            return IOUtils.getImageContentUri(inContext, f);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap decodeSampledBitmapFromResourceMemOpt(
            InputStream inputStream, int reqWidth, int reqHeight) {

        byte[] byteArr = new byte[0];
        byte[] buffer = new byte[1024];
        int len;
        int count = 0;

        try {
            while ((len = inputStream.read(buffer)) > -1) {
                if (len != 0) {
                    if (count + len > byteArr.length) {
                        byte[] newbuf = new byte[(count + len) * 2];
                        System.arraycopy(byteArr, 0, newbuf, 0, count);
                        byteArr = newbuf;
                    }

                    System.arraycopy(buffer, 0, byteArr, count, len);
                    count += len;
                }
            }

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(byteArr, 0, count, options);

            options.inSampleSize = calculateInSampleSize(options, reqWidth,
                    reqHeight);
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            return BitmapFactory.decodeByteArray(byteArr, 0, count, options);

        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options opt, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = opt.outHeight;
        final int width = opt.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public boolean isRunning = true;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LinearLayout spinner = (LinearLayout) findViewById(R.id.list_progress);
            spinner.setVisibility(View.GONE);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        isRunning = true;

        registerReceiver(receiver, new IntentFilter("com.klinker.android.twitter.IMAGE_LOADED"));
    }

    @Override
    public void onPause() {
        isRunning = false;

        unregisterReceiver(receiver);
        super.onPause();
    }


    private void hideSystemUI() {
        sysUiShown = false;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        startAlphaAnimation(findViewById(R.id.buttons_layout), 1, 0);
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

        startAlphaAnimation(findViewById(R.id.buttons_layout), 0, 1);
        startAlphaAnimation(share, 0, 1);
        startAlphaAnimation(download, 0, 1);
        startAlphaAnimation(info, 0, 1);
    }

    private void showDialogAboutSwiping() {
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPrefs.getBoolean("show_swipe_dialog", true)) {
            new AlertDialog.Builder(this)
                    .setTitle("Tip:")
                    .setMessage("You can close the photo viewer by swiping up or down on the picture!")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sharedPrefs.edit().putBoolean("show_swipe_dialog", false).commit();
                        }
                    })
                    .show();
        }
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