package com.klinker.android.twitter_l.views

import android.content.Context
import android.content.Intent
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.activities.BrowserActivity
import com.klinker.android.twitter_l.activities.media_viewer.VideoViewerActivity
import com.klinker.android.twitter_l.data.WebPreview
import com.klinker.android.twitter_l.settings.AppSettings
import com.klinker.android.twitter_l.utils.WebIntentBuilder
import com.klinker.android.twitter_l.utils.api_helper.MercuryArticleParserHelper

class WebPreviewCard @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    interface OnLoad {
        fun onLinkLoaded(link: String, preview: WebPreview?)
    }

    private var loadedPreview: WebPreview? = null

    private val image: ImageView by lazy { findViewById<View>(R.id.web_image) as ImageView }
    private val title: TextView by lazy { findViewById<View>(R.id.web_title) as TextView }
    private val summary: TextView by lazy { findViewById<View>(R.id.web_summary) as TextView }

    init {
        LayoutInflater.from(context).inflate(R.layout.card_web_preview, this, true)

        title.textSize = AppSettings.getInstance(context).textSize + 3.toFloat()
        summary.textSize = AppSettings.getInstance(context).textSize.toFloat()

        setOnClickListener {

        }
    }

    fun loadLink(link: String, listener: OnLoad) {
        if (loadedPreview != null) {
            displayPreview(loadedPreview!!)
            return
        }

        MercuryArticleParserHelper.getArticle(link) { webPreview ->
            listener.onLinkLoaded(link, webPreview)
            displayPreview(webPreview)
        }
    }

    fun displayPreview(preview: WebPreview?) {
        loadedPreview = preview

        if (preview != null) {
            Glide.with(context).load(preview.imageUrl).into(image)
            title.text = preview.title

            if (preview.summary.isBlank()) {
                summary.text = preview.webDomain
            } else {
                summary.text = preview.summary
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
        } else {
            Glide.clear(image)
            title.text = "No Preview available"
            summary.text = ""
        }
    }

    fun clear() {
        image.setImageDrawable(null)
        title.text = ""
        summary.text = ""

        loadedPreview = null

        setOnClickListener { }
    }
}