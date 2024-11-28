/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo

import android.content.Context
import android.location.Location
import com.telenav.sdk.common.model.LocationExtrasInfo
import com.telenav.sdk.common.model.LocationProvider
import java.util.*

/**
 * This location provider can used to change vehicle location
 */
class SimulationLocationProvider(val context: Context) : LocationProvider(NAME) {
    private var location: Location? = null

    companion object {
        const val NAME = "Simulation-provider"
    }

    init {
        location = Location(NAME).apply {
            this.latitude = 37.3982607
            this.longitude = -121.9782241
            this.time = Calendar.getInstance().timeInMillis
        }
    }

    override fun onStart() {
        location?.let {
            updateLocation(it)
        }
    }

    override fun onStop() {
    }

    fun setLocation(location: Location, extras: LocationExtrasInfo? = null) {
        if (location.latitude != this.location?.latitude || location.longitude != this.location?.longitude || location.bearing != this.location?.bearing) {
            this.location = location
            updateLocation(location, extras)
        }
    }

    fun getLastKnownLocation(): Location = location ?: Location(NAME)

}