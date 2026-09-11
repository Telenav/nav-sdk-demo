package com.telenav.sdk.demo.search

data class AutocompleteItem(
    val entityId: String,
    val label: String,
    val fromGoogle: Boolean = false,
    /** Distance from search center in meters (API or computed); 0 when unknown. */
    val distanceMeters: Double = 0.0
)
