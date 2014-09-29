/*
 * Copyright (C) 2014 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.twitter.services;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.klinker.android.twitter.R;
import com.klinker.android.twitter.data.sq_lite.HomeDataSource;
import com.klinker.android.twitter.data.sq_lite.HomeSQLiteHelper;
import com.klinker.android.twitter.settings.AppSettings;
import com.klinker.android.twitter.transaction.KeyProperties;
import com.klinker.android.twitter.utils.WearableUtils;


import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class TweetWearableService extends WearableListenerService {

    private static final String TAG = "TweetWearableService";
    private static final int MAX_ARTICLES_TO_SYNC = 200;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        WearableUtils wearableUtils = new WearableUtils();

        String message = new String(messageEvent.getData());
        Log.d(TAG, "got message: " + message);

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        if (message.equals(KeyProperties.GET_DATA_MESSAGE)) {
            AppSettings settings = AppSettings.getInstance(this);

            Cursor tweets = HomeDataSource.getInstance(this).getCursor(settings.currentAccount);
            PutDataMapRequest dataMap = PutDataMapRequest.create(KeyProperties.PATH);
            ArrayList<String> titles = new ArrayList<String>();
            ArrayList<String> bodies = new ArrayList<String>();
            ArrayList<String> ids = new ArrayList<String>();

            if (tweets != null && tweets.moveToLast()) {
                do {
                    String name = tweets.getString(tweets.getColumnIndex(HomeSQLiteHelper.COLUMN_NAME));
                    String handle = tweets.getString(tweets.getColumnIndex(HomeSQLiteHelper.COLUMN_SCREEN_NAME));
                    String body = tweets.getString(tweets.getColumnIndex(HomeSQLiteHelper.COLUMN_TEXT));
                    long id = tweets.getLong(tweets.getColumnIndex(HomeSQLiteHelper.COLUMN_TWEET_ID));

                    titles.add(name);
                    body = handle + KeyProperties.DIVIDER + body + KeyProperties.DIVIDER;
                    bodies.add(Html.fromHtml(body.replace("<p>", KeyProperties.LINE_BREAK)).toString());
                    ids.add(id + "");
                } while (tweets.moveToPrevious() && tweets.getCount() - tweets.getPosition() < MAX_ARTICLES_TO_SYNC);
                tweets.close();
            }

            dataMap.getDataMap().putStringArrayList(KeyProperties.KEY_TITLE, titles);
            dataMap.getDataMap().putStringArrayList(KeyProperties.KEY_TWEET, bodies);
            dataMap.getDataMap().putStringArrayList(KeyProperties.KEY_ID, ids);
            if (settings.addonTheme) {
                dataMap.getDataMap().putInt(KeyProperties.KEY_PRIMARY_COLOR, settings.accentInt);
                dataMap.getDataMap().putInt(KeyProperties.KEY_ACCENT_COLOR, settings.accentInt);
            } else {
                dataMap.getDataMap().putInt(KeyProperties.KEY_PRIMARY_COLOR, getResources().getColor(R.color.orange_primary_color));
                dataMap.getDataMap().putInt(KeyProperties.KEY_ACCENT_COLOR, getResources().getColor(R.color.orange_primary_color));
            }
            dataMap.getDataMap().putLong(KeyProperties.KEY_DATE, System.currentTimeMillis());

            for (String node : wearableUtils.getNodes(googleApiClient)) {
                byte[] bytes = dataMap.asPutDataRequest().getData();
                Wearable.MessageApi.sendMessage(googleApiClient, node, KeyProperties.PATH, bytes);
                Log.v(TAG, "sent " + bytes.length + " bytes of data to node " + node);
            }
        } /*else if (message.startsWith(KeyProperties.MARK_READ_MESSAGE)) {
            String[] messageContent = message.split(KeyProperties.DIVIDER);
            ArticleHelper helper = new ArticleHelper(this);
            helper.markArticleAsRead(Long.parseLong(messageContent[1]));
        } else if (message.startsWith(KeyProperties.REQUEST_IMAGE)) {
            String[] messageContent = message.split(KeyProperties.DIVIDER);
            ArticleHelper helper = new ArticleHelper(this);
            Cursor articles = helper.getArticlesForAuthor(messageContent[1]);

            if (articles != null && articles.moveToFirst()) {
                String url = new ArticleItem().fillFromCursor(articles).getAuthorUrl();
                File f = new File(Utils.CACHE_DIR + "/" + DigestUtils.shaHex(url));
                Log.v(TAG, url + " " + f.getPath());
                if (f.exists()) {
                    Bitmap image = BitmapFactory.decodeFile(f.getPath());
                    if (image != null) {
                        PutDataMapRequest dataMap = PutDataMapRequest.create(KeyProperties.PATH);
                        byte[] bytes = new IoUtils().convertToByteArray(image);
                        dataMap.getDataMap().putByteArray(KeyProperties.KEY_IMAGE_DATA, bytes);
                        dataMap.getDataMap().putString(KeyProperties.KEY_IMAGE_NAME, messageContent[1]);
                        for (String node : wearableUtils.getNodes(googleApiClient)) {
                            Wearable.MessageApi.sendMessage(googleApiClient, node, KeyProperties.PATH, dataMap.asPutDataRequest().getData());
                            Log.v(TAG, "sent " + bytes.length + " bytes of data to node " + node);
                        }
                    }
                }

                articles.close();
            }
        }*/ else {
            Log.e(TAG, "message not recognized");
        }
    }
}
