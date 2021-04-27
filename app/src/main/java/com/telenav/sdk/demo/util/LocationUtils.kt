package com.telenav.sdk.demo.util

import android.location.Location
import com.telenav.sdk.common.model.Region
import com.telenav.sdk.demo.provider.SimulationLocationProvider
import java.util.*

/**
 * @author zhai.xiang on 2021/4/27
 */
object LocationUtils {

    fun getLocationByRegion(region: Region = Region.NA) : Location{
        val defaultLat : Double
        val defaultLon : Double
        when (region) {
            Region.EU -> {
                //  city center of "Frankfurt, Germany":
                defaultLat = 50.10215257
                defaultLon = 8.681829184
            }
            Region.CN -> {
                //  Telenav CN HQ:
                defaultLat = 31.2059238
                defaultLon = 121.3985708
            }
            Region.TW -> {
                //  City center of Taipei, Taiwan:
                defaultLat = 25.03924079
                defaultLon = 121.516744
            }
            Region.KR -> {
                //  City center of Soul, Korean:
                defaultLat = 37.5335715
                defaultLon = 126.972063
            }
            else -> {
                // Telenav US HQ
                defaultLat = 37.398762
                defaultLon = -121.977216
            }
        }
        return Location(SimulationLocationProvider.NAME).apply {
            this.latitude = defaultLat
            this.longitude = defaultLon
            this.time = Calendar.getInstance().timeInMillis
        }
    }
}