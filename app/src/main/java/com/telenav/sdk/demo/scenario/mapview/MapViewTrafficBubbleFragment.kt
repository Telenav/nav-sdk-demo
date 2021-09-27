/*
 * Copyright © 2018 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.map.api.controllers.*
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.model.AlongRouteTraffic
import com.telenav.sdk.drivesession.model.BetterRouteCandidate
import com.telenav.sdk.drivesession.model.JunctionViewInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.drivesession.model.drg.RouteUpdateContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.scenario.mapview.MapViewNavViewModel
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.content.model.TrafficLevel
import com.telenav.sdk.map.direction.model.Route
import com.telenav.sdk.map.direction.model.RouteEdge
import kotlinx.android.synthetic.main.fragment_map_view_traffic_bubble.*
import kotlinx.android.synthetic.main.fragment_map_view_traffic_bubble.ivFix
import kotlinx.android.synthetic.main.fragment_map_view_traffic_bubble.mapView
import kotlinx.android.synthetic.main.layout_action_bar.*


/**
 * This page is to show how to display traffic bubble.
 * The traffic bubble will appear when navigation start. Each traffic flow can create one bubble to show
 * the current traffic status. And the traffic bubble will disappear when the vehicle pass the bubble
 */
class MapViewTrafficBubbleFragment : Fragment(), NavigationEventListener {
    private val viewModel: MapViewNavViewModel by viewModels()
    private var cameraController: CameraController? = null
    private var vehicleController: VehicleController? = null
    private var annotationController: AnnotationsController? = null
    private var routesController: RoutesController? = null
    private var alongRouteTraffic: AlongRouteTraffic? = null
    private val bubbleList = ArrayList<Annotation>()

    companion object {
        const val LEG_INDEX = "legIndex"
        const val STEP_INDEX = "stepIndex"
        const val EDGE_INDEX = "edgeIndex"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_view_traffic_bubble, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }

        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        tv_title.setText(R.string.title_activity_map_view_traffic_bubble)
        mapView.initialize(savedInstanceState) {
            cameraController = mapView.cameraController()
            annotationController = mapView.annotationsController()
            vehicleController = mapView.vehicleController()
            routesController = mapView.routesController()
        }

        trafficBar.setOrientation(false)
        viewModel.driveSession.eventHub.addNavigationEventListener(this)
        initView()
        initObs()
    }

    override fun onDestroyView() {
        viewModel.driveSession.eventHub.removeNavigationEventListener(this)
        super.onDestroyView()
    }

    private fun initView() {
        btnStartNav.setOnClickListener {
            viewModel.startNavigation(mapView)
            btnStartNav.isEnabled = false
            btnStopNav.isEnabled = true
        }

        btnStopNav.setOnClickListener {
            viewModel.stopNavigation(mapView)
            btnStartNav.isEnabled = false
            btnStopNav.isEnabled = false
            annotationController?.clear()
            updateCurrentPosition(0,0,0)
        }

        ivFix.setOnClickListener {
            cameraController?.position = Camera.Position.Builder().setLocation(viewModel.currentVehicleLocation.value!!).build()
        }

        mapView.setOnTouchListener { touchType: TouchType, position: TouchPosition ->
            if (viewModel.isNavigationOn()) {
                return@setOnTouchListener
            }

            if (touchType == TouchType.LongClick) {
                mapView.routesController().clear()
                activity?.runOnUiThread {
                    val context = context
                    if (context != null) {
                        val factory = annotationController!!.factory()
                        val annotation = factory.create(context, R.drawable.map_pin_green_icon_unfocused, position.geoLocation!!)
                        annotation.style = Annotation.Style.ScreenAnnotationFlagNoCulling
                        annotationController?.clear()
                        annotationController?.add(arrayListOf(annotation))
                    }
                }

                viewModel.currentVehicleLocation.value?.let {
                    viewModel.requestDirection(it, position.geoLocation!!) { result ->
                        activity?.runOnUiThread {
                            btnStartNav.isEnabled = result
                        }
                    }
                }
            }
        }
    }

    private fun initObs() {
        viewModel.currentVehicleLocation.observe(viewLifecycleOwner) {
            vehicleController?.setLocation(it)
            // update current state and remove the traffic bubble if the edge has been passed
            it.extras?.let { bundle ->
                val edgeIndex = bundle.getInt("edgeIndex",0)
                val legIndex = bundle.getInt("legIndex",0)
                val stepIndex = bundle.getInt("stepIndex",0)
                updateCurrentPosition(legIndex, stepIndex, edgeIndex)
            }
        }

        viewModel.route.observe(viewLifecycleOwner) { route ->
            routesController?.clear()
            route?.let {
                val ids = routesController!!.add(listOf(it))
                cameraController?.showRegion(routesController!!.region(ids!!), Margins.Percentages(0.2, 0.2))
            }
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

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        trafficBar.updateTraveledDistance(navEvent.traveledDistance.toFloat())
    }

    override fun onJunctionViewUpdated(junctionViewInfo: JunctionViewInfo) {
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        this.alongRouteTraffic = alongRouteTraffic
        refreshBubble(generatePoiModel(alongRouteTraffic))
        trafficBar.updateTrafficInfo(alongRouteTraffic)
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        if (stopIndex == -1) {
            annotationController?.remove(bubbleList)
            bubbleList.clear()
        }
    }

    override fun onNavigationRouteUpdated(route: Route, info: RouteUpdateContext?) {
        viewModel.route.postValue(route)
        updateCurrentPosition(0,0,0)
    }

    override fun onBetterRouteDetected(status: NavigationEventListener.BetterRouteDetectionStatus, betterRouteCandidate: BetterRouteCandidate?) {
    }

    private fun generatePoiModel(alongRouteTraffic: AlongRouteTraffic) : List<PoiModel>{
        val modelList = ArrayList<PoiModel>()
        val edges = generalEdgeList(viewModel.route.value)
        alongRouteTraffic.alongRouteTrafficFlow?.forEach { flow ->
            val startIndex = edges.indexOfFirst {
                it.legIndex == flow.startLegIndex && it.stepIndex == flow.startStepIndex && it.edgeIndex == flow.startEdgeIndex
            }

            val lastIndex = edges.indexOfLast {
                it.legIndex == flow.endLegIndex && it.stepIndex == flow.endStepIndex && it.edgeIndex == flow.endEdgeIndex
            }

            val middleEdge = edges[(startIndex + lastIndex) / 2]
            val locations = (middleEdge.edge.getEdgeShapePoints() ?: emptyList())
            val poi = PoiModel(middleEdge.legIndex, middleEdge.stepIndex, middleEdge.edgeIndex,
                    locations[locations.size / 2], flow.congestionLevel)
            modelList.add(poi)
        }

        return modelList
    }

    private fun refreshBubble(models :List<PoiModel>){
        annotationController?.remove(bubbleList)
        bubbleList.clear()
        models.forEach {
            if (it.congestionLevel != -1 && it.latLon != null) {
                val location = Location("demo").apply {
                    this.latitude = it.latLon.lat
                    this.longitude = it.latLon.lon
                }

                val poi = annotationController!!.factory().create(Annotation.ExplicitStyle.HeavyCongestionBubble, location)
                poi.extraInfo = Bundle().apply {
                    this.putInt(LEG_INDEX, it.legIndex)
                    this.putInt(STEP_INDEX, it.stepIndex)
                    this.putInt(EDGE_INDEX, it.edgeIndex)
                }

                when (it.congestionLevel) {
                    TrafficLevel.CLOSED -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("CLOSED")
                    }

                    TrafficLevel.CONGESTED -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("CONGESTED")
                    }

                    TrafficLevel.SLOW_SPEED -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("SLOW SPEED")
                    }

                    TrafficLevel.QUEUING -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("QUEUING")
                    }

                    TrafficLevel.FREE_FLOW -> {
                        return@forEach
                    }

                    TrafficLevel.HEAVY -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("HEAVY")
                    }

                    else -> {
                        poi.displayText = Annotation.TextDisplayInfo.Centered("UNKNOWN")
                    }
                }

                poi.displayText?.textSize = 10.0f
                bubbleList.add(poi)
            }
        }

        annotationController?.add(bubbleList)
    }

    /**
     * update the current position and remove the passed annotations
     */
    private fun updateCurrentPosition(legIndex: Int, stepIndex: Int, edgeIndex: Int) {
        if (alongRouteTraffic == null) {
            return
        }

        val removeList = bubbleList.filter { annotation ->
            val leg = annotation.extraInfo?.getInt(LEG_INDEX) ?: -1
            val step = annotation.extraInfo?.getInt(STEP_INDEX) ?: -1
            val edge = annotation.extraInfo?.getInt(EDGE_INDEX) ?: -1
            if (legIndex > leg){
                return@filter true
            }else if (legIndex < leg){
                return@filter false
            }

            if (stepIndex > step){
                return@filter true
            }else if (stepIndex < step){
                return@filter false
            }

            if (edgeIndex > edge){
                return@filter true
            }else if (edgeIndex < edge){
                return@filter false
            }

            return@filter false
        }

        annotationController?.remove(removeList)
        bubbleList.removeAll(removeList)
    }

    private fun generalEdgeList(route: Route?): List<EdgeModel> {
        val result = ArrayList<EdgeModel>()
        route?.routeLegList?.forEachIndexed { leg, routeLeg ->
            routeLeg.routeStepList?.forEachIndexed { step, routeStep ->
                routeStep.routeEdgeList?.forEachIndexed { edge, routeEdge ->
                    result.add(EdgeModel(leg, step, edge, routeEdge))
                }
            }
        }

        return result
    }

    private data class EdgeModel(val legIndex: Int, val stepIndex: Int, val edgeIndex: Int, val edge: RouteEdge)

    private data class PoiModel constructor(val legIndex: Int, val stepIndex: Int, val edgeIndex: Int, val latLon: LatLon?, var congestionLevel: Int = -1)
}