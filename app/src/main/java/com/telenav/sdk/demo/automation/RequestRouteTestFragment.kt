/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.automation

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Margins
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.demo.scenario.navigation.BaseNavFragment
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.content_basic_navigation.*
import java.util.*

/**
 * A simple [Fragment] for automation test of route request
 * @author tang.hui on 2021/3/23
 */
class RequestRouteTestFragment : BaseNavFragment() {

    var requestMode: RequestMode = RequestMode.CLOUD_ONLY
    var routeStyle: Int = RouteStyle.FASTEST
    var routeCount: Int = 2
    var contentLevel: Int = ContentLevel.FULL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.content_basic_navigation, container, false)
    }

    override fun requestDirection(begin: Location, end: Location, wayPointList: MutableList<Location>?) {
        EspressoIdlingResource.increment()
        Log.d("requestDirection", "MapLogsForTestData >>>> requestDirection begin: $begin + end $end")
        val request: RouteRequest = RouteRequest.Builder(
                GeoLocation(begin),
                GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(contentLevel)
                .routeStyle(routeStyle)
                .routeCount(routeCount)
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
                }

            } else {
                activity?.runOnUiThread {
                    navButton.isEnabled = false
                }
            }
            task.dispose()
            EspressoIdlingResource.decrement()
        }
    }

    override fun onNavButtonClick(navigating: Boolean) {
        if (navigating) {
            EspressoIdlingResource.increment()
        } else {
            EspressoIdlingResource.decrement()
        }
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            navButton.performClick()// stop navigation for automation test
        }, 5000)
    }


}