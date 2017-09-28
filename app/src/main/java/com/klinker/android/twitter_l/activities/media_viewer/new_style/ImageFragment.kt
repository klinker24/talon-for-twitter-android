package com.klinker.android.twitter_l.activities.media_viewer.new_style

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.utils.TalonPhotoViewAttacher

class ImageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val root = inflater.inflate(R.layout.fragment_image, container, false)

        val imageLink = getLink(arguments)
        val imageView = root.findViewById<View>(R.id.imageView) as ImageView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && arguments.getInt(EXTRA_INDEX, 0) != 0) {
            imageView.transitionName = ""
        }

        Glide.with(this).load(imageLink).diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<String, GlideDrawable> {
                    override fun onException(e: Exception, model: String, target: Target<GlideDrawable>, isFirstResource: Boolean): Boolean = false
                    override fun onResourceReady(resource: GlideDrawable, model: String, target: Target<GlideDrawable>, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        activity.supportStartPostponedEnterTransition()
                        return false
                    }
                }).into(imageView)

        Handler().postDelayed({ activity.supportStartPostponedEnterTransition() }, 500)
        TalonPhotoViewAttacher(activity as AppCompatActivity, imageView).toolbar = activity.findViewById<View>(R.id.toolbar) as Toolbar

        imageView.post({
            imageView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            imageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            imageView.invalidate()
        })

        return root
    }

    companion object {
        private val EXTRA_URL = "extra_url"
        private val EXTRA_INDEX = "extra_index"

        fun getInstance(index: Int, imageLink: String): ImageFragment {
            val b = Bundle()
            b.putString(EXTRA_URL, imageLink)
            b.putInt(EXTRA_INDEX, index)

            val fragment = ImageFragment()
            fragment.arguments = b

            return fragment
        }

        private fun getLink(args: Bundle): String {
            val url = args.getString(EXTRA_URL)
            if (url.contains("imgur")) return url.replace("t.jpg", ".jpg")
            if (url.contains("insta")) return url.substring(0, url.length - 1) + "l"

            return url
        }
    }
}