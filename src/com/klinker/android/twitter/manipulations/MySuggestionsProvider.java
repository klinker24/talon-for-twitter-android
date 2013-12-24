package com.klinker.android.twitter.manipulations;

import android.content.SearchRecentSuggestionsProvider;

public class MySuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.klinker.android.MySuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public MySuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}