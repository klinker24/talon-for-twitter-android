package com.klinker.android.twitter_l.activities.media_viewer

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.utils.TalonPhotoViewAttacher
import com.klinker.android.twitter_l.utils.Utils
import org.jetbrains.annotations.Nullable
import uk.co.senab.photoview.PhotoViewAttacher


class ImageViewerActivity : AppCompatActivity() {

    var view: View? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val imageLink = intent.getStringExtra("url")
        val imageView = findViewById<View>(R.id.imageView) as ImageView

        Glide.with(this).load(imageLink).diskCacheStrategy(DiskCacheStrategy.SOURCE).into(imageView)

        TalonPhotoViewAttacher(this, imageView)
    }
}