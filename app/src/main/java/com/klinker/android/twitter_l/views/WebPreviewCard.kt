package com.klinker.android.twitter_l.views

import android.content.Context
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.data.WebPreview
import com.klinker.android.twitter_l.settings.AppSettings

class WebPreviewCard @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    interface OnLoad {
        fun onLinkLoaded(link: String, preview: WebPreview)
    }

    private var loadedPreview: WebPreview? = null

    private val image: ImageView by lazy { findViewById<View>(R.id.web_image) as ImageView }
    private val title: TextView by lazy { findViewById<View>(R.id.web_title) as TextView }
    private val summary: TextView by lazy { findViewById<View>(R.id.web_summary) as TextView }

    init {
        LayoutInflater.from(context).inflate(R.layout.card_web_preview, this, true)

        title.textSize = AppSettings.getInstance(context).textSize + 2.toFloat()
        summary.textSize = AppSettings.getInstance(context).textSize.toFloat()
    }

    fun loadLink(link: String, listener: OnLoad) {
        if (loadedPreview != null) {
            displayPreview(loadedPreview!!)
            return
        }

        // TODO: Load here
    }

    fun displayPreview(preview: WebPreview) {
        loadedPreview = preview
    }

    fun clear() {
        image.setImageDrawable(null)
        title.text = ""
        summary.text = ""

        loadedPreview = null
    }
}