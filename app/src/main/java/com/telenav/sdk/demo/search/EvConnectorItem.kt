package com.telenav.sdk.demo.search

data class EvConnectorItem(
    val typeLabel: String,
    val powerLabel: String,
    val available: Int,
    val total: Int
) {
    val summaryLine: String
        get() = buildList {
            if (typeLabel.isNotBlank()) add(typeLabel)
            if (powerLabel.isNotBlank()) add(powerLabel)
        }.joinToString(" · ")
}
