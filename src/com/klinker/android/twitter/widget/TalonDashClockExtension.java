package com.klinker.android.twitter.widget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.HomeContentProvider;
import com.klinker.android.twitter.ui.MainActivity;
import com.klinker.android.twitter.utils.NotificationUtils;


public class TalonDashClockExtension extends DashClockExtension {

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);

        String[] watcher = {"content://" + HomeContentProvider.AUTHORITY};
        this.addWatchContentUris(watcher);
        this.setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int arg0) {

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        int currentAccount = sharedPrefs.getInt("current_account", 1);

        int[] unreads = NotificationUtils.getUnreads(this);
        int homeTweets = unreads[0];
        int mentionsTweets = unreads[1];
        int dmTweets = unreads[2];

        if (!sharedPrefs.getBoolean("timeline_notifications", true)) {
            homeTweets = 0;
        }
        if (!sharedPrefs.getBoolean("mentions_notifications", true)) {
            mentionsTweets = 0;
        }
        if (!sharedPrefs.getBoolean("direct_message_notifications", true)) {
            dmTweets = 0;
        }

        if (homeTweets > 0 || mentionsTweets > 0 || dmTweets > 0) {

            Intent intent = new Intent(this, MainActivity.class);

            Bundle b = new Bundle();
            b.putBoolean("dashclock", true);
            intent.putExtras(b);

            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_stat_icon)
                    .status(homeTweets + mentionsTweets + dmTweets + "")
                    .expandedTitle(NotificationUtils.getTitle(unreads, this, currentAccount)[0])
                    .expandedBody(NotificationUtils.getLongTextNoHtml(unreads, this, currentAccount))
                    .clickIntent(intent));
        } else {
            publishUpdate(new ExtensionData()
                    .visible(false));
        }
    }
}