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
import android.widget.RadioGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.fragment.app.Fragment
import com.telenav.map.api.Margins
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentNavRequestRouteBinding
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*

/**
 * A simple [Fragment] for usage of route request
 * @author tang.hui on 2021/1/12
 */
class RequestRouteFragment : BaseNavFragment<FragmentNavRequestRouteBinding>(), RadioGroup.OnCheckedChangeListener {

    var requestMode: RequestMode = RequestMode.CLOUD_ONLY
    var routeStyle: Int = RouteStyle.FASTEST
    var routeCount: Int = 2
    var contentLevel: Int = ContentLevel.FULL

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNavRequestRouteBinding {
        return FragmentNavRequestRouteBinding.inflate(inflater, container, false)
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.includeContent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toggle = ActionBarDrawerToggle(
            activity,
            binding.drawerLayout,
            binding.drawerLayoutToolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupDrawerButtons()
    }

    override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
        when (checkedId) {
            binding.includeDrawer.rbCloud.id -> requestMode = RequestMode.CLOUD_ONLY
            binding.includeDrawer.rbOnbard.id -> requestMode = RequestMode.EMBEDDED_ONLY
            binding.includeDrawer.rbFastest.id -> routeStyle = RouteStyle.FASTEST
            binding.includeDrawer.rbShortest.id -> routeStyle = RouteStyle.SHORTEST
            binding.includeDrawer.rbEco.id -> routeStyle = RouteStyle.ECO
            binding.includeDrawer.rbEasy.id -> routeStyle = RouteStyle.EASY
            binding.includeDrawer.rbCount1.id -> routeCount = 1
            binding.includeDrawer.rbCount2.id -> routeCount = 2
            binding.includeDrawer.rbCount3.id -> routeCount = 3
            binding.includeDrawer.rbETA.id -> contentLevel = ContentLevel.ETA
            binding.includeDrawer.rbFull.id -> contentLevel = ContentLevel.FULL

        }
    }

    private fun setupDrawerButtons() {
        binding.includeDrawer.rgSource.setOnCheckedChangeListener(this)
        binding.includeDrawer.rgStyle.setOnCheckedChangeListener(this)
        binding.includeDrawer.rgCount.setOnCheckedChangeListener(this)
        binding.includeDrawer.rgContentLevel.setOnCheckedChangeListener(this)

        binding.includeDrawer.btnCommit.setOnClickListener {
            binding.drawerLayout.close()
            binding.includeContent.navButton.performClick()
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
        ).contentLevel(contentLevel)
            .routeStyle(routeStyle)
            .routeCount(routeCount)
            .avoidOption(
                RoutePreferences.Builder()
                    .avoidTollRoads(binding.includeDrawer.cbAvoidTollRoads.isChecked)
                    .avoidHighways(binding.includeDrawer.cbAvoidHighways.isChecked)
                    .avoidHovLanes(binding.includeDrawer.cbAvoidHOVLanes.isChecked)
                    .avoidFerries(binding.includeDrawer.cbAvoidFerries.isChecked)
                    .avoidCarTrains(binding.includeDrawer.cbAvoidCarTrains.isChecked)
                    .avoidUnpavedRoads(binding.includeDrawer.cbAvoidUnpavedRoads.isChecked)
                    .avoidTunnels(binding.includeDrawer.cbAvoidTunnels.isChecked)
                    .avoidTrafficCongestion(binding.includeDrawer.cbUseTraffic.isChecked)
                    .avoidCountryBorders(binding.includeDrawer.cbAvoidCountryBorder.isChecked)
                    .avoidSharpTurns(binding.includeDrawer.cbAvoidSharpTurns.isChecked)
                    .avoidPermitRequiredRoads(binding.includeDrawer.cbAvoidRoadsRequiringPermits.isChecked)
                    .avoidSeasonalRestrictions(binding.includeDrawer.cbAvoidSeasonalRestrictions.isChecked)
                    .build()
            )
            .build()
        val task = DirectionClient.Factory.hybridClient().createRoutingTask(request, requestMode)
        task.runAsync { response ->
            Log.d(
                TAG,
                "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}"
            )
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds = findMapView().routesController().add(routes).toMutableList()
                findMapView().routesController().highlight(routeIds[0])
                val region = findMapView().routesController().region(routeIds)
                findMapView().cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                activity?.runOnUiThread {
                    binding.includeContent.navButton.isEnabled = true
                    binding.includeContent.navButton.setText(R.string.start_navigation)
                    showRouteOptionDialog()
                }

            } else {
                activity?.runOnUiThread {
                    binding.includeContent.navButton.isEnabled = false
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
            findMapView().routesController().highlight(highlightedRouteId!!)
        }
        dialog.show(childFragmentManager, "route option")

    }


}