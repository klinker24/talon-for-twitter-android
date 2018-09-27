package com.klinker.android.twitter_l.views

import android.content.Context
import android.content.Intent
import androidx.cardview.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.activities.BrowserActivity
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity
import com.klinker.android.twitter_l.data.WebPreview
import com.klinker.android.twitter_l.settings.AppSettings
import com.klinker.android.twitter_l.utils.WebIntentBuilder
import com.klinker.android.twitter_l.utils.api_helper.ArticleParserHelper
import com.klinker.android.twitter_l.utils.text.TouchableSpan

class WebPreviewCard @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    interface OnLoad {
        fun onLinkLoaded(link: String, preview: WebPreview)
    }

    private var loadedPreview: WebPreview? = null

    private val progress: ProgressBar by lazy { findViewById<View>(R.id.progress_bar) as ProgressBar }
    private val blankImage: ImageView by lazy { findViewById<View>(R.id.blank_image) as ImageView }
    private val image: ImageView by lazy { findViewById<View>(R.id.web_image) as ImageView }
    private val title: TextView by lazy { findViewById<View>(R.id.web_title) as TextView }
    private val summary: TextView by lazy { findViewById<View>(R.id.web_summary) as TextView }

    init {
        LayoutInflater.from(context).inflate(R.layout.card_web_preview, this, true)

        title.textSize = AppSettings.getInstance(context).textSize + 3.toFloat()
        summary.textSize = AppSettings.getInstance(context).textSize.toFloat()

        setOnClickListener { }
        setOnLongClickListener { true }
    }

    fun loadLink(link: String, listener: OnLoad) {
        tag = link

        if (loadedPreview != null) {
            displayPreview(loadedPreview!!)
            return
        }

        progress.visibility = View.VISIBLE

        ArticleParserHelper.getArticle(link) { webPreview ->
            listener.onLinkLoaded(link, webPreview)
            displayPreview(webPreview)
        }
    }

    fun displayPreview(preview: WebPreview) {
        if (tag != preview.link) {
//            if (loadedPreview != null) {
//                tag = loadedPreview!!.link
//                displayPreview(loadedPreview!!)
//            }

            return
        }

        loadedPreview = preview

        if (preview.imageUrl.isBlank()) {
            blankImage.visibility = View.VISIBLE
            image.visibility = View.GONE
        } else {
            blankImage.visibility = View.GONE
            image.visibility = View.VISIBLE

            try {
                Glide.with(context).load(preview.imageUrl).into(image)
            } catch (e: IllegalArgumentException) {
                // destroyed activity
            }
        }

        progress.visibility = View.GONE

        if (preview.title.isNotBlank()) {
            if (title.visibility != View.VISIBLE) title.visibility = View.VISIBLE
            title.text = preview.title
        } else {
            title.visibility = View.GONE
        }

        if (preview.summary.isNotBlank()) {
            summary.text = preview.summary
        } else {
            if (preview.webDomain.isNotBlank()) summary.text = preview.webDomain
            else summary.visibility = View.GONE
        }

        val link = preview.link
        setOnClickListener {
            if (link.contains("/i/web/status/") || link.contains("twitter.com") && link.contains("/moments/")) {
                val browser = Intent(context, BrowserActivity::class.java)
                browser.putExtra("url", link)
                context.startActivity(browser)
            } else if (link.contains("vine.co/v/")) {
                VideoViewerActivity.startActivity(context, 0L, link, "")
            } else {
                WebIntentBuilder(context)
                        .setUrl(link)
                        .build().start()
            }
        }

        setOnLongClickListener {
            TouchableSpan.longClickWeb(context, link)
            true
        }
    }

    fun clear() {
        Glide.clear(image)
        blankImage.visibility = View.GONE
        progress.visibility = View.GONE

        title.text = ""
        summary.text = ""

        loadedPreview = null

        setOnClickListener { }
        setOnLongClickListener { true }

        tag = ""
    }

    companion object {
        private val ignoredLinks = listOf(
                "pic.twitter.com",
                "twitter.com/i/moments",
                "tl.gd",
                "vine.co",
                "twitch.tv",
                "youtube",
                "youtu.be",
                "bit.ly"
        )

        fun ignoreLink(link: String): Boolean {
            return ignoredLinks.any { link.contains(it) }
        }
    }
}