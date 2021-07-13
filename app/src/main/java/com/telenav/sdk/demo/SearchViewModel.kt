package com.telenav.sdk.demo

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.telenav.map.api.Annotation
import com.telenav.sdk.common.model.Earth.distance
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.core.Callback
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.utils.EntityJsonConverter
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.demo.util.AndroidThreadUtils

class SearchViewModel(val app : Application) : AndroidViewModel(app) {
    companion object {
        const val TAG = "SearchViewModel"
    }

    val annotation = MutableLiveData<Annotation?>()
    val mutableSelectedLocation = MutableLiveData<SearchResultItemDao>()
    private var adapter : SearchAdapter? = null

    private val locationProvider = DemoLocationProvider.Factory.createProvider(app, DemoLocationProvider.ProviderType.SIMULATION)

    init {
        locationProvider.start()
    }

    fun setAdapter(adapter : SearchAdapter){
        this.adapter = adapter
        adapter.setOnClickListener(object : OnClickedLayoutListener {
            override fun onClickLayout(searchTerm: SearchResultItemDao) {
                mutableSelectedLocation.postValue(searchTerm)
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stop()
    }

    fun entitySearch(searchTerm: String, latitude: Double, longitude: Double) {
        val currentLocation = locationProvider.lastKnownLocation
        val entityClient = EntityService.getClient()
        entityClient.searchRequest()
            .setQuery(searchTerm)
            .setLocation(currentLocation.latitude, currentLocation.longitude)
            .asyncCall(object : Callback<EntitySearchResponse> {

                override fun onSuccess(response: EntitySearchResponse) {
                    getSearchResult(response,latitude,longitude)
                }

                override fun onFailure(t: Throwable) {
                    Log.e(
                        TAG,
                        "Get unsuccessful response or throwable happened when executing the request.",
                        t
                    )
                }
            })
    }

    private fun getSearchResult(
        response: EntitySearchResponse,
        latitude: Double,
        longitude: Double
    ) {
        AndroidThreadUtils.runOnUiThread(Runnable {
            val annotations: MutableList<SearchResultItemDao> = mutableListOf()
            // log response in JSON format
            Log.i(TAG, EntityJsonConverter.toPrettyJson(response))

            val resultEntities = response.results
            if (resultEntities == null || resultEntities.isEmpty()) {
                Log.i(TAG, "No result found")
            }
            for (entity in resultEntities) {
                var name = "Entity"
                var location: Location? = null
                if (entity.type == EntityType.ADDRESS) {
                    Log.i(TAG, "Found Address: " + entity.address.formattedAddress)
                    name = entity.address.formattedAddress

                    val thisLocation = Location("")
                    thisLocation.latitude = entity.address.geoCoordinates.latitude
                    thisLocation.longitude = entity.address.geoCoordinates.longitude
                    location = thisLocation
                } else if (entity.type == EntityType.PLACE) {
                    Log.i(TAG, "Found Place: " + entity.place.name)
                    name = entity.place.name+"\n${entity.place.address.formattedAddress}"

                    val thisLocation = Location("")
                    thisLocation.latitude = entity.place.address.geoCoordinates.latitude
                    thisLocation.longitude = entity.place.address.geoCoordinates.longitude
                    location = thisLocation
                }

                if (location != null) {
                    val start = LatLon(location.latitude,location.longitude)
                    val end = LatLon(latitude,longitude)
                    val dist = ("%.2f".format(distance(start,end)/1000)).toDouble()
                    val searchResultDao = SearchResultItemDao(location, name, dist)
                    annotations.add(searchResultDao)

                }
            }
            adapter?.setSearchData(annotations)
        })
    }
}

