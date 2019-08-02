package com.klinker.android.twitter_l.utils

import android.annotation.SuppressLint
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.google.android.vending.licensing.AESObfuscator
import com.google.android.vending.licensing.LicenseChecker
import com.google.android.vending.licensing.LicenseCheckerCallback
import com.google.android.vending.licensing.ServerManagedPolicy
import com.klinker.android.twitter_l.BuildConfig
import com.klinker.android.twitter_l.activities.MainActivity
import java.util.*

@Suppress("ConstantConditionIf")
@SuppressLint("ApplySharedPref")
object LvlCheck {

    @SuppressLint("HardwareIds")
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

        val checker = LicenseChecker(context, ServerManagedPolicy(context, AESObfuscator(
                byteArrayOf(-46, 65, 30, -128, -103, -57, 74, -64, 51, 88, -95, -45, 77, -117, -36, -113, -11, 32, -64, 89),
                context.packageName,
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        )), base64)

        checker.checkAccess(object : LicenseCheckerCallback {
            override fun allow(reason: Int) {
                AnalyticsHelper.appPurchased(context)
                sharedPrefs.edit().putInt("license_days_failed", 0).commit()
            }

            override fun dontAllow(reason: Int) {
                if (retryable) {
                    check(context, false)
                } else {
                    AnalyticsHelper.appNotPurchased(context)

                    // I will give them three days to make the purchase.
                    // There could be an issue with the Play Store, or something else.

                    val daysFailed = sharedPrefs.getInt("license_days_failed", 0) + 1
                    when {
                        daysFailed >= 3 -> {
                            // Warn the user that they are about to be logged out.

                            AnalyticsHelper.appNotPurchasedLastWarning(context)
                            sharedPrefs.edit().putLong("last_licence_check", oneDayAgo).commit()
                            checker.followLastLicensingUrl(context)
                            context.finish()
                        }
                        daysFailed >= 2 -> {
                            // Warn the user that they have failed the license check for two days in a row.
                            // They will be logged out tomorrow if they don't purchase the app

                            AnalyticsHelper.appNotPurchasedFirstWarning(context)
                        }
                    }

                    sharedPrefs.edit().putInt("license_days_failed", daysFailed).commit()
                }
            }

            override fun applicationError(errorCode: Int) {
                // rerun the license check, next time they open the app
                sharedPrefs.edit().putLong("last_licence_check", oneDayAgo).commit()
            }

        })
    }

}