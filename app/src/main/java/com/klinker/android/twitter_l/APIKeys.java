/*
 * Copyright 2014 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.twitter_l;

import android.content.Context;
import android.content.SharedPreferences;
import com.klinker.android.twitter_l.settings.AppSettings;

public class APIKeys {

    public String consumerKey;
    public String consumerSecret;

    public APIKeys(Context c, int currentAccount) {
        super();

        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(c);
        if (currentAccount == -1) {
            if (sharedPrefs == null) {
                sharedPrefs = c.getSharedPreferences("com.klinker.android.twitter_world_preferences",
                        Context.MODE_PRIVATE);
            }

            currentAccount = sharedPrefs.getInt("current_account", 1);
        }

        switch (sharedPrefs.getInt("key_version_" + currentAccount, 1)) {
            case 1:
                consumerKey = TWITTER_CONSUMER_KEY;
                consumerSecret = TWITTER_CONSUMER_SECRET;
                break;
            case 2:
                consumerKey = TWITTER_CONSUMER_KEY_2;
                consumerSecret = TWITTER_CONSUMER_SECRET_2;
                break;
            case 3:
                consumerKey = TWITTER_CONSUMER_KEY_3;
                consumerSecret = TWITTER_CONSUMER_SECRET_3;
                break;
            case 4:
                consumerKey = TWITTER_CONSUMER_KEY_4;
                consumerSecret = TWITTER_CONSUMER_SECRET_4;
                break;
        }
    }

    public APIKeys(Context c) {
        this(c, -1);
    }

    /**
     * Twitter's API Keys
     * THESE TWO ARE THE ONLY ONES REQUIRED TO RUN THE APP!
     * They are easy to obtain from Twitter
     *
     * Sign in with your Twitter credentials here:
     * https://apps.twitter.com/
     *
     * Then create a new application.
     *
     * For steps to creating an application, view the Readme.md
     */

    // Key version is stored in the MaterialLogin.KEY_VERSION field.
    // Talon Plus
    public static String TWITTER_CONSUMER_KEY = BuildConfig.CONSUMER_KEY_OLD_1;
    public static String TWITTER_CONSUMER_SECRET = BuildConfig.CONSUMER_SECRET_OLD_1;

    // Talon (Plus)
    public static String TWITTER_CONSUMER_KEY_2 = BuildConfig.CONSUMER_KEY_OLD_2;
    public static String TWITTER_CONSUMER_SECRET_2 = BuildConfig.CONSUMER_SECRET_OLD_2;

    // Talon - Plus
    public static String TWITTER_CONSUMER_KEY_3 = BuildConfig.CONSUMER_KEY_OLD_3;
    public static String TWITTER_CONSUMER_SECRET_3 = BuildConfig.CONSUMER_SECRET_OLD_3;

    // Talon Android (June 5th w/ 77,056 downloads)
    public static String TWITTER_CONSUMER_KEY_4 = BuildConfig.CONSUMER_KEY;
    public static String TWITTER_CONSUMER_SECRET_4 = BuildConfig.CONSUMER_SECRET;

    /**
     * For the In-App Youtube Player
     * It WILL NOT work if you do not obtain a key for yourself.
     * It is easy to get one of these though.
     *
     * Here is how you can get a key for yourself:
     * https://developers.google.com/youtube/android/player/register
     */
    public static String YOUTUBE_API_KEY = BuildConfig.YOUTUBE_API_KEY;

    /**
     * These are third party service API keys for Talon.
     *
     * If you wish to use these services, You will need to get a key for the ones you want to use.
     */
    public static final String TWEETMARKER_API_KEY = BuildConfig.TWEETMARKER_KEY;
    public static final String TWITLONGER_API_KEY = BuildConfig.TWITLONGER_KEY;
    public static final String GIPHY_API_KEY = BuildConfig.GIPHY_API_KEY;
    public static final String ARTICLE_API_KEY = BuildConfig.ARTICLE_API_KEY;
    public static final String MERCURY_API_KEY = BuildConfig.MERCURY_API_KEY;
}
