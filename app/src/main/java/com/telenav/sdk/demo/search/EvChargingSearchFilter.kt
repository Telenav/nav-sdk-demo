package com.telenav.sdk.demo.search

import com.telenav.sdk.entity.model.base.EvFilter

/** User-selected EV filters for category **771** (charging stations). */
data class EvChargingSearchFilter(
    val connectorTypeIds: List<String> = emptyList(),
    val minPowerKw: Double? = null
) {
    fun toEvFilterOrNull(): EvFilter? {
        val connectorTypes = connectorTypeIds.map { it.trim() }.filter { it.isNotEmpty() }
        val hasConnector = connectorTypes.isNotEmpty()
        val hasPower = minPowerKw != null && minPowerKw > 0
        if (!hasConnector && !hasPower) return null
        val builder = EvFilter.builder()
        if (hasConnector) {
            builder.setConnectorTypes(connectorTypes)
        }
        if (hasPower) {
            builder.setMinPower(minPowerKw)
        }
        return builder.build()
    }
}

object EvChargingCategory {
    const val CATEGORY_ID = "771"
}
