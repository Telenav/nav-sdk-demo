/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

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
import com.telenav.sdk.demo.R
import com.telenav.sdk.drivesession.listener.NavigationEventListener
import com.telenav.sdk.drivesession.model.BetterRouteCandidate
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_nav_auto_refresh.*
import java.util.*

/**
 * A simple [Fragment] for auto refresh route:
 * receive a route update due to road restriction and refresh the route on the map.
 * receive a route update due to deviation and refresh the route on the map.
 * accept a better route from the navigation session.
 *
 * @author tang.hui on 2021/1/13
 */
class AutoRefreshRouteFragment : BaseNavFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nav_auto_refresh, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // add button to manual update route
        btn_update_route.setOnClickListener {
            if (stopNavButton.isEnabled) {// is routing
                vehicleLocation?.let {
                    val location = Location("test")
                    location.latitude = 37.403193176493275
                    location.longitude = -121.9759252448296

                    activity?.runOnUiThread {
                        val factory = map_view.annotationsController().factory()
                        val annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
                        annotation.displayText = Annotation.TextDisplayInfo.Centered("update Destination")
                        map_view.annotationsController().add(arrayListOf(annotation))
                    }

                    val request: RouteRequest = RouteRequest.Builder(
                            GeoLocation(LatLon(it.latitude, it.longitude)),
                            GeoLocation(LatLon(location.latitude, location.longitude))
                    ).contentLevel(ContentLevel.FULL)
                            .routeCount(1)
                            .startTime(Calendar.getInstance().timeInMillis / 1000)
                            .build()
                    val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, RequestMode.CLOUD_ONLY)
                    task.runAsync { response ->
                        Log.d(TAG, "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}")
                        if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                            routes = response.response.result
                            routeIds = map_view.routesController().add(routes)
                            highlightedRouteId = routeIds[0]
                            map_view.routesController().highlight(highlightedRouteId!!)
                            val region = map_view.routesController().region(routeIds)
                            map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))

                            navigationSession?.updateRoute(routes[0])//manual update route

                        }
                        task.dispose()
                    }
                }
            }
        }
    }

    override fun onNavigationRouteUpdated(route: Route, reason: NavigationEventListener.RouteUpdateReason?) {
//        route.dispose()
        Log.d(TAG, "onNavigationRouteUpdated:${route.id}")
        highlightedRouteId?.let { map_view.routesController().remove(it) }
        map_view.routesController().refresh(route)//auto refresh route
        if (route.id != highlightedRouteId) {
            highlightedRouteId = route.id
            map_view.routesController().updateRouteProgress(route.id)
        }
    }

    override fun onBetterRouteDetected(betterRouteCandidate: BetterRouteCandidate) {
        betterRouteCandidate.accept(true)
    }
}