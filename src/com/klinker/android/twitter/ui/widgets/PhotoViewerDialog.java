package com.klinker.android.twitter.ui.widgets;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoViewerDialog extends Activity {

    public Context context;
    public HoloEditText text;
    public ListView list;
    public String url;
    public NetworkedCacheableImageView picture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        url = getIntent().getStringExtra("url");

        if (url.contains("twitpic")) {
            url = url.replace("thumb", "full");
        }

        boolean fromCache = getIntent().getBooleanExtra("from_cache", true);
        boolean doRestart = getIntent().getBooleanExtra("restart", true);

        AppSettings settings = new AppSettings(context);

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        setContentView(R.layout.photo_dialog_layout);

        if (url == null) {
            finish();
            return;
        }

        if (url.contains("insta")) {
            url = url.substring(0, url.length() - 1) + "l";
        }

        picture = (NetworkedCacheableImageView) findViewById(R.id.picture);
        PhotoViewAttacher mAttacher = new PhotoViewAttacher(picture);

        picture.loadImage(url, false, doRestart ? new NetworkedCacheableImageView.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(CacheableBitmapDrawable result) {
                if (isRunning) {
                    overridePendingTransition(0,0);
                    finish();
                    Intent restart = new Intent(context, PhotoViewerDialog.class);
                    restart.putExtra("url", url);
                    restart.putExtra("from_cache", true);
                    restart.putExtra("restart", false);
                    overridePendingTransition(0,0);
                    startActivity(restart);
                }
            }
        } : null, 0, fromCache); // no transform

        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                ((Activity)context).finish();
            }
        });

        HoloTextView download = (HoloTextView) findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                            .setProgress(100, 100, true)
                                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_save));

                            NotificationManager mNotificationManager =
                                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(6, mBuilder.build());

                            URL mUrl = new URL(url);

                            Bitmap bitmap = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());

                            Random generator = new Random();
                            int n = 1000000;
                            n = generator.nextInt(n);
                            String fname = "Image-" + n;

                            IOUtils.saveImage(bitmap, fname, context);
                            mBuilder =
                                    new NotificationCompat.Builder(context)
                                            .setSmallIcon(R.drawable.ic_stat_icon)
                                            .setTicker(getResources().getString(R.string.saved_picture) + "...")
                                            .setContentTitle(getResources().getString(R.string.app_name))
                                            .setContentText(getResources().getString(R.string.saved_picture) + "!")
                                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_save));

                            mNotificationManager.notify(6, mBuilder.build());
                        } catch (Exception e) {
                            NotificationCompat.Builder mBuilder =
                                    new NotificationCompat.Builder(context)
                                            .setSmallIcon(R.drawable.ic_stat_icon)
                                            .setTicker(getResources().getString(R.string.error) + "...")
                                            .setContentTitle(getResources().getString(R.string.app_name))
                                            .setContentText(getResources().getString(R.string.error) + "...")
                                            .setProgress(100, 100, true)
                                            .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_action_save));

                            NotificationManager mNotificationManager =
                                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(6, mBuilder.build());
                        }
                    }
                }).start();

                finish();
            }
        });
    }

    private class DownloadPic extends AsyncTask<String, Void, Bitmap> {


        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                Log.d("ImageUrlAsyncTask", "Downloading: " + url);

                // The bitmap isn't cached so download from the web
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                InputStream is = new BufferedInputStream(conn.getInputStream());

                Bitmap b = BitmapFactory.decodeStream(is);

                return b;

            } catch (IOException e) {
                Log.e("ImageUrlAsyncTask", e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);

            if (result != null) {

            }
        }
    }

    public boolean isRunning = true;

    @Override
    public void onResume() {
        super.onResume();
        isRunning = true;
    }

    @Override
    public void onPause() {
        isRunning = false;
        super.onPause();
    }
}