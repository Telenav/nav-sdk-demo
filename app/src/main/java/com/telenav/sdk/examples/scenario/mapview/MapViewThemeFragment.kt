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
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.examples.provider.DemoLocationProvider
import com.telenav.sdk.examples.util.StyleConstants
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentMapViewThemeBinding
import com.telenav.sdk.map.SDK

/**
 * This fragment shows operations of theme
 * @author zhai.xiang on 2021/2/1
 */
class MapViewThemeFragment : Fragment() {
    private var _binding: FragmentMapViewThemeBinding? = null
    private val binding get() = _binding!!

    lateinit var locationProvider: DemoLocationProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapViewThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val mapViewReadyListener = MapViewReadyListener<MapView> {
        activity?.runOnUiThread {
            binding.includeContent.mapView.getVehicleController()
                ?.setLocation(locationProvider.getLastKnownLocation())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationProvider = DemoLocationProvider.Factory.createProvider(
            requireContext(),
            DemoLocationProvider.ProviderType.SIMULATION
        )
        binding.actionBar.tvTitle.text = getString(R.string.title_activity_map_view_theme)

        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.includeContent.mapView.initialize(mapViewConfig)

        binding.actionBar.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.includeContent.btnShowMenu.setOnClickListener {
            binding.drawerLayout.open()
        }
        setupDrawerOperations()
    }

    private fun setupDrawerOperations() {
        binding.includeOperation.rgMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_day -> setMapMode(DayNightMode.DAY)
                R.id.rb_night -> setMapMode(DayNightMode.NIGHT)
                R.id.rb_auto -> setMapMode(DayNightMode.AUTO)
            }
        }
        binding.includeOperation.rgLoadTss.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_load_default -> seStyleSheet(StyleConstants.DEFAULT_STYLE_PATH)
                R.id.rb_load_iv -> seStyleSheet(StyleConstants.INTERSECTION_VIEW_STYLE_PATH)
                R.id.rb_load_hud -> seStyleSheet(StyleConstants.HUD_STYLE_PATH)
                R.id.rb_load_hud_warm -> seStyleSheet(StyleConstants.HUD_WARM_STYLE_PATH)
                R.id.rb_load_invalid -> seStyleSheet(StyleConstants.INVALID_STYLE_PATH)
                R.id.rb_load_default_full -> seStyleSheet(StyleConstants.DEFAULT_STYLE_FULL_PATH)
                R.id.rb_load_local_warm -> seStyleSheet(StyleConstants.LOCAL_ABSOLUTE_WARM_STYLE_PATH)
                R.id.rb_load_cluster -> seStyleSheet(StyleConstants.CLUSTER_STYLE_PATH)
            }
        }
        binding.includeOperation.rgTextSize.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_text_size_large -> setTextSize(4f)
                R.id.rb_text_size_normal -> setTextSize(1f)
                R.id.rb_text_size_small -> setTextSize(0.5f)
            }
        }
    }

    /**
     * This method shows how to change day night mode of map
     */
    private fun setMapMode(@DayNightMode mode: Int) {
        SDK.getInstance().updateDayNightMode(mode)
    }

    /**
     * This method shows how to change text size of map
     */
    private fun setTextSize(size: Float) {
        binding.includeContent.mapView.themeController().setTextScale(size)
    }

    /**
     * This method shows how to set style sheet of map
     */
    private fun seStyleSheet(stylePath: String) {
        activity?.let {
            binding.includeContent.mapView.themeController().loadStyleSheet(stylePath)
        }
    }
}