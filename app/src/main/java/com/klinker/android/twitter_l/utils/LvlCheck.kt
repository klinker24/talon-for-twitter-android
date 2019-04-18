package com.klinker.android.twitter_l.utils

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.github.javiersantos.piracychecker.*
import com.klinker.android.twitter_l.BuildConfig
import java.util.*


@Suppress("ConstantConditionIf")
object LvlCheck {

    @JvmStatic
    fun check(context: Context, retry: Boolean = true) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val oneDayAgo = Date().time - (1000 * 60 * 60 * 24)
        if (sharedPrefs.getLong("last_licence_check", -1) > oneDayAgo) {
            return
        } else {
            sharedPrefs.edit().putLong("last_licence_check", Date().time).commit()
        }

        val base64 = BuildConfig.LVL_KEY
        if (base64 == "none") {
            return
        }

        context.piracyChecker {
            enableGooglePlayLicensing(base64)
            callback {
                allow {
                    AnalyticsHelper.appPurchased(context)
                }
                doNotAllow { piracyCheckerError, pirateApp ->
                    AnalyticsHelper.appNotPurchased(context)

                    if (retry) {
                        LvlCheck.check(context, false)
                    } else {
                        // TODO: warn the user that they will be logged out.
                    }

                }
                onError { error ->
                    Log.v("LvlChecker", "an error occurred during license verification: $error")
                }
            }
        }.start()
    }

}