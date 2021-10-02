/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo

import android.content.Context
import android.location.Location
import com.telenav.sdk.common.model.LocationProvider
import java.util.*
import kotlin.collections.ArrayList

/**
 * This location provider can used to change vehicle location
 */
class SimulationLocationProvider(val context: Context) : LocationProvider {
    private val listeners = ArrayList<LocationProvider.LocationUpdateListener>()
    private var state = LocationProvider.Status.OUTOF_SERVICE
    private var location: Location? = null

    companion object {
        const val NAME = "Simulation-provider"
    }

    init {
        location = Location(NAME).apply {
            /*
            //  Telenav-US HQ:
            this.latitude = 37.3982607
            this.longitude = -121.9782241
             */

            this.latitude = 37.4194955
            this.longitude = -122.13814
            this.time = Calendar.getInstance().timeInMillis
        }
    }

    fun start() {
        state = LocationProvider.Status.NORMAL
    }

    fun stop() {
        state = LocationProvider.Status.OUTOF_SERVICE
    }

    fun setLocation(location: Location) {
        if (location.latitude != this.location?.latitude || location.longitude != this.location?.longitude || location.bearing != this.location?.bearing) {
            this.location = location
            notifyLocationChanged()
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