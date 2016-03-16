package com.klinker.android.twitter_l.services;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.Utils;

import twitter4j.Twitter;

public class ReplySecondAccountFromWearService extends ReplyFromWearService {

    @Override
    public Twitter getTwitter() {
        return Utils.getSecondTwitter(this);
    }
}
