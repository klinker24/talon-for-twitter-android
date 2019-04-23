package com.klinker.android.twitter_l.utils

import android.annotation.SuppressLint
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import com.github.javiersantos.piracychecker.*
import com.klinker.android.twitter_l.BuildConfig
import com.klinker.android.twitter_l.activities.MainActivity
import java.util.*


@Suppress("ConstantConditionIf")
@SuppressLint("ApplySharedPref")
object LvlCheck {

    @JvmStatic
    fun check(context: MainActivity, retryable: Boolean = true) {
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
                    sharedPrefs.edit().putInt("license_failed_days", 0).commit()
                }
                doNotAllow { _, _ ->
                    if (retryable) {
                        LvlCheck.check(context, false)
                    } else {
                        AnalyticsHelper.appNotPurchased(context)

                        // I will give them three days to make the purchase.
                        // There could be an issue with the Play Store, or something else.

                        val daysFailed = sharedPrefs.getInt("license_failed_days", 0) + 1
                        when {
                            daysFailed >= 3 -> {
                                // Warn the user that they are about to be logged out.

                                AnalyticsHelper.appNotPurchasedLastWarning(context)
                                AlertDialog.Builder(context)
                                        .setMessage("Google Play is still reporting that you have not purchased the app. " +
                                                "You will now be logged out.")
                                        .setPositiveButton(android.R.string.ok) { _, _ -> context.logoutFromTwitter() }
                            }
                            daysFailed >= 2 -> {
                                // Warn the user that they have failed the license check for two days in a row.
                                // They will be logged out tomorrow if they don't purchase the app

                                AnalyticsHelper.appNotPurchasedFirstWarning(context)
                                AlertDialog.Builder(context)
                                        .setMessage("Google Play is reporting that you have not purchased the app. " +
                                                "You will be logged out, tomorrow, unless you make a purchase.")
                                        .setPositiveButton(android.R.string.ok) { _, _ -> }
                            }
                            else -> sharedPrefs.edit().putInt("license_failed_days", daysFailed).commit()
                        }
                    }

                }
                onError { _ ->
                    // rerun the license check, next time they open the app
                    sharedPrefs.edit().putLong("last_licence_check", oneDayAgo).commit()
                }
            }
        }.start()
    }

}