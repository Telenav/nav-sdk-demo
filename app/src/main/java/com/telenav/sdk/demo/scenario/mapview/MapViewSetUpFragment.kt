/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.telenav.sdk.examples.R
import com.telenav.sdk.demo.provider.DemoLocationProvider
import kotlinx.android.synthetic.main.fragment_map_view_set_up.mapView
import kotlinx.android.synthetic.main.layout_action_bar.*

/**
 * This fragment shows how to set up a map view
 * @author zhai.xiang on 2021/1/11
 */
class MapViewSetUpFragment : Fragment() {

    lateinit var locationProvider : DemoLocationProvider
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_view_set_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(requireContext(), DemoLocationProvider.ProviderType.SIMULATION)
        locationProvider.start()
        tv_title.text = getString(R.string.title_activity_map_view_set_up)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationProvider.stop()
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?){
        mapView.initialize(savedInstanceState){
            activity?.runOnUiThread {
                mapView.vehicleController().setLocation(locationProvider.lastKnownLocation)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

}