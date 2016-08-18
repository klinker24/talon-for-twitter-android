package com.klinker.android.twitter_l.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.ActivityUtils;
import com.klinker.android.twitter_l.utils.Utils;

public class SecondActivityRefreshService extends LimitedRunService {

    SharedPreferences sharedPrefs;

    public SecondActivityRefreshService() {
        super("SecondActivityRefreshService");
    }

    @Override
    public void handleIntentIfTime(Intent intent) {
        AppSettings settings = AppSettings.getInstance(this);
        ActivityUtils utils = new ActivityUtils(this, true);

        if (Utils.getConnectionStatus(this) && !settings.syncMobile) {
            return;
        }

        boolean newActivity = utils.refreshActivity();

        if (settings.notifications && settings.activityNot && newActivity) {
            utils.postNotification(ActivityUtils.SECOND_NOTIFICATION_ID);
        }
    }

    private static long LAST_RUN = 0;

    @Override
    protected long getLastRun() {
        return LAST_RUN;
    }

    @Override
    protected void setJustRun(long currentTime) {
        LAST_RUN = currentTime;
    }
}
