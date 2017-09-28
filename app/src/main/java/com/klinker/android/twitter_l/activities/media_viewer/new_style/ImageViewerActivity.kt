package com.klinker.android.twitter_l.activities.media_viewer.new_style

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.adapters.ImagePagerAdapter
import com.klinker.android.twitter_l.utils.Utils
import org.jetbrains.annotations.Nullable

class ImageViewerActivity : AppCompatActivity() {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        supportPostponeEnterTransition()

        setContentView(R.layout.activity_image_viewer)
        prepareToolbar()

        val pager = findViewById<View>(R.id.pager) as ViewPager
        pager.adapter = ImagePagerAdapter(supportFragmentManager, intent.getStringArrayExtra(EXTRA_URLS))
        pager.currentItem = intent.getIntExtra(EXTRA_START_INDEX, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun prepareToolbar() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.title = ""
        (toolbar.layoutParams as FrameLayout.LayoutParams).topMargin = Utils.getStatusBarHeight(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.navigationIcon?.setTintList(ColorStateList.valueOf(Color.WHITE))
        }
    }

    companion object {
        private val EXTRA_URLS = "extra_urls"
        private val EXTRA_TWEET_ID = "extra_tweet_id"
        private val EXTRA_START_INDEX = "extra_start_index"

        @JvmOverloads fun startActivity(context: Context, tweetId: Long = -1L, imageView: ImageView? = null, startIndex: Int = 0, vararg links: String) {
            val viewImage = Intent(context, ImageViewerActivity::class.java)
            viewImage.putExtra(EXTRA_URLS, links)
            viewImage.putExtra(EXTRA_START_INDEX, startIndex)
            if (tweetId != -1L) viewImage.putExtra(EXTRA_TWEET_ID, tweetId)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && imageView != null && links.size == 1) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, imageView, "image")
                context.startActivity(viewImage, options.toBundle())
            } else {
                context.startActivity(viewImage)
            }
        }
    }
}