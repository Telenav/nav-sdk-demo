/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation.avoid

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.gson.Gson
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.sdk.demo.scenario.navigation.BaseNavFragment
import com.telenav.sdk.demo.scenario.search.SearchAlongRouteFragment
import com.telenav.sdk.demo.util.AndroidThreadUtils
import com.telenav.sdk.demo.util.BitmapUtils
import com.telenav.sdk.drivesession.callback.AvoidRouteRequestCallback
import com.telenav.sdk.drivesession.model.avoid.AvoidRouteStatus
import com.telenav.sdk.drivesession.model.avoid.FailReason
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentAvoidIncidentBinding
import com.telenav.sdk.map.model.AlongRouteTraffic
import com.telenav.sdk.map.model.AlongRouteTrafficIncidentInfo
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_avoid_incident.*
import android.location.Location as Location1

/**
 * avoid incident demo
 * @author wuchangzhong on 2021/09/15
 */
class AvoidIncidentFragment : BaseNavFragment(), TnAvoidIncidentRecyclerViewAdapter.OnItemClickListener {

    var isSettingLocation: Boolean = false
    private val tnAvoidIncidentRecyclerViewAdapter: TnAvoidIncidentRecyclerViewAdapter = TnAvoidIncidentRecyclerViewAdapter()
    private lateinit var poiBitmap: Bitmap
    private val poiRes = R.drawable.map_pin_green_icon_unfocused


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentAvoidIncidentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_avoid_incident, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        turn_direction_recycler_view.adapter = tnAvoidIncidentRecyclerViewAdapter
        poiBitmap = BitmapUtils.getBitmapFromVectorDrawable(requireContext(), poiRes)
            ?: BitmapFactory.decodeResource(resources, R.drawable.map_pin_green_icon_unfocused)
        navigationOn.observe(viewLifecycleOwner) {
            if (!it) {
                routes?.clear()
                success_avoid_incident.text = ""
            } else map_view.cameraController().disableFollowVehicle()
        }

        tnAvoidIncidentRecyclerViewAdapter.setOnItemClickListener(this)
        setVehicleLocation.setOnClickListener {
            navigationSession?.stopNavigation()
            map_view.annotationsController().clear()
            map_view.routesController().clear()
            map_view.cameraController().disableFollowVehicle()
            navButton.setText(R.string.stop_navigation)
            navButton.isEnabled = false
            navigating = false
            isSettingLocation = true
        }
    }

    override fun onLongClick(location: Location1?) {
        if (isSettingLocation) {
            //change the location of vehicle
            location?.let { locationProvider.setLocation(it) }
            isSettingLocation = false
        } else {
            super.onLongClick(location)
        }
    }

    override fun onAlongRouteTrafficUpdated(alongRouteTraffic: AlongRouteTraffic) {
        super.onAlongRouteTrafficUpdated(alongRouteTraffic)
        val alongRouteTrafficIncidents = alongRouteTraffic.alongRouteTrafficIncidents
        if (alongRouteTrafficIncidents != null && alongRouteTrafficIncidents.isNotEmpty()) {
            AndroidThreadUtils.runOnUiThread(Runnable {
                tnAvoidIncidentRecyclerViewAdapter.setupData(alongRouteTrafficIncidents)
                showPoi(alongRouteTrafficIncidents)
            })
        }
    }

    /**
     * Show incident POI
     */
    var annotationList = ArrayList<Annotation>()
    private fun showPoi(alongRouteTrafficIncidents: List<AlongRouteTrafficIncidentInfo>) {
        annotationList = ArrayList<Annotation>()
        for (i in alongRouteTrafficIncidents.indices) {
            val alongRouteTrafficIncidentInfo = alongRouteTrafficIncidents[i]
            val item = alongRouteTrafficIncidents[i]
            val location1 = Location1("")
            location1.longitude = item?.incidentLocation?.incidentLocation?.lon!!
            location1.latitude = item?.incidentLocation?.incidentLocation?.lat!!
            val annotation = map_view.annotationsController().factory().create(requireContext(), Annotation.UserGraphic(poiBitmap), location1)
            annotation.extraInfo = Bundle().apply {
                item.incidentLocation?.incidentLocation?.lon?.let { this.putDouble(SearchAlongRouteFragment.NAV_LONGITUDE, it) }
                item.incidentLocation?.incidentLocation?.lat?.let { this.putDouble(SearchAlongRouteFragment.NAV_LATITUDE, it) }
            }
            annotation.displayText = item.incidentLocation?.let { Annotation.TextDisplayInfo.Centered("${alongRouteTrafficIncidentInfo.incidentLocation?.incidentLocation}") }
            annotation.style = Annotation.Style.ScreenAnnotationPopup
            annotationList.add(annotation)
        }

        map_view.annotationsController().add(annotationList)
    }

    override fun onItemClick(view: View, alongRouteTrafficIncidentInfo: AlongRouteTrafficIncidentInfo) {
        showAvoidDialog(view, alongRouteTrafficIncidentInfo)
    }

    private fun showAvoidDialog(view: View, alongRouteTrafficIncidentInfo: AlongRouteTrafficIncidentInfo) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(alongRouteTrafficIncidentInfo?.description)
        builder.setPositiveButton("Avoid") { dialog, _ ->
            navigationSession?.avoidIncident(mutableListOf(alongRouteTrafficIncidentInfo), AvoidRouteRequestCallback {
                val avoidStatus = it.avoidStatus
                val avoidFailReason = it.failReason
                AndroidThreadUtils.runOnUiThread {
                    if (avoidStatus == AvoidRouteStatus.SUCCESS) {
                        var route = it.route!!
                        ll_success_avoid_incident.visibility = View.VISIBLE
                        var avoidSuccessIncident = success_avoid_incident.text
                        success_avoid_incident.text = "${avoidSuccessIncident}\n\n${Gson().toJson(alongRouteTrafficIncidentInfo.incidentLocation?.incidentLocation)}"
                        routeIds.clear()
                        map_view.routesController().clear()
                        routeIds = map_view.routesController().add(mutableListOf(route))
                        map_view.routesController().highlight(routeIds[0])
                        var region = map_view.routesController().region(routeIds)
                        map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                        highlightedRouteId = routeIds[0]
                        navigationSession?.updateRoute(route)
                        tnAvoidIncidentRecyclerViewAdapter.clear()
                        map_view.annotationsController().remove(annotationList)
                        Toast.makeText(activity, "success", Toast.LENGTH_LONG).show()
                    } else {
                        when (avoidFailReason) {
                            FailReason.RouteRequestFail -> {
                                Toast.makeText(activity, "RouteRequestFail", Toast.LENGTH_SHORT).show()
                            }
                            FailReason.CannotAvoid -> {
                                Toast.makeText(activity, "CannotAvoid", Toast.LENGTH_SHORT).show()
                            }
                            FailReason.AvoidRouteInProgress -> {
                                Toast.makeText(activity, "AvoidRouteInProgress", Toast.LENGTH_SHORT).show()
                            }
                            FailReason.WrongNavStatus -> {
                                Toast.makeText(activity, "WrongNavStatus", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    dialog.dismiss()
                }

            }

            )
        }
        builder.setNegativeButton("cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun getDemonstrateSpeed(): Double {
        return 20.0
    }
}