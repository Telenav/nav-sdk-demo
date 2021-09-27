/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
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
import android.widget.RadioGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.fragment.app.Fragment
import com.telenav.map.api.Margins
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.drawer_nav_request_route.*
import kotlinx.android.synthetic.main.fragment_nav_request_route.*
import java.util.*

/**
 * A simple [Fragment] for usage of route request
 * @author tang.hui on 2021/1/12
 */
class RequestRouteFragment : BaseNavFragment(), RadioGroup.OnCheckedChangeListener {

    var requestMode: RequestMode = RequestMode.CLOUD_ONLY
    var routeStyle: Int = RouteStyle.FASTEST
    var routeCount: Int = 2
    var contentLevel: Int = ContentLevel.FULL

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nav_request_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toggle = ActionBarDrawerToggle(activity, drawer_layout, drawer_layout_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        setupDrawerButtons()
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            rb_cloud.id -> requestMode = RequestMode.CLOUD_ONLY
            rb_onbard.id -> requestMode = RequestMode.EMBEDDED_ONLY
            rb_fastest.id -> routeStyle = RouteStyle.FASTEST
            rb_shortest.id -> routeStyle = RouteStyle.SHORTEST
            rb_eco.id -> routeStyle = RouteStyle.ECO
            rb_easy.id -> routeStyle = RouteStyle.EASY
            rb_count1.id -> routeCount = 1
            rb_count2.id -> routeCount = 2
            rb_count3.id -> routeCount = 3
            rb_ETA.id -> contentLevel = ContentLevel.ETA
            rb_full.id -> contentLevel = ContentLevel.FULL

        }
    }

    private fun setupDrawerButtons() {
        rg_source.setOnCheckedChangeListener(this)
        rg_style.setOnCheckedChangeListener(this)
        rg_count.setOnCheckedChangeListener(this)
        rg_content_level.setOnCheckedChangeListener(this)

        btn_commit.setOnClickListener {
            drawer_layout.close()
            navButton.performClick()
        }
    }

    override fun requestDirection(begin: Location, end: Location, wayPointList: MutableList<Location>?) {
        Log.d("MapLogsForTestData", "MapLogsForTestData >>>> requestDirection begin: $begin + end $end")
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(begin),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(contentLevel)
                .routeStyle(routeStyle)
                .routeCount(routeCount)
                .avoidOption(RoutePreferences.Builder()
                        .avoidTollRoads(cb_avoid_toll_roads.isChecked)
                        .avoidHighways(cb_avoid_highways.isChecked)
                        .avoidHovLanes(cb_avoid_HOV_lanes.isChecked)
                        .avoidFerries(cb_avoid_ferries.isChecked)
                        .avoidCarTrains(cb_avoid_car_trains.isChecked)
                        .avoidUnpavedRoads(cb_avoid_unpaved_roads.isChecked)
                        .avoidTunnels(cb_avoid_tunnels.isChecked)
                        .avoidTrafficCongestion(cb_use_traffic.isChecked)
                        .avoidCountryBorders(cb_avoid_country_border.isChecked)
                        .avoidSharpTurns(cb_avoid_sharp_turns.isChecked)
                        .avoidPermitRequiredRoads(cb_avoid_roads_requiring_permits.isChecked)
                        .avoidSeasonalRestrictions(cb_avoid_seasonal_restrictions.isChecked)
                        .build())
                .startTime(Calendar.getInstance().timeInMillis / 1000)
                .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, requestMode)
        task.runAsync { response ->
            Log.d(TAG, "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}")
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds = map_view.routesController().add(routes)
                map_view.routesController().highlight(routeIds[0])
                val region = map_view.routesController().region(routeIds)
                map_view.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                activity?.runOnUiThread {
                    navButton.isEnabled = true
                    navButton.setText(R.string.start_navigation)
                    showRouteOptionDialog()
                }

            } else {
                activity?.runOnUiThread {
                    navButton.isEnabled = false
                }
            }
            task.dispose()
        }
    }

    private fun showRouteOptionDialog() {
        if (routes.size < 2) {
            return
        }
        val list = mutableListOf<String>()
        routes.forEach {
            list.add("distance:${it.distance}")
        }
        val dialog = RouteOptionDialogFragment.newInstance(list) {
            highlightedRouteId = routeIds[it]
            map_view.routesController().highlight(highlightedRouteId!!)
        }
        dialog.show(childFragmentManager, "route option")

    }


}