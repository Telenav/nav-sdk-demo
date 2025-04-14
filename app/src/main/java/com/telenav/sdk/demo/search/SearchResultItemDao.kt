package com.telenav.sdk.examples

import android.location.Location
import com.telenav.sdk.map.direction.model.Address
import com.telenav.sdk.map.direction.model.LineLocationReference

data class SearchResultItemDao(
    var displayLocation: Location,
    var navLocation: Location?,
    var address: Address?,
    val displayText: String,
    val distance: Double,
    val lineLocationReference: LineLocationReference? = null
)