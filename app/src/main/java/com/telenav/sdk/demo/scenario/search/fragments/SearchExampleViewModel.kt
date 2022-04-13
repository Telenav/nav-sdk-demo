package com.telenav.sdk.demo.scenario.search.fragments

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.map.api.controllers.SearchController
import com.telenav.map.api.search.PoiAnnotationFactory
import com.telenav.map.api.search.SearchEngine
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.MMFeedbackInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.drivesession.model.StreetInfo

/**
 * @author Mykola Ivantsov - (p)
 */
class SearchExampleViewModel : ViewModel(), PositionEventListener {

    private val vehicleLocation = MutableLiveData<Location>()
    private val chosenSearchItemMap = HashMap<Int, String>()
    fun put(id: Int, value: String) {
        chosenSearchItemMap[id] = value
    }

    fun getAllSearchItemList() = chosenSearchItemMap.values.toList()
    fun remove(key: Int) {
        chosenSearchItemMap.remove(key)
    }

    val startLocation: Location by lazy {
        createLocation(48.113520, 11.572267)
    }
    val stopLocation: Location by lazy {
        createLocation(37.351183, -121.970336)
    }

    fun getVehicleLocationLiveData(): LiveData<Location> = vehicleLocation

    fun injectPoiAnnotationFactory(
        searchController: SearchController,
        poiAnnotationFactory: PoiAnnotationFactory
    ) {
        searchController.injectPoiAnnotationFactory(poiAnnotationFactory)
    }

    fun injectPoiAnnotationFactory(searchController: SearchController, searchEngine: SearchEngine) {
        searchController.injectSearchEngine(searchEngine)
    }

    override fun onLocationUpdated(vehicleLocationIn: Location) {
        vehicleLocation.postValue(vehicleLocationIn)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        //do nothing
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        //do nothing
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
        //do nothing
    }

    fun createLocation(latitude: Double, longitude: Double): Location {
        return Location("MOCK").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }
}