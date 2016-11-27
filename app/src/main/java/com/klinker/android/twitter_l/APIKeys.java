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
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;
import com.google.android.vending.licensing.util.Base64;
import com.klinker.android.twitter_l.settings.AppSettings;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class APIKeys {

    public String consumerKey;
    public String consumerSecret;

    public APIKeys(Context c, int currentAccount) {
        SharedPreferences sharedPrefs = AppSettings.getSharedPreferences(c);
        if (currentAccount == -1) {
            currentAccount = sharedPrefs.getInt("current_account", 1);
        }

        switch (sharedPrefs.getInt("key_version_" + currentAccount, 1)) {
            case 1:
                consumerKey = TWITTER_CONSUMER_KEY;
                consumerSecret = TWITTER_CONSUMER_SECRET;
                break;
            case 2:
                consumerKey = getConsumerKey(c, sharedPrefs.getString("consumer_key_2", ""), 2);
                consumerSecret = TWITTER_CONSUMER_SECRET_2;
                break;
            case 3:
                consumerKey = getConsumerKey(c, sharedPrefs.getString("consumer_key_3", ""), 3);
                consumerSecret = TWITTER_CONSUMER_SECRET_3;
                break;
        }
    }

    public APIKeys(Context c) {
        this(c, -1);
    }

    private String getConsumerKey(Context c, String encrypted, int keyVersion) {
        try {
            Signature[] signatures =
                    c.getPackageManager().getPackageInfo(c.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
            String sig = signatures[0].toCharsString();

            String key = sig.substring(12,28);

            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            byte[] decrypted = cipher.doFinal(Base64.decode(encrypted));

            String decrypt = new String(decrypted);

            switch (keyVersion) {
                case 1:
                    return TWITTER_CONSUMER_KEY;
                case 2:
                    return decrypt + TWITTER_CONSUMER_KEY_2_FINAL;
                case 3:
                    return decrypt + TWITTER_CONSUMER_KEY_3_FINAL;
                default:
                    return TWITTER_CONSUMER_KEY;
            }

        } catch (Throwable e) {
            e.printStackTrace();
            return encrypted;
        }
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
    public static String TWITTER_CONSUMER_KEY = "ByivySWf9pykD5CpamBNODii8";
    public static String TWITTER_CONSUMER_SECRET = "ARKOhkKBSTeOl1fyhL2njfMMqRlDkDmlhFtEJZfD5jmgP7kttg";

    // Talon (Plus)
    public static String TWITTER_CONSUMER_KEY_2_FINAL = "vs2feUuRy";
    public static String TWITTER_CONSUMER_SECRET_2 = "DcKxlwIl9xrjMYxQd1oEc1HXqChP52L63uPZFsLIZVy3YGqpIu";

    // Talon - Plus
    public static String TWITTER_CONSUMER_KEY_3_FINAL = "89gZ0FjMm";
    public static String TWITTER_CONSUMER_SECRET_3 = "JNd4xRJm2QyUEPMKiL46Idm13kITNNrY0BZh2iUAQqbVEVl0UJ";

    /**
     * For the In-App Youtube Player
     * It WILL NOT work if you do not obtain a key for yourself.
     * It is easy to get one of these though.
     *
     * Here is how you can get a key for yourself:
     * https://developers.google.com/youtube/android/player/register
     */
    public static String YOUTUBE_API_KEY = "AIzaSyB4uB2R54EAfkYGN1lsm1CdcQhBBzqGwLY";

    /**
     * These are third party service API keys for Talon.
     *
     * If you wish to use these services, You will need to get a key as I will not be sharing mine
     * for obvious security reasons.
     *
     * Tweetmarker is a paid service, so if you want a key, you will have to pay $75 a month for it
     * For Twitlonger, you must request access to their API for your app. I do not know if he would grant an Open Source Api key or not.
     * TwitPic is dead, but I kept its classes in here so that you can still learn from them. The service no longer is supported.
     */
    public static final String TWEETMARKER_API_KEY = "TA-89115729700A";
    public static final String TWITLONGER_API_KEY = "rU5qsRgK23glt5dcUQ55b4hsN8F5rak0";
    public static final String TWITPIC_API_KEY = "8cd3757bb6acb94c61e3cbf840c91872";
    public static final String GIPHY_API_KEY = "3oEduMYKu0Rj1WA4dG";
    public static final String ARTICLE_API_KEY = "df3f7fe7fc691b6019ccf37c3f2f5606";
}
