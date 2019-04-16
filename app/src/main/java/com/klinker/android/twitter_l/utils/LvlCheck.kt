package com.klinker.android.twitter_l.utils

import android.content.Context
import android.util.Log
import com.github.javiersantos.piracychecker.*
import com.klinker.android.twitter_l.BuildConfig


@Suppress("ConstantConditionIf")
object LvlCheck {

    @JvmStatic
    fun check(context: Context) {
        val base64 = BuildConfig.LVL_KEY
        if (base64 == "none") {
            return
        }

        context.piracyChecker {
            enableGooglePlayLicensing(base64)
            callback {
                allow {
                    Log.v("LvlChecker", "app was purchased on the store.")
                }
                doNotAllow { piracyCheckerError, pirateApp ->
                    Log.v("LvlChecker", "the app is unlicensed.")
                }
                onError { error ->
                    Log.v("LvlChecker", "an error occurred during license verification: $error")
                }
            }
        }.start()
    }

}