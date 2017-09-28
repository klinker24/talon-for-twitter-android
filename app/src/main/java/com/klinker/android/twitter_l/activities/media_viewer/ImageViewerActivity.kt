package com.klinker.android.twitter_l.activities.media_viewer

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.utils.TalonPhotoViewAttacher
import org.jetbrains.annotations.Nullable


class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        supportPostponeEnterTransition()

        setContentView(R.layout.activity_image_viewer)

        val imageLink = getLink(intent)
        val imageView = findViewById<View>(R.id.imageView) as ImageView

        Glide.with(this).load(imageLink).diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<String, GlideDrawable> {
                    override fun onException(e: Exception, model: String, target: Target<GlideDrawable>, isFirstResource: Boolean): Boolean = false
                    override fun onResourceReady(resource: GlideDrawable, model: String, target: Target<GlideDrawable>, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        supportStartPostponedEnterTransition()
                        return false
                    }
                }).into(imageView)

        Handler().postDelayed({ supportStartPostponedEnterTransition() }, 500)
        TalonPhotoViewAttacher(this, imageView)

        imageView.post({
            imageView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            imageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            imageView.invalidate()
        })
    }

    private fun getLink(intent: Intent): String {
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.contains("imgur")) return url.replace("t.jpg", ".jpg")
        if (url.contains("insta")) return url.substring(0, url.length - 1) + "l"

        return url
    }

    companion object {
        val EXTRA_URL = "url"
    }
}