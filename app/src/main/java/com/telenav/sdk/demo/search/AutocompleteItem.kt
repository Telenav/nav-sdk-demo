package com.telenav.sdk.demo.search

data class AutocompleteItem(
    val entityId: String,
    val label: String,
    val fromGoogle: Boolean = false
)
