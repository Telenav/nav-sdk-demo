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
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.map.api.controllers.Feature
import com.telenav.sdk.demo.R
import kotlinx.android.synthetic.main.fragment_map_view_elements.*
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.layout_content_map.*
import kotlinx.android.synthetic.main.layout_operation_elements.*

/**
 * This fragment shows how to show and hide elements.
 * @author zhai.xiang on 2021/1/28
 */
class MapViewElementsFragment : Fragment() {
    private lateinit var viewModel: MapViewNavViewModel
    private val elementOperations = listOf(
            ElementOperationItem("Traffic"){
                setTraffic(it)
            },
            ElementOperationItem("Landmarks"){
                setLandmarks(it)
            },
            ElementOperationItem("Buildings"){
                setBuildings(it)
            },
            ElementOperationItem("Terrain"){
                setTerrain(it)
            },
            ElementOperationItem("Globe"){
                setGlobe(it)
            },
            ElementOperationItem("Compass"){
                setCompass(it)
            },
            ElementOperationItem("ADI Line"){
                setADILine(it)
            },
            ElementOperationItem("Scale Bar"){
                setScaleBar(it)
            },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_view_elements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))
                .get(MapViewNavViewModel::class.java)
        viewModel.route.observe(viewLifecycleOwner){
            if (it != null){
                startNavButton.isEnabled = true
                stopNavButton.isEnabled = false
            }
        }
        viewModel.currentVehicleLocation.observe(viewLifecycleOwner){
            mapView.vehicleController().setLocation(it)
        }
        tv_title.text = getString(R.string.title_activity_map_view_elements)
        mapView.initialize(savedInstanceState, null)
        viewModel.requestDirection()
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        btn_show_menu.setOnClickListener {
            drawer_layout.open()
        }
        setupDrawerOperations()
    }

    private fun setupDrawerOperations() {
        recyclerElements.adapter = ElementAdapter()
        recyclerElements.layoutManager = LinearLayoutManager(activity)
        recyclerElements.addItemDecoration(DividerItemDecoration(activity,LinearLayoutManager.VERTICAL))

        startNavButton.setOnClickListener {
            viewModel.startNavigation(mapView)
            startNavButton.isEnabled = false
            stopNavButton.isEnabled = true
        }

        stopNavButton.setOnClickListener {
            viewModel.stopNavigation(mapView)
            startNavButton.isEnabled = true
            stopNavButton.isEnabled = false
        }

    }

    /**
     * this method show or hide traffic
     */
    private fun setTraffic(on : Boolean){
        setFeature(mapView.featuresController().traffic(), on)
    }

    /**
     * this method show or hide landmarks
     */
    private fun setLandmarks(on : Boolean){
        setFeature(mapView.featuresController().landmarks(), on)
    }

    /**
     * this method show or hide building
     */
    private fun setBuildings(on : Boolean){
        setFeature(mapView.featuresController().buildings(), on)
    }

    /**
     * this method show or hide terrain
     */
    private fun setTerrain(on : Boolean){
        setFeature(mapView.featuresController().terrain(), on)
    }

    /**
     * this method show or hide globe
     */
    private fun setGlobe(on : Boolean){
        setFeature(mapView.featuresController().globe(), on)
    }

    /**
     * this method show or hide compass
     */
    private fun setCompass(on : Boolean){
        setFeature(mapView.featuresController().compass(), on)
    }

    /**
     * this method show or hide adi line
     * Notice: routesController().updateRouteProgress should also be called if adi line
     */
    private fun setADILine(on : Boolean){
//        setFeature(mapView.featuresController().adiLine(), on)
    }

    /**
     * this method show or hide scale bar
     */
    private fun setScaleBar(on : Boolean){
        setFeature(mapView.featuresController().scaleBar(), on)
    }

    private fun setFeature(feature: Feature, on: Boolean){
        if (on){
            feature.setEnabled()
        }else{
            feature.setDisabled()
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


    inner class ElementAdapter() : RecyclerView.Adapter<ElementViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_map_view_element_item,parent,false)
            return ElementViewHolder(view).apply {
                this.sw.setOnCheckedChangeListener(onCheckedChange)
            }
        }

        override fun onBindViewHolder(holder: ElementViewHolder, position: Int) {
            holder.bindTo(elementOperations[position])
        }

        override fun getItemCount(): Int = elementOperations.size

        private val onCheckedChange = CompoundButton.OnCheckedChangeListener{ buttonView, isChecked ->
            val position = recyclerElements.getChildAdapterPosition(buttonView.parent as View)
            if (position >= 0 && position < elementOperations.size){
                elementOperations[position].operation(isChecked)
            }
        }
    }

    inner class ElementViewHolder(view : View) : RecyclerView.ViewHolder(view){
        private val tvTitle : TextView = view.findViewById(R.id.tv_title)
        val sw : SwitchCompat = view.findViewById(R.id.sc)

        fun bindTo(item : ElementOperationItem){
            tvTitle.text = item.text
        }
    }

    data class ElementOperationItem(val text:String, val operation: (Boolean)-> Unit)

}