package com.telenav.sdk.demo

import android.location.Location

data class SearchResultItemDao(
    val location: Location,
    val displayText: String,
    val distance: Double)