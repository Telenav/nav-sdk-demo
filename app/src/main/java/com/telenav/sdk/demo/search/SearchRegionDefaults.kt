package com.telenav.sdk.demo.search

/**
 * Default search center per region — aligned with [com.telenav.sdk.demo.SimulationLocationProvider].
 */
object SearchRegionDefaults {

    fun defaultLocation(region: String): Pair<Double, Double> = when (region) {
        "EU" -> 50.10215257 to 8.681829184          // Frankfurt
        "CN" -> 31.2059238 to 121.3985708
        "TW" -> 25.03924079 to 121.516744
        "KR" -> 37.5335715 to 126.972063
        "SEA" -> -6.2033775 to 106.8447530
        "MEA" -> 25.1977404 to 55.2694173
        "ANZ" -> -33.833708 to 151.213987
        "SA" -> -15.78 to -47.88
        "PAK" -> 33.70576551218519 to 73.0470588444717
        "ISC" -> 27.720505464188587 to 85.32304234841881
        "ISR" -> 31.7683 to 35.2137
        else -> 37.398762 to -121.977216            // Telenav US HQ
    }
}
