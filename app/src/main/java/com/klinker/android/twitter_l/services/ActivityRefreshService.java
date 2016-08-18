package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;

import com.klinker.android.twitter_l.activities.main_fragments.other_fragments.ActivityFragment;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ActivityUtils;
import com.klinker.android.twitter_l.utils.Utils;

public class ActivityRefreshService extends ScheduledService {

    SharedPreferences sharedPrefs;

    public ActivityRefreshService() {
        super("ActivityRefreshService");
    }

    @Override
    protected ScheduleInfo getScheduleInfo(AppSettings settings) {
        return new ScheduleInfo(ActivityRefreshService.class, ActivityFragment.ACTIVITY_REFRESH_ID, settings.activityRefresh);
    }

    @Override
    public void handleIntent(Intent intent) {
        AppSettings settings = AppSettings.getInstance(this);
        ActivityUtils utils = new ActivityUtils(this, false);

        if (Utils.getConnectionStatus(this) && !settings.syncMobile) {
            return;
        }

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification();
        }

        if (settings.syncSecondMentions) {
            Intent second = new Intent(this, SecondActivityRefreshService.class);
            startService(second);
        }
    }
}
