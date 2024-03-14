package com.telenav.sdk.examples.scenario.navigation.realReach

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
/*
 *  Copyright © 2022 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

/**
 * @author wu.changzhong on 2022/3/23
 */
class RealReachViewModel : ViewModel() {
    val distance = MutableLiveData(0)
    val requestMode = MutableLiveData(RequestMode.HYBRID)
    val origin = MutableLiveData<GeoLocation>()
    val isochronePoints = MutableLiveData<List<LatLon>?>()
    var realReachType = MutableLiveData(RealReachType.EV)

    fun requestIsochrone() {
        val request = IsochroneRequest.Builder(
            origin.value!!
        ).setMaxDistance(distance.value!!)
            .build()
        val task = DirectionClient.Factory.hybridClient().createIsochroneTask(request, requestMode.value!!)
        task.runAsync { response ->
            Log.d("MapLogsForTestData", "MapLogsForTestData >>>> request isochrone task: $response")
            if (response.response.status == DirectionErrorCode.OK) {
                isochronePoints.postValue(response.response.result?.geometry)
            } else {
                isochronePoints.postValue(null)
            }
            task.dispose()
        }
    }


    fun requestEvIsochrone(){
        val latitude = origin.value?.location?.latitude
        val longitude = origin.value?.location?.longitude

        if (latitude != null && longitude != null){
            val evIsochroneRequest = EvIsochroneRequest(LatLon(latitude, longitude))
            val task = DirectionClient.Factory.hybridClient().createEvIsochroneTask(evIsochroneRequest)
            task.runAsync { response ->
                Log.d("MapLogsForTestData", "MapLogsForTestData >>>> request isochrone task: $response")
                if (response.response.status == DirectionErrorCode.OK) {
                    isochronePoints.postValue(response.response.result?.geometry)
                } else {
                    isochronePoints.postValue(null)
                }
                task.dispose()
            }
        }
    }

}