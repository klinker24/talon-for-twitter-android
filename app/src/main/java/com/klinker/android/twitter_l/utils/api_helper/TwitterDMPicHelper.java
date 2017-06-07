package com.klinker.android.twitter_l.utils.api_helper;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;

import twitter4j.Twitter;


public class TwitterDMPicHelper {

    public Bitmap getDMPicture(String picUrl, Twitter twitter, Context c) {

        try {
            InputStream stream = twitter.getDMImageAsStream(picUrl);
            Bitmap b = BitmapFactory.decodeStream(stream);
            stream.close();

            return b;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}