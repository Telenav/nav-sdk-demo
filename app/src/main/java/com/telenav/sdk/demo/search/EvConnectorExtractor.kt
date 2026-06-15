package com.telenav.sdk.demo.search

import com.telenav.sdk.entity.model.base.Connector
import com.telenav.sdk.entity.model.base.Entity
import java.util.Locale
import kotlin.math.abs

object EvConnectorExtractor {

    fun connectors(entity: Entity): List<EvConnectorItem> {
        val raw = entity.facets?.evConnectors?.connectors.orEmpty()
        if (raw.isEmpty()) return emptyList()
        return raw.mapNotNull { toItem(it) }
    }

    private fun toItem(connector: Connector): EvConnectorItem? {
        val typeName = connector.connectorType?.name?.trim().orEmpty()
        val typeLabel = formatConnectorType(typeName)
        val powerLabel = formatPower(connector.maxPower)
        if (typeLabel.isBlank() && powerLabel.isBlank()) return null

        val total = resolveTotal(connector)
        val available = resolveAvailable(connector, total)
        return EvConnectorItem(
            typeLabel = typeLabel,
            powerLabel = powerLabel,
            available = available,
            total = total
        )
    }

    private fun resolveTotal(connector: Connector): Int {
        val connectorNumber = connector.connectorNumber ?: 0
        if (connectorNumber > 0) return connectorNumber
        val available = connector.available ?: 0
        val inuse = connector.inuse ?: 0
        if (available > 0 || inuse > 0) return (available + inuse).coerceAtLeast(0)
        return 0
    }

    private fun resolveAvailable(connector: Connector, total: Int): Int {
        val available = connector.available
        if (available != null && available >= 0) return available.coerceAtMost(total.takeIf { it > 0 } ?: available)
        val inuse = connector.inuse ?: 0
        if (total > 0 && inuse >= 0) return (total - inuse).coerceAtLeast(0)
        return 0
    }

    fun formatConnectorType(rawName: String): String {
        if (rawName.isBlank()) return ""
        return rawName
            .trim()
            .replace('_', ' ')
            .lowercase(Locale.getDefault())
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
    }

    fun formatPower(maxPower: Double?): String {
        if (maxPower == null || abs(maxPower) < 0.000_001) return ""
        val rounded = (maxPower * 10.0).toInt() / 10.0
        val display = if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.1f", rounded)
        }
        return "$display kW"
    }
}
