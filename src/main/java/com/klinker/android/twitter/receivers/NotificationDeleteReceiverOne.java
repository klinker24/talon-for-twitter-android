package com.klinker.android.twitter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.klinker.android.twitter.services.MarkReadService;

/**
 * Created by luke on 7/24/14.
 */
public class NotificationDeleteReceiverOne extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("notification_deleted_talon", "starting receiver for notification deleted on account 1");
        context.startService(new Intent(context, MarkReadService.class));
    }
}
