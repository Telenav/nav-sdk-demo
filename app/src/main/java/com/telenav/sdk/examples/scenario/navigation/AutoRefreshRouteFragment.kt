/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.telenav.map.api.Annotation
import com.telenav.map.api.Margins
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.drivesession.model.BetterRouteProposal
import com.telenav.sdk.drivesession.model.BetterRouteUpdateProgress
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentNavAutoRefreshBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

/**
 * A simple [Fragment] for auto refresh route:
 * receive a route update due to road restriction and refresh the route on the map.
 * receive a route update due to deviation and refresh the route on the map.
 * accept a better route from the navigation session.
 *
 * @author tang.hui on 2021/1/13
 */
class AutoRefreshRouteFragment : BaseNavFragment<FragmentNavAutoRefreshBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNavAutoRefreshBinding {
        return FragmentNavAutoRefreshBinding.inflate(inflater, container, false)
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.includeContent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // add button to manual update route
        binding.btnUpdateRoute.setOnClickListener {
            //@TODO: Need to update logic here to determine when routing
            var isRouting = false
            if (isRouting) {//subViewButton.isEnabled) {// is routing
                vehicleLocation?.let {
                    val location = Location("test")
                    location.latitude = 37.403193176493275
                    location.longitude = -121.9759252448296

                    activity?.runOnUiThread {
                        val factory =
                            binding.includeContent.mapView.annotationsController().factory()
                        val annotation = factory.create(
                            requireContext(),
                            R.drawable.map_pin_green_icon_unfocused,
                            location
                        )
                        annotation.displayText =
                            Annotation.TextDisplayInfo.Centered("update Destination")
                        binding.includeContent.mapView.annotationsController()
                            .add(arrayListOf(annotation))
                    }

                    val request: RouteRequest = RouteRequest.Builder(
                        GeoLocation(it),
                        GeoLocation(LatLon(location.latitude, location.longitude))
                    ).contentLevel(ContentLevel.FULL)
                        .routeCount(1)
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
                            routeIds = binding.includeContent.mapView.routesController().add(routes)
                                .toMutableList()
                            highlightedRouteId = routeIds[0]
                            binding.includeContent.mapView.routesController()
                                .highlight(highlightedRouteId!!)
                            val region =
                                binding.includeContent.mapView.routesController().region(routeIds)
                            binding.includeContent.mapView.cameraController()
                                .showRegion(region, Margins.Percentages(0.20, 0.20))
                        }
                        task.dispose()
                    }
                }
            }
        }
    }

    override fun onBetterRouteDetected(proposal: BetterRouteProposal) {
        navigationSession?.acceptRouteProposal(proposal)
    }

    override fun onNavigationRouteUpdating(progress: BetterRouteUpdateProgress) {
        Log.d(TAG, "onNavigationRouteUpdating:${progress.newRoute?.id}")
        highlightedRouteId?.let { binding.includeContent.mapView.routesController().remove(it) }
        if (progress.status == BetterRouteUpdateProgress.Status.SUCCEEDED) {
            binding.includeContent.mapView.routesController()
                .refresh(progress.newRoute!!)//auto refresh route
            if (progress.newRoute!!.id != highlightedRouteId) {
                highlightedRouteId = progress.newRoute!!.id
                binding.includeContent.mapView.routesController()
                    .updateRouteProgress(progress.newRoute!!.id)
            }
        }
    }
}