/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.automation

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.direction.model.*
import kotlinx.android.synthetic.main.fragment_pure_navigation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PureNavigationFragment : Fragment() {
    val viewModel: PureNavigationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pure_navigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()

        btn_route_commit.setOnClickListener {
            viewModel.requestDirection(getRequestRouteMode(), getAvoidOptions(), getRequestRouteStyle(),
                    getRequestRouteContentLevel()) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (it == 0) {
                        Toast.makeText(requireContext(), "Request success!", Toast.LENGTH_SHORT).show()
                        btn_navigation_real.isEnabled = true
                        btn_navigation_simulation.isEnabled = true
                    }else{
                        Toast.makeText(requireContext(), "Request fail, code: $it", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btn_navigation_real.setOnClickListener {
            viewModel.startNavigation(false)
            viewModel.startRunningTraceData()
            btn_navigation_real.isEnabled = false
            btn_navigation_simulation.isEnabled = false
            btn_navigation_stop.isEnabled = true
        }

        btn_navigation_simulation.setOnClickListener {
            viewModel.startNavigation(true)
            btn_navigation_real.isEnabled = false
            btn_navigation_simulation.isEnabled = false
            btn_navigation_stop.isEnabled = true
        }

        btn_navigation_stop.setOnClickListener {
            viewModel.stopNavigation()
            btn_navigation_real.isEnabled = true
            btn_navigation_simulation.isEnabled = true
            btn_navigation_stop.isEnabled = false
        }

        sw_adas.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAdas(isChecked)
        }

        sw_alert.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAlert(isChecked)
        }
    }

    private fun observe() {
        viewModel.vehicleLocation.observe(viewLifecycleOwner) {
            tv_current_location.text = String.format("[%.5f, %.5f]", it.latitude, it.longitude)
        }

        viewModel.currentStreetName.observe(viewLifecycleOwner) {
            tv_current_street.text = it
        }

        viewModel.navigationOn.observe(viewLifecycleOwner) {
            if (it) {
                tv_current_status.text = "Navigation on"
                tv_current_status.setTextColor(Color.GREEN)
            } else {
                tv_current_status.text = "Navigation off"
                tv_current_status.setTextColor(Color.GRAY)
            }
        }

        viewModel.startLocationText.observe(viewLifecycleOwner) {
            tv_location_start.text = it
        }

        viewModel.stopLocationText.observe(viewLifecycleOwner) {
            tv_location_stop.text = it
        }

        viewModel.junctionBitmap.observe(viewLifecycleOwner) {
            if (it == null){
                iv_junction.visibility = View.GONE
            }else{
                iv_junction.visibility = View.VISIBLE
                iv_junction.setImageBitmap(it)
            }
        }

        viewModel.alertNumber.observe(viewLifecycleOwner) {
            tv_alert.text = "Alert times: $it"
        }

        viewModel.adasNumber.observe(viewLifecycleOwner) {
            tv_adas.text = "Adas times: $it"
        }

        viewModel.drgNumber.observe(viewLifecycleOwner) {
            tv_drg.text = "Route update times: $it"
        }
    }

    private fun getRequestRouteMode(): RequestMode {
        return when (rg_source.checkedRadioButtonId) {
            R.id.rb_cloud -> RequestMode.CLOUD_ONLY
            R.id.rb_onbard -> RequestMode.EMBEDDED_ONLY
            else -> RequestMode.HYBRID
        }
    }

    private fun getRequestRouteStyle(): Int {
        return when (rg_route_style.checkedRadioButtonId) {
            R.id.rb_fastest -> RouteStyle.FASTEST
            R.id.rb_shortest -> RouteStyle.SHORTEST
            R.id.rb_pedestrian -> RouteStyle.PEDESTRIAN
            R.id.rb_personalized -> RouteStyle.PERSONALIZED
            R.id.rb_eco -> RouteStyle.ECO
            else -> RouteStyle.FASTEST
        }
    }

    private fun getRequestRouteContentLevel(): Int {
        return when (rg_content_level.checkedRadioButtonId) {
            R.id.rb_ETA -> ContentLevel.ETA
            R.id.rb_overview -> ContentLevel.OVERVIEW
            R.id.rb_full -> ContentLevel.FULL
            else -> ContentLevel.FULL
        }
    }

    private fun getAvoidOptions(): RoutePreferences {
        return RoutePreferences.Builder()
                .avoidCarTrains(cb_avoid_car_trains.isChecked)
                .avoidCountryBorders(cb_avoid_country_border.isChecked)
                .avoidFerries(cb_avoid_ferries.isChecked)
                .avoidHighways(cb_avoid_highways.isChecked)
                .avoidHovLanes(cb_avoid_HOV_lanes.isChecked)
                .avoidPermitRequiredRoads(cb_avoid_roads_requiring_permits.isChecked)
                .avoidSeasonalRestrictions(cb_avoid_seasonal_restrictions.isChecked)
                .avoidSharpTurns(cb_avoid_sharp_turns.isChecked)
                .avoidTollRoads(cb_avoid_toll_roads.isChecked)
                .avoidTunnels(cb_avoid_tunnels.isChecked)
                .avoidUnpavedRoads(cb_avoid_unpaved_roads.isChecked)
                .useTraffic(cb_use_traffic.isChecked)
                .build()
    }

}