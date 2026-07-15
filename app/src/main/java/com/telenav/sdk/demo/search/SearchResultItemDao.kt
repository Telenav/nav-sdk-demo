package com.telenav.sdk.examples

import android.location.Location
import com.telenav.sdk.demo.search.EvConnectorItem
import com.telenav.sdk.map.direction.model.Address
import com.telenav.sdk.map.direction.model.LineLocationReference

data class SearchResultItemDao(
    var displayLocation: Location,
    var navLocation: Location?,
    var address: Address?,
    /** POI name or primary title */
    val name: String,
    /** Street / city address line */
    val addressLine: String,
    /** Phone, category, rating, hours, etc. */
    val detailLine: String,
    /** Short label for map pin */
    val displayText: String,
    /** Distance from search center in miles. */
    val distance: Double,
    val lineLocationReference: LineLocationReference? = null,
    /** True when the result comes from Google search; false for Telenav (TN) results. */
    val fromGoogle: Boolean = false,
    /** Entity id for detail lookup (Google ChIJ… or TN id). */
    val entityId: String = "",
    /** Average rating when available from search facets. */
    val rating: Double? = null,
    val category: String = "",
    val priceLevel: String = "",
    val openStatusLabel: String = "",
    val isOpenNow: Boolean? = null,
    val closingTimeLabel: String = "",
    val evConnectors: List<EvConnectorItem> = emptyList()
)
