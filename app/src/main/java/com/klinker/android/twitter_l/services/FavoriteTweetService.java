package com.klinker.android.twitter_l.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationManagerCompat;

import com.klinker.android.twitter_l.services.abstract_services.KillerIntentService;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.NotificationUtils;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;

public class FavoriteTweetService extends KillerIntentService {

    private static final String ARG_ACCOUNT_TO_FAVORITE_WITH = "account_num";
    private static final String ARG_TWEET_ID = "tweet_id";
    private static final String ARG_NOTIFICATION_ID = "notification_id";

    public static Intent getIntent(Context callingContext, int accountNumberToFavoriteWith, long tweetId, int notificationId) {
        Intent favorite = new Intent(callingContext, FavoriteTweetService.class);
        favorite.putExtra(ARG_ACCOUNT_TO_FAVORITE_WITH, accountNumberToFavoriteWith);
        favorite.putExtra(ARG_TWEET_ID, tweetId);
        favorite.putExtra(ARG_NOTIFICATION_ID, notificationId);

        return favorite;
    }

    public FavoriteTweetService() {
        super("FavoriteTweetService");
    }

    @Override
    protected void handleIntent(Intent intent) {
        int accountToFavoriteWith = intent.getIntExtra(ARG_ACCOUNT_TO_FAVORITE_WITH, 1);
        long tweetId = intent.getLongExtra(ARG_TWEET_ID, 1);
        int notificationId = intent.getIntExtra(ARG_NOTIFICATION_ID, 1);

        Twitter twitter;
        if (accountToFavoriteWith == AppSettings.getInstance(this).currentAccount) {
            twitter = Utils.getTwitter(this, AppSettings.getInstance(this));
        } else {
            twitter = Utils.getSecondTwitter(this);
        }

        try {
            twitter.createFavorite(tweetId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);

        notificationManager.cancel(notificationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationUtils.cancelGroupedNotificationWithNoContent(this);
        }
    }
}
