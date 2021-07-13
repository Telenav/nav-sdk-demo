/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.provider

import android.content.Context
import android.location.Location
import androidx.annotation.RawRes
import com.telenav.sdk.common.model.LocationProvider
import com.telenav.sdk.examples.R
import com.telenav.sdk.demo.util.KmlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author zhai.xiang on 2021/4/2
 */
class FileLocationProvider(val context: Context) : DemoLocationProvider {
    private val listeners = ArrayList<LocationProvider.LocationUpdateListener>()
    private val locations = ArrayList<Location>()
    private var timer : Timer? = null
    private var currentIndex = 0
    private var status : LocationProvider.Status = LocationProvider.Status.OUTOF_SERVICE
    private var res : Int? = null

    companion object{
        private const val NAME = "File-location"
        private const val INTERVAL = 250L
    }

    fun setKmlFileRes(@RawRes res : Int){
        this.res = res
    }

    override fun start() {
        this.res = R.raw.route_na
        CoroutineScope(Dispatchers.IO).launch {
            if (res == null){
                return@launch
            }
            currentIndex = 0
            locations.clear()
            locations.addAll(KmlParser.parse(context, res!!))
            if (locations.isNotEmpty()) {
                status = LocationProvider.Status.NORMAL
                timer?.cancel()
                timer = Timer()
                timer?.schedule(timerTask, INTERVAL, INTERVAL)
            }
        }
    }

    private val timerTask = object :TimerTask(){
        override fun run() {
            val location = locations[currentIndex]
            location.time = Calendar.getInstance().timeInMillis
            notifyLocationChanged(location)
            currentIndex = (currentIndex + 1).coerceAtMost(locations.size - 1)
        }
    }
    private fun notifyLocationChanged(location: Location) {
        listeners.forEach {
            it.onLocationChanged(location)
        }
    }

    override fun stop() {
        status = LocationProvider.Status.OUTOF_SERVICE
        timer?.cancel()
        currentIndex = 0
    }

    override fun setLocation(location: Location) {
    }

    override fun getName(): String = NAME

    override fun getStatus(): LocationProvider.Status = status

    override fun getLastKnownLocation(): Location {
        return locations.getOrNull(currentIndex) ?: Location(NAME)
    }

    override fun addLocationUpdateListener(listener: LocationProvider.LocationUpdateListener) {
        listeners.add(listener)
    }

    override fun removeLocationUpdateListener(listener: LocationProvider.LocationUpdateListener) {
        listeners.remove(listener)
    }
}