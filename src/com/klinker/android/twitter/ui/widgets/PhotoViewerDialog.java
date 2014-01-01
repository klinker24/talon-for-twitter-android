package com.klinker.android.twitter.ui.widgets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.klinker.android.twitter.R;
import com.klinker.android.twitter.manipulations.NetworkedCacheableImageView;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.utils.IOUtils;

import java.net.URL;
import java.util.Random;

import uk.co.senab.bitmapcache.CacheableBitmapDrawable;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoViewerDialog extends Activity {

    public Context context;
    public HoloEditText text;
    public ListView list;
    public String url;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        url = getIntent().getStringExtra("url");
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

        NetworkedCacheableImageView picture = (NetworkedCacheableImageView) findViewById(R.id.picture);
        PhotoViewAttacher mAttacher = new PhotoViewAttacher(picture);

        picture.loadImage(url, false, doRestart ? new NetworkedCacheableImageView.OnImageLoadedListener() {
            @Override
            public void onImageLoaded(CacheableBitmapDrawable result) {
                overridePendingTransition(0,0);
                finish();
                Intent restart = new Intent(context, PhotoViewerDialog.class);
                restart.putExtra("url", url);
                restart.putExtra("from_cache", true);
                restart.putExtra("restart", false);
                overridePendingTransition(0,0);
                startActivity(restart);
            }
        } : null, 0, fromCache); // no transform

        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                ((Activity)context).finish();
            }
        });

        ImageButton download = (ImageButton) findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, getResources().getString(R.string.saving_picture), Toast.LENGTH_SHORT).show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();

                        try {
                            URL mUrl = new URL(url);

                            Bitmap bitmap = BitmapFactory.decodeStream(mUrl.openConnection().getInputStream());

                            Random generator = new Random();
                            int n = 1000000;
                            n = generator.nextInt(n);
                            String fname = "Image-" + n;

                            IOUtils.saveImage(bitmap, fname, context);

                            Toast.makeText(context, getResources().getString(R.string.saved_picture), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(context, context.getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
                        }
                    }
                }).start();

                finish();
            }
        });
    }

}