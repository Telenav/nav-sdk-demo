package com.telenav.sdk.demo.search

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.telenav.sdk.common.model.Earth.distance
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.core.Callback
import com.telenav.sdk.demo.SimulationLocationProvider
import com.telenav.sdk.demo.utils.AndroidThreadUtils
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.search.EntitySearchResponse
import com.telenav.sdk.entity.utils.EntityJsonConverter
import com.telenav.sdk.examples.BuildConfig
import com.telenav.sdk.examples.SearchResultItemDao
import com.telenav.sdk.map.direction.model.Address

class SearchViewModel(val app: Application) : AndroidViewModel(app) {
    companion object {
        const val TAG = "SearchViewModel"
    }

    val mutableSelectedLocation = MutableLiveData<SearchResultItemDao>()
    private var adapter: SearchAdapter? = null

    private val locationProvider = SimulationLocationProvider(BuildConfig.Region)

    init {
        locationProvider.onStart()
    }

    fun setAdapter(adapter: SearchAdapter) {
        this.adapter = adapter
        adapter.setOnClickListener(object : OnClickedLayoutListener {
            override fun onClickLayout(itemDao: SearchResultItemDao) {
                mutableSelectedLocation.postValue(itemDao)
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.onStop()
    }

    fun entitySearch(searchTerm: String, latitude: Double, longitude: Double) {
        val currentLocation = locationProvider.getLastKnownLocation()
        val entityClient = EntityService.getClient()
        entityClient.searchRequest()
            .setQuery(searchTerm)
            .setLocation(currentLocation.latitude, currentLocation.longitude)
            .asyncCall(object : Callback<EntitySearchResponse> {

                override fun onSuccess(response: EntitySearchResponse) {
                    getSearchResult(response, latitude, longitude)
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
                var displayLocation = Location("")
                var navLocation = Location("")
                var geoAddress: Address? = null

                if (entity.type == EntityType.ADDRESS) {
                    Log.i(TAG, "Found Address: " + entity.address.formattedAddress)

                    entity?.address?.apply {
                        name = formattedAddress
                        geoCoordinates?.apply {
                            displayLocation.latitude = this.latitude
                            displayLocation.longitude = this.longitude
                        }

                        navCoordinates?.apply {
                            navLocation.latitude = this.latitude
                            navLocation.longitude = this.longitude
                        }

                        geoAddress = Address(street?.formattedName,crossStreet?.formattedName,houseNumber)
                    }
                } else if (entity.type == EntityType.PLACE) {
                    Log.i(TAG, "Found Place: " + entity.place.name)
                    entity?.place?.apply {
                        name = this.name + "\n${entity.place.address.formattedAddress}"
                        address?.apply {
                            geoCoordinates?.apply {
                                displayLocation.latitude = this.latitude
                                displayLocation.longitude = this.longitude
                            }

                            navCoordinates?.apply {
                                navLocation.latitude = this.latitude
                                navLocation.longitude = this.longitude
                            }

                            geoAddress = Address(street?.formattedName,crossStreet?.formattedName,houseNumber)
                        }
                    }
                }
                val end = LatLon(displayLocation.latitude,displayLocation.longitude)
                val start = LatLon(latitude, longitude)
                val dist = ("%.2f".format(distance(start, end) / 1000)).toDouble()

                val searchResultDao = SearchResultItemDao(displayLocation,navLocation,geoAddress, name, dist)
                annotations.add(searchResultDao)
            }
            adapter?.setSearchData(annotations)
        })
    }
}

