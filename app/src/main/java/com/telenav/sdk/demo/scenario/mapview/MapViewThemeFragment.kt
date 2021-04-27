/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.common.model.Region
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.util.LocationUtils
import com.telenav.sdk.map.SDK
import kotlinx.android.synthetic.main.fragment_map_view_set_up.*
import kotlinx.android.synthetic.main.fragment_map_view_theme.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_map.*
import kotlinx.android.synthetic.main.layout_content_map.mapView
import kotlinx.android.synthetic.main.layout_operation_theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This fragment shows operations of theme
 * @author zhai.xiang on 2021/2/1
 */
class MapViewThemeFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map_view_theme, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_map_view_theme)
        mapView.initialize(savedInstanceState){
            mapView.vehicleController().setLocation(LocationUtils.getLocationByRegion())
        }
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }
        setupDrawerOperations()
    }

    private fun setupDrawerOperations() {
        rg_mode.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.rb_day -> setMapMode(DayNightMode.DAY)
                R.id.rb_night -> setMapMode(DayNightMode.NIGHT)
                R.id.rb_auto -> setMapMode(DayNightMode.AUTO)
            }
        }
        rg_load_tss.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.rb_load1 -> seStyleSheet(R.raw.newstyle)
                R.id.rb_load2 -> seStyleSheet(R.raw.newstyle2)
            }
        }
        rg_text_size.setOnCheckedChangeListener{_,checkedId->
            when(checkedId){
                R.id.rb_text_size_large -> setTextSize(4f)
                R.id.rb_text_size_normal -> setTextSize(1f)
                R.id.rb_text_size_small -> setTextSize(0.5f)
            }
        }
    }

    /**
     * This method shows how to change day night mode of map
     */
    private fun setMapMode(@DayNightMode mode: Int){
        SDK.getInstance().updateDayNightMode(mode)
    }

    /**
     * This method shows how to change text size of map
     */
    private fun setTextSize(size : Float){
        mapView.themeController().setTextScale(size)
    }

    /**
     * This method shows how to set style sheet of map
     */
    private fun seStyleSheet(res : Int){
        CoroutineScope(Dispatchers.IO).launch{
            val bytes = resources.openRawResource(res).readBytes()
            CoroutineScope(Dispatchers.Main).launch {
                activity?.let {
                    mapView.themeController().loadStyleSheet(bytes)
                }
            }
        }
    }
}