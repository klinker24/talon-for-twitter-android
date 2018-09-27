package com.klinker.android.twitter_l.data;
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

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.core.os.BuildCompat;
import android.util.DisplayMetrics;

import com.github.ajalt.reprint.core.Reprint;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.DynamicShortcutUtils;
import com.klinker.android.twitter_l.utils.EmojiUtils;
import com.klinker.android.twitter_l.utils.NotificationChannelUtil;
import com.klinker.android.twitter_l.utils.text.EmojiInitializer;

import java.util.Locale;

public class App extends MultiDexApplication {

    public static long DATA_USED = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        MultiDex.install(this);
        Reprint.initialize(this);
        EmojiInitializer.INSTANCE.initializeEmojiCompat(this);

        updateResources(this);
        runBackgroundSetup();
    }

    public static void updateResources(Context app) {
        AppSettings settings = AppSettings.getInstance(app);
        Resources res = app.getResources();

        if (!settings.locale.equals("none")) {
            // Change locale settings in the app.
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            if (settings.locale.equals("zh-rCN")) {
                conf.locale = Locale.SIMPLIFIED_CHINESE;
            } else if (settings.locale.equals("pt-rBR")) {
                conf.locale = new Locale("pt", "BR");
            } else {
                conf.locale = new Locale(settings.locale);
            }
            res.updateConfiguration(conf, dm);
        } else {
            // Change locale settings in the app.
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = new Locale(Locale.getDefault().getLanguage());
            res.updateConfiguration(conf, dm);
        }
    }

    public static App getInstance(Context context) {
        return (App) context.getApplicationContext();
    }

    public void runBackgroundSetup() {
        if (!"robolectric".equals(Build.FINGERPRINT) && BuildCompat.isAtLeastNMR1()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        NotificationChannelUtil.createNotificationChannels(App.this);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    EmojiUtils.init(App.this);

                    try {
                        new DynamicShortcutUtils(App.this).buildProfileShortcut();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }
    }
}