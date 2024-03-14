package com.telenav.sdk.examples.base

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.sdk.examples.case.SetMapViewOnTouchListenerUseCase
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator

/**
 * a basic [Fragment] for Map
 * @author wuchangzhong on 2022/11/24
 */
abstract class BaseMapFragment : Fragment(), PositionEventListener {

    val TAG = this.javaClass.simpleName
    var flag : Boolean = true
    lateinit var mapView: MapView
    val driveSession: DriveSession = DriveSession.Factory.createInstance()
    var vehicleLocation: Location? = null
    lateinit var locationProvider: DemoLocationProvider
    val setMapViewOnTouchListener = SetMapViewOnTouchListenerUseCase()
    val onTouchMapListener = TouchListener { touchType: TouchType, data: TouchPosition ->
        when (touchType) {

            TouchType.Down, TouchType.Click, TouchType.Move, TouchType.Cancel -> {
            }

            TouchType.LongClick -> {
                val location = data.geoLocation
                setVehicleNewLocation(location)
            }
        }
    }

    init {
        driveSession.eventHub?.let {
            it.addPositionEventListener(this)
        }
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        mapView.featuresController().traffic().setEnabled()
        mapView.featuresController().landmarks().setEnabled()
        mapView.featuresController().buildings().setEnabled()
        mapView.featuresController().terrain().setEnabled()
        mapView.featuresController().globe().setDisabled()
        mapView.featuresController().compass().setEnabled()
        mapView.featuresController().scaleBar().setEnabled()
        mapView.layoutController().setVerticalOffset(-0.5)

        // recenter to vehicle position
        mapView.cameraController().position =
            Camera.Position.Builder().setLocation(locationProvider.getLastKnownLocation()).build()
    }

   open abstract fun findMapView():MapView


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        driveSession.injectLocationProvider(locationProvider)
        initMapView()
    }

    open fun initMapView(){
         mapView = findMapView()
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        mapView.initialize(mapViewConfig)
        setMapViewOnTouchListener(mapView, true, onTouchMapListener)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        flag = true
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        flag = false
    }

    override fun onDestroy() {
        super.onDestroy()
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        super.onDestroy()
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        this.vehicleLocation = vehicleLocation
        if (flag){
            mapView.getVehicleController()?.setLocation(vehicleLocation)
        }
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        Log.i(TAG, "onCandidateRoadDetected: ")
    }

    private fun setVehicleNewLocation(location: Location?) {
        if (location != null) {
            locationProvider.setLocation(location)
        }
    }

}