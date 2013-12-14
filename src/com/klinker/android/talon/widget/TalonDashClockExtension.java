package com.klinker.android.talon.widget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.klinker.android.talon.R;
import com.klinker.android.talon.sq_lite.HomeContentProvider;
import com.klinker.android.talon.sq_lite.HomeDataSource;
import com.klinker.android.talon.sq_lite.MentionsDataSource;
import com.klinker.android.talon.ui.MainActivity;


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

        HomeDataSource data = new HomeDataSource(this);
        data.open();
        int homeTweets = data.getUnreadCount(currentAccount);
        data.close();

        MentionsDataSource mentions = new MentionsDataSource(this);
        mentions.open();
        int mentionsTweets = mentions.getUnreadCount(currentAccount);
        mentions.close();

        int dmTweets = sharedPrefs.getInt("dm_unread_" + currentAccount, 0);

        if (homeTweets > 0 || mentionsTweets > 0 || dmTweets > 0) {

            String body = "";

            if (homeTweets > 0) {
                body += homeTweets + " " + (homeTweets == 1 ? this.getResources().getString(R.string.new_tweet) : this.getResources().getString(R.string.new_tweets)) + (mentionsTweets > 0 || dmTweets > 0 ? "\n" : "");
            }

            if (mentionsTweets > 0) {
                body += mentionsTweets + " " + (mentionsTweets == 1 ? this.getResources().getString(R.string.new_mention) : this.getResources().getString(R.string.new_mentions)) + (dmTweets > 0 ? "\n" : "");
            }

            if (dmTweets > 0) {
                body += dmTweets + " " + (dmTweets == 1 ? this.getResources().getString(R.string.new_direct_message) : this.getResources().getString(R.string.new_direct_messages));
            }

            Intent intent = new Intent(this, MainActivity.class);

            Bundle b = new Bundle();
            b.putBoolean("dashclock", true);
            intent.putExtras(b);

            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.timeline_dark)
                    .status(homeTweets + mentionsTweets + dmTweets + "")
                    .expandedTitle(this.getResources().getString(R.string.app_name))
                    .expandedBody(body)
                    .clickIntent(intent));
        } else {
            publishUpdate(new ExtensionData()
                    .visible(false));
        }
    }
}