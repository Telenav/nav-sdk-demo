/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @author tang.hui
 */
class WhereamiViewModel : ViewModel() {

    val showNavigationDetails = MutableLiveData<Boolean>(false)
    val currentStreetName = MutableLiveData<String>()
    val compassDirectionLiveData = MutableLiveData<String>()
    val roadStateLiveData = MutableLiveData<String>()
    val countryLiveData = MutableLiveData<String>()
    val countyLiveData = MutableLiveData<String>()
    val nextStreetName = MutableLiveData<String>()
    val nextStreetDistanceToVehicle = MutableLiveData<String>()
    val previousStreetName = MutableLiveData<String>()
    val previousStreetDistanceToVehicle = MutableLiveData<String>()
    val closetStreetName= MutableLiveData<String>()
    val closetStreetDistance = MutableLiveData<String>()

    fun onLocationUpdated(vehicleLocation: Location) {
        compassDirectionLiveData.postValue("compass:" + getCompassDirection(vehicleLocation.bearing))
    }

    private fun getCompassDirection(degree: Float): String {
        return when {
            degree >= 347.5 || degree < 22.5 -> "N"
            degree >= 22.5 && degree < 67.5 -> "NE"
            degree >= 67.5 && degree < 112.5 -> "E"
            degree >= 112.5 && degree < 157.5 -> "SE"
            degree >= 157.5 && degree < 202.5 -> "S"
            degree >= 202.5 && degree < 247.5 -> "SW"
            degree >= 247.5 && degree < 302.5 -> "W"
            degree >= 302.5 && degree < 347.5 -> "NW"
            else -> "--"
        }
    }

}


