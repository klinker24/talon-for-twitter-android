package com.klinker.android.twitter_l.utils;

import android.content.Context;

import com.klinker.android.twitter_l.services.ActivityRefreshService;
import com.klinker.android.twitter_l.services.DataCheckService;
import com.klinker.android.twitter_l.services.DirectMessageRefreshService;
import com.klinker.android.twitter_l.services.ListRefreshService;
import com.klinker.android.twitter_l.services.MentionsRefreshService;
import com.klinker.android.twitter_l.services.SendScheduledTweet;
import com.klinker.android.twitter_l.services.TimelineRefreshService;
import com.klinker.android.twitter_l.services.TrimDataService;

public class ServiceUtils {

    public static void rescheduleAllServices(Context context) {
        DataCheckService.scheduleRefresh(context);
        TimelineRefreshService.scheduleRefresh(context);
        TrimDataService.scheduleRefresh(context);
        MentionsRefreshService.scheduleRefresh(context);
        DirectMessageRefreshService.scheduleRefresh(context);
        ListRefreshService.scheduleRefresh(context);
        ActivityRefreshService.scheduleRefresh(context);
        SendScheduledTweet.scheduleNextRun(context);
    }
}
