package com.klinker.android.twitter_l.views.widgets.text

import android.content.Context
import androidx.emoji.widget.EmojiTextViewHelper
import androidx.appcompat.widget.AppCompatTextView
import android.text.InputFilter
import android.util.AttributeSet
import com.klinker.android.twitter_l.R
import com.klinker.android.twitter_l.data.EmojiStyle
import com.klinker.android.twitter_l.settings.AppSettings

open class EmojiableTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatTextView(context, attrs, defStyleAttr) {

    // for some reason, the flag emojis aren't displaying in people's names
    private val useEmojiCompat: Boolean
        get() = AppSettings.getInstance(context).emojiStyle != EmojiStyle.DEFAULT && id != R.id.name

    private var helper: EmojiTextViewHelper? = null
    private val emojiHelper: EmojiTextViewHelper
        get() {
            if (helper == null) {
                helper = EmojiTextViewHelper(this)
            }
            return helper as EmojiTextViewHelper
        }

    init {
        if (useEmojiCompat) {
            emojiHelper.updateTransformationMethod()
        }
    }

    override fun setFilters(filters: Array<InputFilter>) {
        if (useEmojiCompat) {
            super.setFilters(emojiHelper.getFilters(filters))
        } else {
            super.setFilters(filters)
        }
    }

    override fun setAllCaps(allCaps: Boolean) {
        super.setAllCaps(allCaps)
        emojiHelper.setAllCaps(allCaps)
    }

}