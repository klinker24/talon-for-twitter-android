package com.klinker.android.twitter_l.utils

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.github.javiersantos.piracychecker.*
import com.klinker.android.twitter_l.BuildConfig


@Suppress("ConstantConditionIf")
object LvlCheck {

    @JvmStatic
    fun check(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPrefs.getBoolean("was_downloaded_from_store", false)) {
            return
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
                    sharedPrefs.edit().putBoolean("was_downloaded_from_store", true).commit()
                }
                doNotAllow { piracyCheckerError, pirateApp ->
                    AnalyticsHelper.appNotPurchased(context)

                    // TODO: warn the user that they will be logged out.
                }
                onError { error ->
                    Log.v("LvlChecker", "an error occurred during license verification: $error")
                }
            }
        }.start()
    }

}