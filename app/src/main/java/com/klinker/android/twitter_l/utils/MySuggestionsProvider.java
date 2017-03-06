package com.klinker.android.twitter_l.utils;
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
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;
import android.provider.SearchRecentSuggestions;

import com.lapism.searchview.adapter.SearchItem;

import java.util.ArrayList;
import java.util.List;

public class MySuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.klinker.android.twitter_l.MySuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public MySuggestionsProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
    }

    public List<SearchItem> query(Context context, String query) {
        Cursor c = context.getContentResolver().query(
                Uri.parse("content://" + AUTHORITY + "/suggestions"),
                SearchRecentSuggestions.QUERIES_PROJECTION_1LINE,
                "mentionsQuery LIKE '%" + query + "%'",
                null,
                "date DESC LIMIT 250"
        );

        List<SearchItem> items = new ArrayList();

        if (c.moveToFirst()) {
            do {
                items.add(new SearchItem(c.getString(c.getColumnIndex("mentionsQuery"))));
            } while (c.moveToNext());
        }

        try {
            c.close();
        } catch (Exception e) {

        }

        return items;
    }

    public List<SearchItem> getAllSuggestions(Context context) {
        Cursor c = context.getContentResolver().query(
                Uri.parse("content://" + AUTHORITY + "/suggestions"),
                SearchRecentSuggestions.QUERIES_PROJECTION_1LINE,
                null,
                null,
                "date DESC LIMIT 250"
        );

        List<SearchItem> items = new ArrayList();

        if (c.moveToFirst()) {
            do {
                items.add(new SearchItem(c.getString(c.getColumnIndex("query"))));
            } while (c.moveToNext());
        }

        try {
            c.close();
        } catch (Exception e) {

        }

        return items;
    }
}
