package com.telenav.sdk.demo.search

import android.util.Log
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.Price
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object FuelPriceExtractor {

    private const val TAG = "FuelPriceExtractor"

    fun fuelPrices(entity: Entity): List<FuelPriceItem> {
        val priceInfo = entity.facets?.priceInfo
        val priceDetails = priceInfo?.priceDetails.orEmpty()
        if (priceDetails.isEmpty()) {
            Log.i(
                TAG,
                "no fuel prices: entityId=${entity.id} priceLevel=${priceInfo?.priceLevel} " +
                    "priceDetails=null or empty"
            )
            return emptyList()
        }

        val items = priceDetails.mapNotNull { toItem(it) }
        Log.i(
            TAG,
            "fuel prices parsed: entityId=${entity.id} raw=${priceDetails.size} mapped=${items.size} " +
                items.joinToString(" | ") { it.displayLine }
        )
        return items
    }

    private fun toItem(price: Price): FuelPriceItem? {
        val label = price.label?.takeIf { it.isNotBlank() }
            ?: price.type?.takeIf { it.isNotBlank() }
            ?: return null
        val displayLine = formatDisplayLine(label, price)
        return FuelPriceItem(label = label, displayLine = displayLine)
    }

    private fun formatDisplayLine(label: String, price: Price): String {
        val amountText = formatAmount(price)
        val unit = price.unit?.takeIf { it.isNotBlank() }
        val priceText = if (unit != null) "$amountText/$unit" else amountText
        val updatedAt = price.lastUpdateTime?.let(::formatLastUpdateTime)
        return if (updatedAt != null) {
            "$label: $priceText · updated $updatedAt"
        } else {
            "$label: $priceText"
        }
    }

    private fun formatAmount(price: Price): String {
        val amount = price.amount ?: return "—"
        val formatted = String.format(Locale.US, "%.3f", amount).trimEnd('0').trimEnd('.')
        val symbol = price.symbol?.takeIf { it.isNotBlank() }
        if (symbol != null) {
            return if (symbol.length == 1 || symbol.endsWith("$")) {
                "$symbol$formatted"
            } else {
                "$formatted $symbol"
            }
        }
        val currencyCode = price.currency?.currencyCode
        return if (currencyCode != null) {
            "$formatted $currencyCode"
        } else {
            formatted
        }
    }

    private fun formatLastUpdateTime(epochSeconds: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return formatter.format(Date(epochSeconds * 1000L))
    }
}
