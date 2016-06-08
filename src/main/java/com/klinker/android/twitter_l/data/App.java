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

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;

import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.utils.EmojiUtils;

import java.io.File;
import java.util.Locale;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                EmojiUtils.init(App.this);
            }
        }).start();
        updateResources(this);
    }

    public static void updateResources(Context app) {
        AppSettings settings = AppSettings.getInstance(app);
        Resources res = app.getResources();

        if (!settings.locale.equals("none")) {
            // Change locale settings in the app.
            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.locale = new Locale(settings.locale);
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
}