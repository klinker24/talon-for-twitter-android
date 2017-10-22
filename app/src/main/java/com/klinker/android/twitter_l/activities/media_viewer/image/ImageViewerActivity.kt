package com.klinker.android.twitter_l.activities.media_viewer.image

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.adapters.ImagePagerAdapter
import com.klinker.android.twitter_l.utils.Utils
import org.jetbrains.annotations.Nullable
import com.klinker.android.twitter_l.views.DetailedTweetView
import com.flipboard.bottomsheet.BottomSheetLayout
import com.klinker.android.twitter_l.views.TweetView
import twitter4j.Status

class ImageViewerActivity : AppCompatActivity(), TweetView.TweetLoaded {

    private val pager: ViewPager by lazy { findViewById<View>(R.id.pager) as ViewPager }
    private val adapter: ImagePagerAdapter by lazy { ImagePagerAdapter(supportFragmentManager, intent.getStringArrayExtra(EXTRA_URLS)) }

    private val tweetId: Long by lazy { intent.getLongExtra(EXTRA_TWEET_ID, -1L) }
    private val tweetView: DetailedTweetView by lazy { DetailedTweetView.create(this, tweetId).setTweetLoadedCallback(this) as DetailedTweetView }
    private val bottomSheet: BottomSheetLayout by lazy { findViewById<View>(R.id.bottom_sheet) as BottomSheetLayout }

    private val createdTime = System.currentTimeMillis()

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        supportPostponeEnterTransition()

        setContentView(R.layout.activity_image_viewer)
        prepareToolbar()

        pager.adapter = adapter
        pager.currentItem = intent.getIntExtra(EXTRA_START_INDEX, 0)

        if (tweetId != -1L) {
            tweetView.setShouldShowImage(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.getItem(2).isVisible = false
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> adapter.getItem(pager.currentItem).downloadImage()
            R.id.menu_share -> adapter.getItem(pager.currentItem).shareImage()
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onLoaded(status: Status?) {
        if (tweetId != -1L && status != null) {

            val timeout = if (System.currentTimeMillis() - createdTime > TIME_TO_DISPLAY_COUNT) {
                0
            } else {
                System.currentTimeMillis() - createdTime
            }

            Handler().postDelayed({
                findViewById<View>(R.id.show_info).setOnClickListener { showInfo() }
                findViewById<View>(R.id.show_info).visibility = View.VISIBLE

                val retweetCount = findViewById<View>(R.id.retweet_count) as TextView
                val likeCount = findViewById<View>(R.id.like_count) as TextView

                retweetCount.text = status.retweetCount.toString()
                likeCount.text = status.favoriteCount.toString()
            }, timeout)
        }
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

    private fun showInfo() {
        tweetView.view.setBackgroundResource(R.color.dark_background)
        bottomSheet.showWithSheetView(tweetView.view)
    }

    fun hasMultipleImages(): Boolean {
        return intent.getStringArrayExtra(EXTRA_URLS).size > 1
    }

    companion object {
        private val EXTRA_URLS = "extra_urls"
        private val EXTRA_TWEET_ID = "extra_tweet_id"
        private val EXTRA_START_INDEX = "extra_start_index"
        private val TIME_TO_DISPLAY_COUNT = 1000

        @JvmOverloads fun startActivity(context: Context?, tweetId: Long = -1L, imageView: ImageView? = null, startIndex: Int = 0, vararg links: String) {
            if (context == null) {
                return
            }

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