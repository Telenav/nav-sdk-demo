/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.provider

import android.content.Context
import android.location.Location
import com.telenav.sdk.common.model.LocationExtrasInfo
import com.telenav.sdk.common.model.Region
import com.telenav.sdk.examples.util.RegionCachedHelper
import java.util.Calendar

/**
 * This location provider can used to change vehicle location
 */
class SimulationLocationProvider(val context: Context) : DemoLocationProvider(NAME) {
    private var location: Location? = null

    companion object {
        const val NAME = "Simulation-provider"
    }

    init {
        setLocationByRegion(RegionCachedHelper.getRegion(context))
    }

    override fun onStart() {
        location?.let {
            updateLocation(it)
        }
    }

    override fun onStop() {
    }

    override fun setLocation(location: Location, extras: LocationExtrasInfo?) {
        if (location.latitude != this.location?.latitude || location.longitude != this.location?.longitude || location.bearing != this.location?.bearing) {
            this.location = location
            updateLocation(location, extras)
        }
    }

    fun setLocationByRegion(region: Region) {
        val defaultLat: Double
        val defaultLon: Double
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
            Region.SEA -> {
                //  City center of Jakarta, Indonesia:
                defaultLat = -6.2033775
                defaultLon = 106.8447530
            }
            Region.MEA -> {
                //  City center of Dubai, United Arab Emirates
                defaultLat = 25.1977404
                defaultLon = 55.2694173
            }
            Region.ANZ -> {
                //  City center of Sydney
                defaultLat = -33.833708
                defaultLon = 151.213987
            }
            Region.SA ->{
                //  City center of Brazil，Brasilia
                defaultLat = -15.78
                defaultLon = -47.88
            }
            Region.PAK ->{
                //  City center of Islamabad
                defaultLat = 33.70576551218519
                defaultLon = 73.0470588444717
            }
            Region.ISC ->{
                //  City center of kathmandu
                defaultLat = 27.720505464188587
                defaultLon = 85.32304234841881
            }
            Region.ISR ->{
                //  City center of Jerusalem
                defaultLat = 31.7683
                defaultLon = 35.2137
            }
            else -> {
                // Telenav US HQ
                defaultLat = 37.398762
                defaultLon = -121.977216
            }
        }
        location = Location(NAME).apply {
            this.latitude = defaultLat
            this.longitude = defaultLon
            this.time = Calendar.getInstance().timeInMillis
        }
    }

    override fun getLastKnownLocation(): Location = location ?: Location(NAME)
}