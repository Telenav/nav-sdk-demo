/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */
package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.controllers.*
import com.telenav.map.views.TnMapView
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.*
import com.telenav.sdk.drivesession.model.drg.BetterRouteContext
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRect
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRegionForRoutesInfo
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRouteRequest
import com.telenav.sdk.examples.scenario.mapview.CVPPositionOnRoadViewModel.Companion.createRoutingTask
import com.telenav.sdk.map.direction.model.Route
import kotlinx.android.synthetic.main.c_v_p_position_on_road_fragment.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
/**
 * This fragment shows how you can get and show the route from CVP to the destination
 * point on the map
 *
 * @author Mykola Ivantsov - (p)
 */
class CVPPositionOnRoadFragment : Fragment(), PositionEventListener, NavigationEventListener {

    companion object {
        fun newInstance() = CVPPositionOnRoadFragment()
        private const val TAG = "CVPPositionOnRoadFragment"
        private const val REACH_DESTINATION = -1 //-1 means reach destination
    }

    private val viewModel: CVPPositionOnRoadViewModel by viewModels()
    private val backListener = View.OnClickListener { _: View ->
        findNavController().navigateUp()
    }
    private val success: (Boolean) -> Unit = {
        runInMain {
            setVisible(btn_remaining_way, View.VISIBLE)
        }
    }
    private val onLoad: () -> Unit = {
        runInMain {
            setVisible(btn_remaining_way, View.GONE)
        }
    }
    private val navigateListener = View.OnClickListener { _: View ->
        viewModel.navigateToLocation(
            getRoutesController(),
            getCameraController(),
            this,
            this,
            viewModel.startLocation,
            viewModel.stopLocation,
            createRoutingTask,
            createRouteRequest,
            success,
            onLoad
        )

    }
    private val remainingWayListener = View.OnClickListener {
        disableFollowVehicle(getCameraController())
        setRenderMode(getCameraController(), Camera.RenderMode.M2D)
        setBearing(mapView, CVPPositionOnRoadViewModel.BEAR, createCameraPosition)
        showActiveRouteInRegion(getRoutesController(), getCameraController())
    }

    private val getAnnotationsController: () -> AnnotationsController = {
        mapView.annotationsController()
    }

    private val getCameraController: () -> CameraController = {
        mapView.cameraController()
    }

    private val getRoutesController: () -> RoutesController = {
        mapView.routesController()
    }

    private val getVehicleController: () -> VehicleController = {
        mapView.vehicleController()
    }

    private val createCameraPosition: (Float) -> Camera.Position = { bear ->
        Camera.Position.Builder().setBearing(bear).build()
    }

    private val mapListener: MapViewReadyListener<MapView> = MapViewReadyListener<MapView> {
        mapView.apply {
            featuresController().traffic().setEnabled()
            featuresController().compass().setEnabled()
            vehicleController().setIcon(R.drawable.cvp)
        }

        runInMain {
            moveCVPToLocation(viewModel.startLocation, getCameraController, getVehicleController)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.c_v_p_position_on_road_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_cvp_position_on_road)
        mapViewInit(savedInstanceState, mapListener)

        setOnClickListener(iv_back, backListener)
        setOnClickListener(btn_navigate, navigateListener)
        setOnClickListener(btn_remaining_way, remainingWayListener)
    }

    private fun setBearing(
        mapView: TnMapView,
        bear: Float,
        createCameraPosition: (Float) -> Camera.Position
    ) {
        createCameraPosition(bear).let { cameraPosition ->
            mapView.cameraController().position = cameraPosition
        }
    }

    private fun setOnClickListener(view: View, onClickListener: View.OnClickListener) {
        view.setOnClickListener(onClickListener)
    }

    private fun setVisible(view: View, visibility: Int) {
        view.visibility = visibility
    }

    private fun setRenderMode(cameraController: CameraController, renderMode: Camera.RenderMode) {
        cameraController.renderMode = renderMode
    }

    // the initialize function must be called after SDK is initialized
    private fun mapViewInit(
        savedInstanceState: Bundle?,
        mapViewReadyListener: MapViewReadyListener<MapView>,
        tnMapView: TnMapView = this.mapView
    ) {
        tnMapView.initialize(savedInstanceState, mapViewReadyListener)
    }

    private fun moveCVPToLocation(
        location: Location,
        cameraController: () -> CameraController,
        vehicleController: () -> VehicleController
    ) {
        cameraController().position = Camera.Position.Builder().setLocation(location).build()
        vehicleController().setLocation(location)
    }

    private fun runInMain(run: () -> Unit): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            run()
        }
    }

    private fun disableFollowVehicle(cameraController: CameraController) {
        cameraController.disableFollowVehicle()
    }

    private fun showActiveRouteInRegion(
        routesController: RoutesController,
        cameraController: CameraController
    ) {
        val regionForRoutesInfo = createRegionForRoutesInfo(
            listOf(viewModel.mapActiveRouteId),
            createRect(
                CVPPositionOnRoadViewModel.X,
                CVPPositionOnRoadViewModel.Y,
                mapRect.width,
                mapRect.height
            ),
            CVPPositionOnRoadViewModel.GRID_ALIGNED,
            CVPPositionOnRoadViewModel.SHOW_FULL_ROUTE_OVERVIEW,
            CVPPositionOnRoadViewModel.INCLUDE_CVP
        )
        viewModel.showActiveRouteInRegion(
            routesController,
            cameraController,
            regionForRoutesInfo
        )
    }

    private fun printDebugLog(msg: String) {
        TaLog.d(TAG, msg)
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        getVehicleController().setLocation(vehicleLocation)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        printDebugLog("onStreetUpdated")
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        printDebugLog("onCandidateRoadDetected")
    }

    override fun onMMFeedbackUpdated(feedback: MMFeedbackInfo) {
        printDebugLog("onMMFeedbackUpdated")
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        printDebugLog("onNavigationEventUpdated")
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
        printDebugLog("onJunctionViewUpdated")
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        printDebugLog("onAlongRouteTrafficUpdated")
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        printDebugLog("onNavigationStopReached: stopIndex=$stopIndex, stopLocation:$stopLocation")
        if (stopIndex == REACH_DESTINATION) {
            getAnnotationsController().clear()
            getRoutesController().clear()
            getCameraController().disableFollowVehicle()
            setVisible(btn_remaining_way, View.GONE)
            viewModel.killNavigationSession()
        }
    }

    override fun onNavigationRouteUpdated(route: Route, betterRouteContext: BetterRouteContext?) {
        route.dispose()
    }

    override fun onNavigationRouteUpdated(route: Route, routeUpdateContext: RouteUpdateContext?) {
        //do nothing
    }

    override fun onBetterRouteInfoUpdated(betterRouteInfo: BetterRouteInfo) {
        betterRouteInfo.betterRouteCandidate?.accept(false)
    }

    override fun onBetterRouteDetected(
        status: NavigationEventListener.BetterRouteDetectionStatus,
        betterRouteCandidate: BetterRouteCandidate?
    ) {
        betterRouteCandidate?.accept(false)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}