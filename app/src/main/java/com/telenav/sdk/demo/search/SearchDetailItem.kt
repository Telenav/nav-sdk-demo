package com.telenav.sdk.demo.search

import com.telenav.sdk.examples.SearchResultItemDao

data class SearchDetailReview(
    val author: String,
    val rating: Double,
    val text: String,
    val timeLabel: String
)

data class SearchDetailItem(
    val entityId: String,
    val name: String,
    val addressLine: String,
    val distanceMiles: Double,
    val fromGoogle: Boolean,
    val category: String,
    val phone: String,
    val hours: String,
    val rating: Double?,
    val ratingCount: Int?,
    val priceLevel: String,
    val openStatusLabel: String,
    val isOpenNow: Boolean?,
    val closingTimeLabel: String,
    val hoursSchedule: String,
    val photoUrls: List<String>,
    val reviews: List<SearchDetailReview>,
    val evConnectors: List<EvConnectorItem> = emptyList(),
    val fuelPrices: List<FuelPriceItem> = emptyList(),
    val navigationItem: SearchResultItemDao
)
