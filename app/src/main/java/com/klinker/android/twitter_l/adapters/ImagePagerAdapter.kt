package com.klinker.android.twitter_l.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

import com.klinker.android.twitter_l.activities.media_viewer.new_style.ImageFragment

class ImagePagerAdapter(fm: FragmentManager, private val urls: Array<String>) : FragmentPagerAdapter(fm) {

    private val fragments = mutableListOf<Fragment>()

    init {
        urls.indices.mapTo(fragments) { ImageFragment.getInstance(it, urls[it]) }
    }

    override fun getItem(i: Int): Fragment {
        return fragments[i]
    }

    override fun getCount(): Int {
        return fragments.size
    }
}
