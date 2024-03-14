/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation.avoid

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.sdk.examples.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.scenario.search.SearchAlongRouteFragment
import com.telenav.sdk.examples.util.AndroidThreadUtils
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.drivesession.model.RerouteRequest
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentAvoidIncidentBinding
import com.telenav.sdk.map.direction.model.DirectionErrorCode
import com.telenav.sdk.map.model.AlongRouteTraffic
import com.telenav.sdk.map.model.AlongRouteTrafficIncidentInfo
import android.location.Location as Location1

/**
 * avoid incident demo
 * @author wuchangzhong on 2021/09/15
 */
class AvoidIncidentFragment : BaseNavFragment<FragmentAvoidIncidentBinding>(),
    TnAvoidIncidentRecyclerViewAdapter.OnItemClickListener {

    var isSettingLocation: Boolean = false
    private val tnAvoidIncidentRecyclerViewAdapter: TnAvoidIncidentRecyclerViewAdapter =
        TnAvoidIncidentRecyclerViewAdapter()
    private lateinit var poiBitmap: Bitmap
    private val poiRes = R.drawable.map_pin_green_icon_unfocused

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAvoidIncidentBinding {
        val binding: FragmentAvoidIncidentBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_avoid_incident, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.contentBasicNavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.turnDirectionRecyclerView.adapter = tnAvoidIncidentRecyclerViewAdapter
        poiBitmap = BitmapUtils.getBitmapFromVectorDrawable(requireContext(), poiRes)
            ?: BitmapFactory.decodeResource(resources, R.drawable.map_pin_green_icon_unfocused)
        navigationOn.observe(viewLifecycleOwner) {
            if (!it) {
                routes.clear()
                binding.successAvoidIncident.text = ""
            } else binding.contentBasicNavigation.mapView.cameraController().disableFollowVehicle()
        }

        tnAvoidIncidentRecyclerViewAdapter.setOnItemClickListener(this)
        binding.setVehicleLocation.setOnClickListener {
            driveSession.stopNavigation()
            binding.contentBasicNavigation.mapView.annotationsController().clear()
            binding.contentBasicNavigation.mapView.routesController().clear()
            binding.contentBasicNavigation.mapView.cameraController().disableFollowVehicle()
            binding.contentBasicNavigation.navButton.setText(R.string.stop_navigation)
            binding.contentBasicNavigation.navButton.isEnabled = false
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
            location1.longitude = item.incidentLocation?.incidentPosition?.lon!!
            location1.latitude = item.incidentLocation?.incidentPosition?.lat!!
            val annotation = binding.contentBasicNavigation.mapView.annotationsController().factory()
                .create(requireContext(), Annotation.UserGraphic(poiBitmap), location1)
            annotation.extraInfo = Bundle().apply {
                item.incidentLocation?.incidentPosition?.lon?.let {
                    this.putDouble(
                        SearchAlongRouteFragment.NAV_LONGITUDE,
                        it
                    )
                }
                item.incidentLocation?.incidentPosition?.lat?.let {
                    this.putDouble(
                        SearchAlongRouteFragment.NAV_LATITUDE,
                        it
                    )
                }
            }
            annotation.displayText =
                item.incidentLocation?.let { Annotation.TextDisplayInfo.Centered("${alongRouteTrafficIncidentInfo.incidentLocation?.incidentPosition}") }
            annotation.style = Annotation.Style.ScreenAnnotationPopup
            annotationList.add(annotation)
        }

        binding.contentBasicNavigation.mapView.annotationsController().add(annotationList)
    }

    override fun onItemClick(
        view: View,
        alongRouteTrafficIncidentInfo: AlongRouteTrafficIncidentInfo
    ) {
        showAvoidDialog(view, alongRouteTrafficIncidentInfo)
    }

    private fun showAvoidDialog(
        view: View,
        alongRouteTrafficIncidentInfo: AlongRouteTrafficIncidentInfo
    ) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(alongRouteTrafficIncidentInfo.description)
        builder.setPositiveButton("Avoid") { dialog, _ ->
            val request = RerouteRequest.Builder().setAvoidIncidents(mutableListOf(alongRouteTrafficIncidentInfo)).build()
            val task = navigationSession?.createRerouteTask(request)
            task?.runAsync{
                AndroidThreadUtils.runOnUiThread {
                    if (it.getResponse().getStatus() == DirectionErrorCode.OK) {
                        val routeInfo = it.getResponse()
                        val route = routeInfo.getRoute()!!
                        val routesController = mapView.getRoutesController()!!
                        routeIds = routesController.add(mutableListOf(route))!!.toMutableList()
                        routesController.highlight(routeIds[0])
                        val region = routesController.region(routeIds)
                        mapView.cameraController()
                            .showRegion(region, Margins.Percentages(0.20, 0.20))
                        highlightedRouteId = routeIds[0]
                        navigationSession?.acceptRerouteResult(routeInfo)
                        Toast.makeText(activity, "success", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(activity, "Reroute error!!", Toast.LENGTH_LONG).show()
                    }

                    dialog.dismiss()
                }
            }
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