/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.mapview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.examples.R
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.android.synthetic.main.poi_annotation_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * Example of the API usage for creating custom POI annotations.
 *
 * @author Dmytro Lavrikov
 */
class POIAnnotationFragment : Fragment() {

    companion object {
        private const val TAG = "POIAnnotationFragment"
        fun newInstance() = POIAnnotationFragment()
    }

    private lateinit var viewModel: POIAnnotationViewModel
    private lateinit var annotationsController: AnnotationsController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.poi_annotation_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(POIAnnotationViewModel::class.java)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }
        mapViewInit(savedInstanceState)

        val onItemClickListener = View.OnClickListener {
            updateStyleTextValue((it as TextView).text)
        }

        val clearAnnotationsClickListener = View.OnClickListener {
            mapView.annotationsController().clear()
        }

        recyclerview_ev_styles.adapter =
            POIListAdapter(viewModel.poiCategories, onItemClickListener)
        recyclerview_ev_styles.layoutManager = LinearLayoutManager(context)
        recyclerview_ev_styles.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        btn_clear_annotations.setOnClickListener(clearAnnotationsClickListener)
    }

    private fun handleMapTouch(touchType: TouchType, touchPosition: TouchPosition) {
        TaLog.d(TAG, "handleMapTouch touchType = $touchType, touchPosition = $touchPosition")
        if (touchType == TouchType.Click) {
            val annotation = viewModel.createPOIAnnotation(
                annotationsController = annotationsController,
                location = touchPosition.geoLocation ?: viewModel.defaultAnnotationLocation,
                styleKey = et_annotation_style.text.toString(),
                text = et_annotation_text.text.toString()
            )
            mapView.annotationsController().add(mutableListOf(annotation))
        }
    }

    // the initialize function must be called after SDK is initialized
    private fun mapViewInit(savedInstanceState: Bundle?) {
        mapView.initialize(savedInstanceState) {
            CoroutineScope(Dispatchers.Main).launch {
                mapView.cameraController()?.position =
                    Camera.Position.Builder().setLocation(viewModel.baseLocation).build()
                mapView.vehicleController()?.setLocation(viewModel.baseLocation)
                annotationsController = mapView.annotationsController()
                mapView.setOnTouchListener(TouchListener { touchType, position ->
                    handleMapTouch(touchType, position)
                })
            }
        }
    }

    private fun updateStyleTextValue(text: CharSequence) {
        et_annotation_style.setText(text)
    }

    private class POIListAdapter(
        val poiList: List<String>,
        val itemClickListener: View.OnClickListener
    ) : RecyclerView.Adapter<POIListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIListViewHolder {
            return POIListViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_1, parent, false)
            )
        }

        override fun onBindViewHolder(holder: POIListViewHolder, position: Int) {
            holder.text.text = poiList[position]
            holder.text.setOnClickListener(itemClickListener)
        }

        override fun getItemCount(): Int {
            return poiList.size
        }
    }

    private class POIListViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val text: TextView = root.findViewById(android.R.id.text1)
    }

}