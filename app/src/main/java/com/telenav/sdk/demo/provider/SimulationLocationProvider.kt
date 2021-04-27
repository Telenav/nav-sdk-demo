/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.provider

import android.content.Context
import android.location.Location
import com.telenav.sdk.common.model.LocationProvider
import com.telenav.sdk.common.model.Region
import java.util.*
import kotlin.collections.ArrayList

/**
 * This location provider can used to change vehicle location
 */
class SimulationLocationProvider(val context: Context) : DemoLocationProvider {
    private val listeners = ArrayList<LocationProvider.LocationUpdateListener>()
    private var state = LocationProvider.Status.OUTOF_SERVICE
    private var location: Location? = null

    companion object {
        const val NAME = "Simulation-provider"
    }

    init {
        setLocationByRegion(Region.EU)
    }

    override fun start() {
        state = LocationProvider.Status.NORMAL
    }

    override fun stop() {
        state = LocationProvider.Status.OUTOF_SERVICE
    }

    override fun setLocation(location: Location) {
        if (location.latitude != this.location?.latitude || location.longitude != this.location?.longitude || location.bearing != this.location?.bearing) {
            this.location = location
            notifyLocationChanged()
        }
    }

    fun setLocationByRegion(region: Region){
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
        location =  Location(NAME).apply {
            this.latitude = defaultLat
            this.longitude = defaultLon
            this.time = Calendar.getInstance().timeInMillis
        }
    }

    private fun notifyLocationChanged() {
        listeners.forEach {
            it.onLocationChanged(lastKnownLocation)
        }
    }

    override fun getStatus(): LocationProvider.Status = state

    override fun addLocationUpdateListener(listener: LocationProvider.LocationUpdateListener) {
        listeners.add(listener)
        listener.onLocationChanged(lastKnownLocation)
    }

    override fun getName(): String = NAME

    override fun removeLocationUpdateListener(listener: LocationProvider.LocationUpdateListener) {
        listeners.remove(listener)
    }

    override fun getLastKnownLocation(): Location = location ?: Location(NAME)
}