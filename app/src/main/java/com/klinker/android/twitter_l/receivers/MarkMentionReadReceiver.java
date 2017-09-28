package com.klinker.android.twitter_l.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.klinker.android.twitter_l.data.sq_lite.MentionsDataSource;
import com.klinker.android.twitter_l.activities.media_viewer.image.TimeoutThread;

public class MarkMentionReadReceiver extends BroadcastReceiver {

    private static final String ARG_TWEET_ID = "tweet_id";

    public static Intent getIntent(Context callingContext, long tweetId) {
        Intent receiver = new Intent(callingContext, MarkMentionReadReceiver.class);
        receiver.putExtra(ARG_TWEET_ID, tweetId);

        return receiver;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v("talon_notification", "swiped to delete a notification");

        final long tweetId = intent.getLongExtra(ARG_TWEET_ID, 1);

        if (tweetId != 1) {
            new TimeoutThread(new Runnable() {
                @Override
                public void run() {
                    MentionsDataSource dataSource = MentionsDataSource.getInstance(context);
                    dataSource.markRead(tweetId);
                }
            }).start();
        }
    }
}
