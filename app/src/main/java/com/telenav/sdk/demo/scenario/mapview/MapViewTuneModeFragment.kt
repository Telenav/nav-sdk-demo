/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.diagnosis.RenderMode
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.provider.DemoLocationProvider
import com.telenav.sdk.demo.provider.SimulationLocationProvider
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.MMFeedbackInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.drivesession.model.StreetInfo
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.fragment_map_view_tune_mode.*
import kotlinx.android.synthetic.main.layout_action_bar.iv_back
import kotlinx.android.synthetic.main.layout_action_bar.tv_title
import kotlinx.android.synthetic.main.layout_content_map_with_text.*
import kotlinx.android.synthetic.main.layout_content_map_with_text.mapView
import kotlinx.android.synthetic.main.layout_content_map_with_text.tv_state
import kotlinx.android.synthetic.main.layout_operation_tune_mode.*
import kotlinx.android.synthetic.main.layout_operation_tune_mode.btn_offset_down
import kotlinx.android.synthetic.main.layout_operation_tune_mode.navButton
import java.util.*

/**
 * This fragment shows how to change the map mode
 * @author zhai.xiang on 2021/1/18
 */
class MapViewTuneModeFragment : Fragment(), PositionEventListener {

    private val startLocation = Location("MapView").apply {
        this.latitude = 37.398762
        this.longitude = -121.977216
        this.bearing = 45.0f
    }

    private val stopLocation = Location("MapView").apply {
        this.latitude = 37.40835858
        this.longitude = -121.967860455
    }

    private val driveSession: DriveSession = DriveSession.Factory.createInstance()
    private var route: Route? = null
    private var navigationSession: NavigationSession? = null
    private var currentVerticalOffset = 0.0
    private var currentVehicleLocation: Location? = null
    private lateinit var locationProvider : DemoLocationProvider

    init {
        driveSession.eventHub?.addPositionEventListener(this)
    }

    override fun onDestroyView() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        locationProvider.stop()
        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_view_tune_mode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(requireContext(), DemoLocationProvider.ProviderType.SIMULATION)
        driveSession.injectLocationProvider(locationProvider)
        (locationProvider as SimulationLocationProvider).setLocation(startLocation)
        locationProvider.start()
        tv_title.text = getString(R.string.title_activity_map_view_tune_mode)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout_map_view_tune.open()
        }
        mapViewInit(savedInstanceState)
        setupDrawerOperations()
        requestDirection(startLocation, stopLocation) {
            navButton.isEnabled = it
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        mapView.vehicleController().setLocation(vehicleLocation)
        currentVehicleLocation = vehicleLocation
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState) {
            mapView.cameraController().position = Camera.Position.Builder().setLocation(startLocation).build()
        }

        setCameraUpdateListener()
    }

    private fun setupDrawerOperations() {
        sc_follow_vehicle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rg_follow_model.visibility = View.VISIBLE
                enableFollowVehicle()
                tv_title_enable_follow_vehicle.text = "Follow Vehicle On"
            } else {
                rg_follow_model.visibility = View.GONE
                tv_title_enable_follow_vehicle.text = "Follow Vehicle Off"
                disableFollowVehicle()
            }
        }

        rg_follow_model.setOnCheckedChangeListener { _, _ ->
            enableFollowVehicle()
        }

        btn_move_to_vehicle_position.setOnClickListener {
            moveToVehiclePosition()
        }

        var navigating = false
        navButton.setOnClickListener {
            navigating = !navigating
            if (navigating) {
                startNavigation(mapView, driveSession)
                navButton.setText(R.string.stop_navigation)
            } else {
                stopNavigation(mapView)
                navButton.setText(R.string.start_navigation)
            }
        }

        btn_offset_top.setOnClickListener {
            setVerticalOffset(-0.1)
        }

        btn_offset_down.setOnClickListener {
            setVerticalOffset(0.1)
        }

        btn_show_follow_mode.setOnClickListener {
            showFollowMode()
        }

        btn_show_region.setOnClickListener {
            showRegion()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    /**
     * This function shows how to enable follow vehicle with mode
     */
    private fun enableFollowVehicle() {
        val model = getSelectedFollowMode()
        val useAutoZoom = getUseAutoZoom()
        mapView.cameraController().enableFollowVehicleMode(model, useAutoZoom)
    }

    /**
     * This function shows how to disable follow vehicle.
     */
    private fun disableFollowVehicle() {
        mapView.cameraController().disableFollowVehicle()
    }

    private fun getSelectedFollowMode(): Camera.FollowVehicleMode =
            when (rg_follow_model.checkedRadioButtonId) {
                R.id.rb_heading_up_3D -> Camera.FollowVehicleMode.HeadingUp
                R.id.rb_north_up -> Camera.FollowVehicleMode.NorthUp
                else -> Camera.FollowVehicleMode.Static
            }

    private fun getUseAutoZoom(): Boolean {
        return sc_use_auto_zoom.isChecked
    }

    /**
     *T his function shows how to move camera to the vehicle.
     */
    private fun moveToVehiclePosition() {
        currentVehicleLocation?.let {
            mapView.cameraController().position = Camera.Position.Builder().setLocation(it).build()
        }
    }

    /**
     * This function shows how to set vertical offset of map view.
     */
    private fun setVerticalOffset(value: Double) {
        currentVerticalOffset += value
        currentVerticalOffset = currentVerticalOffset.coerceAtMost(1.0).coerceAtLeast(-1.0)
        mapView.layoutController().setVerticalOffset(currentVerticalOffset)
    }

    /**
     * This function shows how to get follow mode.
     */
    private fun showFollowMode() {
        activity?.runOnUiThread {
            val status = mapView.mapDiagnosis().mapViewStatus
            val text = when (status.renderModeval) {
                RenderMode.RenderingMode_Invalid -> "Invalid"
                RenderMode.RenderingMode_3D -> "3D"
                RenderMode.RenderingMode_3DNorthUp -> "3D North Up"
                RenderMode.RenderingMode_3DHeadingUp -> "3D Heading Up"
                RenderMode.RenderingMode_2D -> "2D"
                RenderMode.RenderingMode_2DNorthUp -> "2D North Up"
                RenderMode.RenderingMode_2DHeadingUp -> "2D Heading Up"
                else -> ""
            }
            Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * This function shows how to add a listener to update camera state
     */
    private fun setCameraUpdateListener() {
        mapView.addMapViewListener {
            it.cameraLocation
            val text = String.format(
                    Locale.getDefault(),"camera position: [%.4f , %.4f]\nzoom level: %.1f\nrange horizontal: %.3f\nvertical offset %.1f",
                    it.cameraLocation.latitude,
                    it.cameraLocation.longitude,
                    it.zoomLevel,
                    it.rangeHorizontal,
                    currentVerticalOffset, )
            activity?.runOnUiThread{
                tv_state?.text = text
            }
        }
    }

    /**
     * This function shows how to show region of routes.
     */
    private fun showRegion(){
        val routeIds = mapView.routesController().add(listOf(route))
        val region = mapView.routesController().region(routeIds)
        mapView.cameraController().showRegion(region)
    }


    private fun requestDirection(begin: Location, end: Location, result: (Boolean) -> Unit) {
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(begin),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
                .routeCount(1)
                .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                route = response.response.result[0]!!
                activity?.runOnUiThread {
                    result(true)
                }

            } else {
                activity?.runOnUiThread {
                    result(false)
                }
            }
            task.dispose()
        }
    }

    private fun startNavigation(map: MapView, driveSession : DriveSession){
        navigationSession?.stopNavigation()
        val routeIds = map.routesController().add(listOf(route))
        map.routesController().highlight(routeIds[0])
        if (route != null) {
            navigationSession = driveSession.startNavigation(route!!, true, 40.0)
        }
    }

    private fun stopNavigation(map: MapView){
        navigationSession?.stopNavigation()
        map.routesController().clear()
    }
}