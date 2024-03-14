/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewSetUpBinding

/**
 * This fragment shows how to set up a map view
 * @author zhai.xiang on 2021/1/11
 */
class MapViewSetUpFragment : Fragment() {
    private var _binding: FragmentMapViewSetUpBinding? = null
    private val binding get() = _binding!!

    lateinit var locationProvider: DemoLocationProvider
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewSetUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        binding.include.tvTitle.text = getString(R.string.title_activity_map_view_set_up)
        binding.include.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        activity?.runOnUiThread {
            binding.mapView.vehicleController().setLocation(locationProvider.getLastKnownLocation())
        }
    }

    /**
     * the initialize function must be called after SDK is initialized
     */
    private fun mapViewInit(savedInstanceState: Bundle?) {
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

}