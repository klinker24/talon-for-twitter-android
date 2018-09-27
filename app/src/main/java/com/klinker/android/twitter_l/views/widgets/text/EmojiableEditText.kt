package com.klinker.android.twitter_l.views.widgets.text

import android.content.Context
import androidx.emoji.widget.EmojiEditTextHelper
import androidx.appcompat.widget.AppCompatEditText
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.klinker.android.twitter_l.data.EmojiStyle
import com.klinker.android.twitter_l.settings.AppSettings

open class EmojiableEditText : AppCompatEditText {

    private val useEmojiCompat: Boolean
        get() = AppSettings.getInstance(context).emojiStyle != EmojiStyle.DEFAULT

    private var mEmojiEditTextHelper: EmojiEditTextHelper? = null
    private val emojiEditTextHelper: EmojiEditTextHelper
        get() {
            if (mEmojiEditTextHelper == null) {
                mEmojiEditTextHelper = EmojiEditTextHelper(this)
            }
            return mEmojiEditTextHelper as EmojiEditTextHelper
        }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        if (useEmojiCompat) {
            super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
        }
    }

    override fun setKeyListener(keyListener: android.text.method.KeyListener) {
        if (useEmojiCompat) {
            super.setKeyListener(emojiEditTextHelper.getKeyListener(keyListener))
        } else {
            super.setKeyListener(keyListener)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return if (useEmojiCompat) {
            val inputConnection = super.onCreateInputConnection(outAttrs)
            emojiEditTextHelper.onCreateInputConnection(inputConnection, outAttrs)!!
        } else {
            super.onCreateInputConnection(outAttrs)
        }
    }
}