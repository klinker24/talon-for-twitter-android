package com.klinker.android.twitter_l.utils.api_helper

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

import com.klinker.android.twitter_l.data.sq_lite.DMDataSource
import com.klinker.android.twitter_l.services.background_refresh.SecondDMRefreshService
import com.klinker.android.twitter_l.settings.AppSettings
import com.klinker.android.twitter_l.utils.NotificationUtils
import com.klinker.android.twitter_l.utils.Utils
import twitter4j.*

object DirectMessageDownload {

    @JvmStatic fun download(context: Context, useSecondAccount: Boolean, alwaysSync: Boolean): Int {
        val sharedPrefs = AppSettings.getSharedPreferences(context)
        val settings = AppSettings.getInstance(context)

        // if they have mobile data on and don't want to sync over mobile data
        if (Utils.getConnectionStatus(context) && !settings.syncMobile && !alwaysSync) {
            return 0
        }

        try {
            var currentAccount = sharedPrefs.getInt("current_account", 1)
            val twitter: Twitter

            if (useSecondAccount) {
                twitter = Utils.getSecondTwitter(context)

                currentAccount = if (currentAccount == 1) 2 else 1
            } else {
                twitter = Utils.getTwitter(context, settings)
            }

            val lastId = sharedPrefs.getLong("last_direct_message_id_$currentAccount", 0)
            val dms = twitter.getDirectMessageEvents(50)

            if (dms.size != 0) {
                sharedPrefs.edit().putLong("last_direct_message_id_$currentAccount", dms[0].id).apply()
            }

            var dataSource = DMDataSource.getInstance(context)
            var inserted = 0

            val possibleUserIds = mutableListOf<Long>()
            for (i in 0 until dms.size) {
                possibleUserIds.add(dms[i].recipientId)
                possibleUserIds.add(dms[i].senderId)
            }

            val possibleUserIdsArray = possibleUserIds.distinctBy { it }.toLongArray()
            val possibleUsers = if (possibleUserIdsArray.isNotEmpty()) twitter.lookupUsers(*possibleUserIdsArray) else emptyList<User>()

            for (i in dms.indices) {
                val directMessage = dms[i]
                if (directMessage.id > lastId) {
                    try {
                        dataSource.createDirectMessage(directMessage, possibleUsers, currentAccount)
                    } catch (e: Exception) {
                        dataSource = DMDataSource.getInstance(context)
                        dataSource.createDirectMessage(directMessage, possibleUsers, currentAccount)
                    }

                    inserted++
                }
            }

            sharedPrefs.edit().putBoolean("refresh_me", true).apply()
            sharedPrefs.edit().putBoolean("refresh_me_dm", true).apply()

            if (settings.notifications && settings.dmsNot && inserted > 0) {
                val currentUnread = sharedPrefs.getInt("dm_unread_$currentAccount", 0)
                sharedPrefs.edit().putInt("dm_unread_$currentAccount", inserted + currentUnread).apply()

                if (useSecondAccount) {
                    NotificationUtils.notifySecondDMs(context, currentAccount)
                } else {
                    NotificationUtils.refreshNotification(context)
                }
            }


            if (!useSecondAccount && settings.syncSecondMentions) {
                SecondDMRefreshService.startNow(context)
            }

            if (!useSecondAccount) {
                context.sendBroadcast(Intent("com.klinker.android.twitter.NEW_DIRECT_MESSAGE"))
            }

            return inserted
        } catch (e: TwitterException) {
            // Error in updating status
            Log.d("Twitter Update Error", e.message)
        }

        return 0

    }
}
