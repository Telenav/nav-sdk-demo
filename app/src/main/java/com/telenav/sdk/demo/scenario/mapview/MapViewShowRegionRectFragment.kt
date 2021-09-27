/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

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
import com.telenav.sdk.demo.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_map_view_show_region.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import java.util.*

/**
 * A simple [Fragment] show rect about region
 * @author wu.changzhong on 2021/8/27
 */
class MapViewShowRegionRectFragment : BaseNavFragment(){

    private var destinationLocation: Location? = null
    private var wayPointList = mutableListOf<Location>()
    private var wayPointAnnotationList = mutableListOf<Annotation>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_view_show_region, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_show_region_rect)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        navigationOn.observe(viewLifecycleOwner) {
            tnRectView.visibility = View.GONE
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
            val factory = map_view.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
            annotation?.displayText = Annotation.TextDisplayInfo.Centered("Destination")
            map_view.annotationsController().add(arrayListOf(annotation))
            vehicleLocation?.let { requestDirection(it, location) }
        } else {
            val factory = map_view.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.add_location, location)
            annotation?.displayText = Annotation.TextDisplayInfo.Centered("wayPoint" + (wayPointList.size + 1))
            map_view.annotationsController().add(arrayListOf(annotation))
            activity?.runOnUiThread {
                AlertDialog.Builder(StopPointFragment@ this.requireContext())
                        .setCancelable(false)
                        .setIcon(R.drawable.add_location)
                        .setTitle("New Annotation")
                        .setPositiveButton("add wayPoint") { dialog, whichButton ->
                            dialog.dismiss()
                            wayPointList.add(wayPointList.size, location)
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
                            map_view.annotationsController().clear()
                            onLongClick(location)
                        }
                        .setNeutralButton("cancel") { dialog, whichButton ->
                            dialog.dismiss()
                            map_view.annotationsController().remove(arrayListOf(annotation))
                        }
                        .show()
            }
        }

    }

    override fun requestDirection(begin: Location, end: Location, wayPointList: MutableList<Location>?) {
        Log.d("MapLogsForTestData", "MapLogsForTestData >>>> requestDirection begin: $begin + end $end")
        val wayPoints: ArrayList<GeoLocation> = arrayListOf()
        wayPointList?.forEach {
            wayPoints.add(GeoLocation(LatLon(it.latitude, it.longitude)))
        }
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(begin),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
                .routeCount(2)
                .stopPoints(wayPoints)
                .startTime(Calendar.getInstance().timeInMillis / 1000)
                .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
        task.runAsync { response ->
            Log.d(TAG, "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds = map_view.routesController().add(routes)
                map_view.routesController().highlight(routeIds[0])
                val region = map_view.routesController().region(routeIds)
                val width = map_view.width
                val height = map_view.height
                val rect = Rect(width / 2 - 300, height / 2 - 300, width - 300, height - 300)
                tnRectView?.setRect(rect)
                map_view.cameraController().showRegion(region,rect)
                highlightedRouteId = routeIds[0]
                activity?.runOnUiThread {
                    navButton.isEnabled = true
                    navButton.setText(R.string.start_navigation)
                }

            } else {
                activity?.runOnUiThread {
                    navButton.isEnabled = false
                }
            }
            task.dispose()
        }
    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        super.onNavigationStopReached(stopIndex, stopLocation)
        if (stopIndex >= 0 && stopIndex < wayPointAnnotationList.size) {
            map_view.annotationsController().remove(arrayListOf(wayPointAnnotationList[stopIndex]))
        }
    }

}