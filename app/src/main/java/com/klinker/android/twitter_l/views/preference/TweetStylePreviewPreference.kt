package com.klinker.android.twitter_l.views.preference

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.preference.Preference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.settings.AppSettings
import com.klinker.android.twitter_l.utils.Utils
import com.klinker.android.twitter_l.utils.text.TextUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class TweetStylePreviewPreference : Preference {

    companion object {
        private val NAME = "Luke Klinker"
        private val SCREEN_NAME = "@lukeklinker"
        private val RETWEETER = "KlinkerApps"
        private val TWEET = "@TalonAndroid and Pulse SMS are great!"

        private val IMAGE_URL = "https://pbs.twimg.com/media/DKK-EnFUMAYoxeZ.jpg:large"
        private val PROFILE_PIC_URL = "https://pbs.twimg.com/profile_images/720974705042137088/q1foR7W5_400x400.jpg"
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var root: View? = null
    private val tweetHolder
        get() = root?.findViewById<LinearLayout>(R.id.root_view)
    
    init {
        layoutResource = R.layout.preference_tweet_preview
    }

    override fun onBindView(view: View?) {
        super.onBindView(view)
        if (view != null) {
            root = view
            showTweet()
        }
    }
    
    fun showTweet() {
        val holder = tweetHolder ?: return
        val settings = AppSettings.getInstance(context)
        
        holder.removeAllViews()
        val tweet = when {
            settings.condensedTweets() -> LayoutInflater.from(context).inflate(R.layout.tweet_condensed, holder, true)
            settings.revampedTweets() -> LayoutInflater.from(context).inflate(R.layout.tweet_revamp, holder, true)
            else -> LayoutInflater.from(context).inflate(R.layout.tweet, holder, true)
        }


        setTweetContent(tweet)
    }
    
    @SuppressLint("SetTextI18n")
    private fun setTweetContent(tweet: View) {
        val settings = AppSettings.getInstance(context)

        val profilePic = tweet.findViewById<ImageView>(R.id.profile_pic)
        val name = tweet.findViewById<TextView>(R.id.name)
        val screenName = tweet.findViewById<TextView>(R.id.screenname)
        val retweeter = tweet.findViewById<TextView>(R.id.retweeter)
        val time = tweet.findViewById<TextView>(R.id.time)
        val tweetText = tweet.findViewById<TextView>(R.id.tweet)
        val background = tweet.findViewById<View>(R.id.background)
        val image = tweet.findViewById<ImageView>(R.id.image)
        val imageHolder = tweet.findViewById<View>(R.id.picture_holder)
        val revampedRetweetIcon = tweet.findViewById<View>(R.id.retweet_icon)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            image.clipToOutline = true
        }

        tweetText.textSize = settings.textSize.toFloat()
        screenName.textSize = (settings.textSize - if (settings.condensedTweets() || settings.revampedTweets()) 1 else 2).toFloat()
        name.textSize = (settings.textSize + if (settings.condensedTweets()) 1 else 4).toFloat()
        time.textSize = (settings.textSize - if (settings.revampedTweets()) 2 else 3).toFloat()
        retweeter.textSize = (settings.textSize - 2).toFloat()

        name.text = NAME
        screenName.text = SCREEN_NAME
        tweetText.text = TextUtils.colorText(context, TWEET, settings.themeColors.accentColor)
        retweeter.text = TextUtils.colorText(context, context.resources.getString(R.string.retweeter) + RETWEETER, settings.themeColors.accentColor)
        time.text = Utils.getTimeAgo(System.currentTimeMillis() - 1000 * 60 * 5, context, settings.revampedTweets())

        Glide.with(context).load(PROFILE_PIC_URL).into(profilePic)
        Glide.with(context).load(IMAGE_URL).into(image)

        when {
            settings.picturesType == AppSettings.PICTURES_NORMAL || settings.picturesType == AppSettings.CONDENSED_TWEETS || settings.revampedTweets() -> {
                imageHolder.visibility = View.VISIBLE
            }
            settings.picturesType == AppSettings.PICTURES_SMALL -> {
                imageHolder.layoutParams.height = Utils.toDP(120, context)
                imageHolder.requestLayout()
                imageHolder.visibility = View.VISIBLE
            }
            else -> imageHolder.visibility = View.GONE
        }

        retweeter.visibility = View.VISIBLE

        if (!settings.revampedTweets()) {
            val a = context.theme.obtainStyledAttributes(intArrayOf(R.attr.windowBackground))
            val resource = a.getResourceId(0, 0)
            a.recycle()
            background.setBackgroundResource(resource)
        } else {
            retweeter.text = TextUtils.colorText(context, "@" + RETWEETER, settings.themeColors.accentColor)
            revampedRetweetIcon.visibility = View.VISIBLE
        }

        if (settings.absoluteDate) {
            var dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            var timeFormatter = android.text.format.DateFormat.getTimeFormat(context)

            if (AppSettings.getInstance(context).militaryTime) {
                dateFormatter = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                timeFormatter = SimpleDateFormat("kk:mm")
            }

            val date = Date(System.currentTimeMillis() - 1000 * 60 * 5)
            time.text = timeFormatter.format(date).replace("24:", "00:") + ", " + dateFormatter.format(date)
        }
    }
}