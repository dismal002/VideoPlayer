package com.softwinner.fireplayer.provider;

import android.content.SearchRecentSuggestionsProvider;

public class MySuggestionProvider extends SearchRecentSuggestionsProvider {
    public static final String AUTHORITY = "com.softwinner.fireplayer.MySuggestionProvider";
    public static final int MODE = 3;

    public MySuggestionProvider() {
        setupSuggestions(AUTHORITY, 3);
    }
}
