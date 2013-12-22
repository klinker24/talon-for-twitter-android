package com.klinker.android.talon.ui.widgets;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.klinker.android.talon.R;
import com.klinker.android.talon.manipulations.NetworkedCacheableImageView;
import com.klinker.android.talon.settings.AppSettings;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoViewerDialog extends Activity {

    public Context context;
    private View mDialogView;
    public HoloEditText text;
    public ListView list;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        String url = getIntent().getStringExtra("url");
        boolean fromCache = getIntent().getBooleanExtra("from_cache", true);

        AppSettings settings = new AppSettings(context);

        if (Build.VERSION.SDK_INT > 18 && settings.uiExtras) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        setContentView(R.layout.photo_dialog_layout);

        NetworkedCacheableImageView picture = (NetworkedCacheableImageView) findViewById(R.id.picture);
        picture.loadImage(url, false, null, 0, fromCache); // no transform

        PhotoViewAttacher mAttacher = new PhotoViewAttacher(picture);

        mAttacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                ((Activity)context).finish();
            }
        });
    }

}