package com.telenav.sdk.demo.search

data class WordSuggestionItem(
    val displayText: String,
    /** Full query text to place in the search box when selected. */
    val fullQuery: String
)
