package com.klinker.android.twitter_l.activities.media_viewer.image

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.core.app.ActivityOptionsCompat
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.widget.Toolbar
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
import kotlin.collections.HashMap
import twitter4j.Status

@SuppressLint("InlinedApi")
class ImageViewerActivity : AppCompatActivity(), TweetView.TweetLoaded {

    private val pager: ViewPager by lazy { findViewById<View>(R.id.pager) as ViewPager }
    private val adapter: ImagePagerAdapter by lazy { ImagePagerAdapter(supportFragmentManager, intent.getStringArrayExtra(EXTRA_URLS)) }

    private val tweetIds: LongArray by lazy { intent.getLongArrayExtra(EXTRA_TWEET_IDS) }

    private val tweetViews: MutableMap<Long, DetailedTweetView> by lazy { HashMap<Long, DetailedTweetView>() }
    private val toolbarMenu: Menu by lazy { findViewById<Toolbar>(R.id.toolbar).menu }

    private val bottomSheet: BottomSheetLayout by lazy { findViewById<View>(R.id.bottom_sheet) as BottomSheetLayout }

    private val createdTime = System.currentTimeMillis()

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Utils.isAndroidO()) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        supportPostponeEnterTransition()

        setContentView(R.layout.activity_image_viewer)
        prepareToolbar()

        pager.adapter = adapter
        pager.currentItem = intent.getIntExtra(EXTRA_START_INDEX, 0)

        val initTweetId = tweetIds[pager.currentItem]

        if (initTweetId != -1L){
            val tweetView = tweetViews.getOrPut(initTweetId) { DetailedTweetView.create(this, initTweetId).setTweetLoadedCallback(this) as DetailedTweetView }
            tweetView.setShouldShowImage(false)
        }

        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val tweetId = tweetIds[position]

                if (tweetId == -1L) {
                    findViewById<View>(R.id.show_info).visibility = View.GONE
                    toolbarMenu.getItem(2).isVisible = false

                } else {

                    val tweetView = tweetViews.getOrPut(tweetId) {
                        val tv = DetailedTweetView.create(this@ImageViewerActivity, tweetId) as DetailedTweetView
                        tv.setShouldShowImage(false)
                        tv
                    }

                    val switchTime = System.currentTimeMillis()

                    val timeout = if (System.currentTimeMillis() - switchTime > TIME_TO_DISPLAY_COUNT) {
                        0L
                    } else {
                        TIME_TO_DISPLAY_COUNT - (System.currentTimeMillis() - switchTime)
                    }

                    Handler().postDelayed({
                        val status = tweetView.status
                        toolbarMenu.getItem(2).isVisible = status != null

                        if (status != null) {

                            findViewById<View>(R.id.show_info).visibility = View.VISIBLE

                            val retweetCount = findViewById<TextView>(R.id.retweet_count)
                            val likeCount = findViewById<TextView>(R.id.like_count)

                            retweetCount.text = status.retweetCount.toString()
                            likeCount.text = status.favoriteCount.toString()

                        } else {

                            findViewById<View>(R.id.show_info).visibility = View.GONE

                        }
                    }, timeout)

                }

            }
        })

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.getItem(2).isVisible = tweetIds[pager.currentItem] != -1L
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> adapter.getItem(pager.currentItem).downloadImage()
            R.id.menu_share -> adapter.getItem(pager.currentItem).shareImage()
            R.id.menu_info -> showInfo()
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onLoaded(status: Status?) {

        if (tweetIds[pager.currentItem] != -1L && status != null) {

            val timeout = if (System.currentTimeMillis() - createdTime > TIME_TO_DISPLAY_COUNT) {
                0L
            } else {
                TIME_TO_DISPLAY_COUNT - (System.currentTimeMillis() - createdTime)
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
        val tweetId = tweetIds[pager.currentItem]
        val tweetView = tweetViews.getOrPut(tweetId) { DetailedTweetView.create(this, tweetId).setTweetLoadedCallback(this) as DetailedTweetView }
        tweetView.view.setBackgroundResource(R.color.dark_background)
        bottomSheet.showWithSheetView(tweetView.view)
    }

    fun hasMultipleImages(): Boolean {
        return intent.getStringArrayExtra(EXTRA_URLS).size > 1
    }

    companion object {
        private val EXTRA_URLS = "extra_urls"
        private val EXTRA_TWEET_IDS = "extra_tweet_ids"
        private val EXTRA_START_INDEX = "extra_start_index"
        private val TIME_TO_DISPLAY_COUNT = 1500L

        @JvmOverloads fun startActivity(context: Context?, tweetId: Long = -1L, imageView: ImageView? = null, startIndex: Int = 0, vararg links : String) {

            if (context == null) {
                return
            }

            val viewImage = Intent(context, ImageViewerActivity::class.java)

            viewImage.putExtra(EXTRA_TWEET_IDS, LongArray(links.size) {tweetId})
                    .putExtra(EXTRA_URLS, links)
                    .putExtra(EXTRA_START_INDEX, startIndex)


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && imageView != null && links.size == 1) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, imageView, "image")
                context.startActivity(viewImage, options.toBundle())
            } else {
                context.startActivity(viewImage)
            }

        }

        @JvmOverloads fun startActivity(context: Context?, imageView: ImageView? = null, startIndex: Int = 0, linksWithIds: List<Pair<String, Long>>) {

            if (context == null) {
                return
            }


            val viewImage = Intent(context, ImageViewerActivity::class.java)
                    .putExtra(EXTRA_START_INDEX, startIndex)
                    .putExtra(EXTRA_URLS, linksWithIds.map { it.first }.toTypedArray())
                    .putExtra(EXTRA_TWEET_IDS, LongArray(linksWithIds.size) { x -> linksWithIds[x].second })


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && imageView != null && linksWithIds.size == 1) {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity, imageView, "image")
                context.startActivity(viewImage, options.toBundle())
            } else {
                context.startActivity(viewImage)
            }

        }

    }
}
