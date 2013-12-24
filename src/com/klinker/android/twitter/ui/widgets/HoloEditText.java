/*
 * Copyright 2013 Jacob Klinker
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

package com.klinker.android.twitter.ui.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.EditText;

public class HoloEditText extends EditText {

    private static SharedPreferences sharedPreferences;

    public HoloEditText(Context context) {
        super(context);
        setUp(context);
    }

    public HoloEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setUp(context);
    }

    public HoloEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setUp(context);
    }

    private void setUp(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }

        setTypeface(context);
    }

    public static Typeface typeface;
    private static boolean useDeviceFont;

    private void setTypeface(Context context) {
        if (typeface == null) {
            useDeviceFont = sharedPreferences.getBoolean("device_font", false);
            typeface = Typeface.createFromAsset(context.getAssets(), "Roboto-Light.ttf");
        }

        if (!useDeviceFont) {
            setTypeface(typeface);
        }
    }
}
