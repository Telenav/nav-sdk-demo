/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.telenav.sdk.common.model.LocationProvider

/**
 * @author zhai.xiang on 2021/4/1
 */
class RealLocationProvider(val context: Context) : DemoLocationProvider {
    private val listeners = ArrayList<LocationProvider.LocationUpdateListener>()
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var status : LocationProvider.Status = LocationProvider.Status.OUTOF_SERVICE
    private val locationListener : LocationListener
    private var provider : String = LocationManager.GPS_PROVIDER

    companion object{
        const val NAME = "GPS-provider"
        const val PROVIDER = LocationManager.GPS_PROVIDER
    }

    @SuppressLint("MissingPermission")
    override fun start(){
        if (isPermissionGranted()) {
            val minDistanceMeters = 2.0f
            val list = locationManager.getProviders(true)
            if (list.contains(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER
            } else if (list.contains(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER
            }
            locationManager.requestLocationUpdates(provider, 1000, minDistanceMeters, locationListener)
        }
    }

    override fun stop(){
        locationManager.removeUpdates(locationListener)
    }

    override fun setLocation(location: Location) {
    }


    init {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                notifyLocationChanged(location)
            }

            override fun onProviderDisabled(provider: String) {
                status = LocationProvider.Status.OUTOF_SERVICE
            }

            override fun onProviderEnabled(provider: String) {
                status = LocationProvider.Status.NORMAL
            }
        }
    }

    override fun getName(): String = NAME


    private fun notifyLocationChanged(location: Location) {
        listeners.forEach {
            it.onLocationChanged(location)
        }
    }

    override fun getStatus(): LocationProvider.Status {
        return status
    }

    @SuppressLint("MissingPermission")
    override fun getLastKnownLocation(): Location {
        return if (isPermissionGranted()) {
            locationManager.getLastKnownLocation(PROVIDER) ?: Location(NAME)
        }else{
            Location(NAME)
        }
    }

    private fun isPermissionGranted() : Boolean{
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun addLocationUpdateListener(listener: LocationProvider.LocationUpdateListener) {
        listeners.add(listener)
        listener.onLocationChanged(lastKnownLocation)
    }

    override fun removeLocationUpdateListener(listener: LocationProvider.LocationUpdateListener) {
        listeners.remove(listener)
    }

}