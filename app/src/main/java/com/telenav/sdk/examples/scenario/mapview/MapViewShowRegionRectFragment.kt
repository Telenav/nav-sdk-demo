/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentMapViewShowRegionBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

/**
 * A simple [Fragment] show rect about region
 * @author wu.changzhong on 2021/8/27
 */
class MapViewShowRegionRectFragment : BaseNavFragment<FragmentMapViewShowRegionBinding>() {

    private var destinationLocation: Location? = null
    private var wayPointList = mutableListOf<Waypoint>()
    private var wayPointAnnotationList = mutableListOf<Annotation>()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMapViewShowRegionBinding {
        return FragmentMapViewShowRegionBinding.inflate(inflater, container, false)
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.contentBasicNavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.include.tvTitle.text = getString(R.string.title_activity_map_view_show_region_rect)
        binding.include.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        navigationOn.observe(viewLifecycleOwner) {
            binding.rectView.visibility = View.GONE
            if (!it) {
                destinationLocation = null
                wayPointList.clear()
                wayPointAnnotationList.clear()
            }
        }
    }

    override fun onLongClick(location: Location?) {
        if (location == null) {
            return
        }
        val annotation: Annotation?
        if (destinationLocation == null) {
            destinationLocation = location
            val factory = binding.contentBasicNavigation.mapView.annotationsController().factory()
            annotation =
                factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
            annotation.displayText = Annotation.TextDisplayInfo.Centered("Destination")
            binding.contentBasicNavigation.mapView.annotationsController().add(arrayListOf(annotation))
            vehicleLocation?.let { requestDirection(it, location) }
        } else {
            val factory = binding.contentBasicNavigation.mapView.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.add_location, location)
            annotation.displayText =
                Annotation.TextDisplayInfo.Centered("wayPoint" + (wayPointList.size + 1))
            binding.contentBasicNavigation.mapView.annotationsController().add(arrayListOf(annotation))
            activity?.runOnUiThread {
                AlertDialog.Builder(StopPointFragment@ this.requireContext())
                    .setCancelable(false)
                    .setIcon(R.drawable.add_location)
                    .setTitle("New Annotation")
                    .setPositiveButton("add wayPoint") { dialog, whichButton ->
                        dialog.dismiss()
                        wayPointList.add(wayPointList.size, Waypoint(GeoLocation(location)))
                        wayPointAnnotationList.add(annotation)
                        if (vehicleLocation != null) {
                            requestDirection(vehicleLocation!!, destinationLocation!!, wayPointList)
                        }
                    }
                    .setNegativeButton("new destination") { dialog, whichButton ->
                        dialog.dismiss()
                        destinationLocation = null
                        wayPointList.clear()
                        wayPointAnnotationList.clear()
                        binding.contentBasicNavigation.mapView.annotationsController().clear()
                        onLongClick(location)
                    }
                    .setNeutralButton("cancel") { dialog, whichButton ->
                        dialog.dismiss()
                        binding.contentBasicNavigation.mapView.annotationsController().remove(arrayListOf(annotation))
                    }
                    .show()
            }
        }

    }

    override fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Waypoint>?
    ) {
        Log.d(
            "MapLogsForTestData",
            "MapLogsForTestData >>>> requestDirection begin: $begin + end $end"
        )

        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(2)
            .stopPoints(wayPointList)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            Log.d(
                TAG,
                "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}"
            )
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds = binding.contentBasicNavigation.mapView.routesController().add(routes).toMutableList()
                binding.contentBasicNavigation.mapView.routesController().highlight(routeIds[0])
                val region = binding.contentBasicNavigation.mapView.routesController().region(routeIds)
                val width = binding.contentBasicNavigation.mapView.width
                val height = binding.contentBasicNavigation.mapView.height
                val rect = Rect(width / 2 - 300, height / 2 - 300, width - 300, height - 300)
                binding.rectView?.setRect(rect)
                binding.contentBasicNavigation.mapView.cameraController().showRegion(region, rect)
                highlightedRouteId = routeIds[0]
                activity?.runOnUiThread {
                    binding.contentBasicNavigation.navButton.isEnabled = true
                    binding.contentBasicNavigation.navButton.setText(R.string.start_navigation)
                }

            } else {
                activity?.runOnUiThread {
                    binding.contentBasicNavigation.navButton.isEnabled = false
                }
            }
            task.dispose()
        }
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        super.onNavigationStopReached(stopIndex, stopLocation)
        if (stopIndex >= 0 && stopIndex < wayPointAnnotationList.size) {
            binding.contentBasicNavigation.mapView.annotationsController().remove(arrayListOf(wayPointAnnotationList[stopIndex]))
        }
    }

}