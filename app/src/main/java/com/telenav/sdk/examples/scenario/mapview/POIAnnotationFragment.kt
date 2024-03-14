/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.mapview

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.map.api.Annotation
import com.telenav.map.api.MapView
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.MapViewReadyListener
import com.telenav.map.api.POIAnnotationParams
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.controllers.Camera
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.map.api.touch.listeners.TouchListener
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.PoiAnnotationFragmentBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var _binding: PoiAnnotationFragmentBinding? = null
    private val binding get() = _binding!!

    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)
    private val viewModel by viewModels<POIAnnotationViewModel>()
    private var annotationsController: AnnotationsController? = null
    private val mapViewReadyListener = MapViewReadyListener<MapView>() {
        runInMain {
            binding.mapView.cameraController().position =
                Camera.Position.Builder().setLocation(viewModel.baseLocation).build()
            binding.mapView.vehicleController().setLocation(viewModel.baseLocation)
            annotationsController = binding.mapView.annotationsController()
            binding.mapView.setOnTouchListener(TouchListener { touchType, position ->
                handleMapTouch(touchType, position)
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PoiAnnotationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.include.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = mapViewReadyListener
        )
        binding.mapView.initialize(mapViewConfig)

        val onItemClickListener = View.OnClickListener {
            updateStyleTextValue((it as TextView).text)
        }

        val clearAnnotationsClickListener = View.OnClickListener {
            binding.mapView.annotationsController().clear()
        }

        binding.recyclerviewEvStyles.adapter =
            POIListAdapter(viewModel.poiCategories, onItemClickListener)
        binding.recyclerviewEvStyles.layoutManager = LinearLayoutManager(context)
        binding.recyclerviewEvStyles.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.btnClearAnnotations.setOnClickListener(clearAnnotationsClickListener)

        binding.btnRefresh.setOnClickListener {
            binding.tvAnnotattionSize.text = "${binding.mapView.getAnnotationsController()?.current()?.size ?: 0}"
        }
        binding.btnRemove.setOnClickListener {
            if (annotationsCache.isEmpty()) return@setOnClickListener
            binding.mapView.getAnnotationsController()?.remove(listOf(annotationsCache.last()))
            annotationsCache.remove(annotationsCache.last())
        }
        binding.btnUpdate.setOnClickListener {
            updateAnnotationResources(count++)
        }
    }

    private var count = 0
    private var icon_size = 25.0f

    private val testAnnotations = mutableListOf<Annotation?>()

    //update annotation graphic resource used to verify graphic resource management

    //Note ！！！  just for test usage, some of the annotation texture resources do not supporting change use this way
    private fun updateAnnotationResources(count: Int) {

        val updatedAnnotations = testAnnotations.map { annotation ->
            // Ensure that the annotation is not null
            annotation?.let {
                // Get the bitmap safely
                if (count % 3 == 0) {
                    annotation.updateFloatValue("custom-size",icon_size)
                    icon_size += 5
                } else {
                    val iconResId = if (count % 2 == 0) {
                        R.drawable.map_pin_orange_icon_unfocused
                    } else {
                        R.drawable.map_pin_red_icon_unfocused
                    }

                    val bitmap = BitmapUtils.getBitmapFromVectorDrawable(
                        requireContext(),
                        iconResId
                    )

                    bitmap?.let { bmp ->
                        // Update the userGraphic property
                        annotation.userGraphic = Annotation.UserGraphic(bmp)
                    }
                }
            }
            annotation
        }

        if (updatedAnnotations.isNotEmpty()) {
            binding.mapView.getAnnotationsController()?.update(updatedAnnotations)
        }

    }


    //create all kind of Annotations
    private fun createAllKindsAnnotations(location: Location) {

        testAnnotations.clear()

        val annotation1 = annotationsController?.factory()?.create(
            requireContext(),
            R.drawable.map_pin_green_icon_unfocused,
            location.apply { latitude += 0.001 })
        annotation1?.style = Annotation.Style.ScreenAnnotationFlagNoCulling
        annotation1?.type = Annotation.Type.Screen2D

        val annotation2 = annotationsController?.factory()?.create(
            requireContext(),
            R.drawable.map_pin_red_icon_unfocused,
            location.apply { longitude += 0.001 })
        annotation2?.style = Annotation.Style.ScreenAnnotationFlagNoCulling
        annotation2?.type = Annotation.Type.Screen2D
        annotation2?.userGraphic = Annotation.UserGraphic(
            BitmapUtils.getBitmapFromVectorDrawable(
                requireContext(),
                R.drawable.map_pin_green_icon_unfocused
            )!!
        )


        /**
         * texture can not be change case
         * layer<annotation> annotation-traffic-lights[annotation-data="alert_annotations.Alert_TrafficLights"][zoom>=13]
         * {
         *   icon-image         : "traffic-lights.png";
         *   icon-padding       : @icon-padding-global;
         *   icon-opacity       : @annotation-poi-icon-opacity;
         *   icon-size          : stepped(zoom, [15: 7, 16: 8, 17: 9, 18: 10]);
         *   icon-placement     : billboard;
         *   priority           : @priority-annotation-default;
         *   collision-enabled  : enabled;
         * };
         */
        val annotation3 = annotationsController?.factory()?.create(
            POIAnnotationParams(
                "alert_annotations.Alert_TrafficLights",
                location.apply {
                    latitude -= 0.001
                    longitude -= 0.001
                },
                "text"
            )
        )
        annotation3?.type = Annotation.Type.ViewerFacing


        val annotation4 = annotationsController?.factory()?.create(requireContext(),
            Annotation.UserGraphic(
                BitmapUtils.getBitmapFromVectorDrawable(
                    requireContext(),
                    R.drawable.map_pin_green_icon_unfocused
                )!!
            ), location.apply {
                latitude -= 0.001
            })
        annotation4?.style = Annotation.Style.ScreenAnnotationFlagNoCulling
        annotation4?.type = Annotation.Type.Screen2D


        /**
         * texture can be change case
         * [[annotation-data="screen_annotations.custom_flag_screen_annotation"]
         * {
         *   priority             : 490 + $custom-priority;
         *   icon-image           : $texture              ;
         *   icon-size            : $custom-size          ;
         *   text                 : $custom-text          ;
         *   icon-anchor-position : $custom-anchor-offset ;
         *   text-font            : @text-medium          ;
         *   text-size            : $custom-text-size     ;
         *   text-position-offset : $custom-text-offset   ;
         *   text-color           : $custom-text-color    ;
         *   text-max-width       : 30       ;
         *   icon-margin          : animated(zoom,[1:15, 15:10, 18:0]);
         *   icon-layer-type      : "Hud"    ;
         *   model-layer-type     : "Hud"    ;
         *   text-layer-type      : "Hud"    ;
         *   };
         */
        val annotation5 = annotationsController?.factory()?.create(
            POIAnnotationParams(
                "screen_annotations.custom_flag_screen_annotation_no_culling",
                location.apply {
                    longitude -= 0.001
                },
                ""
            )
        )
        annotation5?.type = Annotation.Type.Screen2D
        annotation5?.userGraphic = Annotation.UserGraphic(
            BitmapUtils.getBitmapFromVectorDrawable(
                requireContext(),
                R.drawable.map_pin_green_icon_unfocused
            )!!
        )

        testAnnotations.add(annotation1)
        testAnnotations.add(annotation2)
        testAnnotations.add(annotation3)
        testAnnotations.add(annotation4)
        testAnnotations.add(annotation5)

        annotationsController?.add(testAnnotations)

    }

    private val annotationsCache = mutableListOf<Annotation>()

    private fun handleMapTouch(touchType: TouchType, touchPosition: TouchPosition) {
        TaLog.d(TAG, "handleMapTouch touchType = $touchType, touchPosition = $touchPosition")
        if (touchType == TouchType.LongClick) {
            annotationsController?.let { annotationsControllerNonNull ->
                try {
                    if (binding.etAnnotationStyle.text.toString().isEmpty()) {
                        createAllKindsAnnotations(
                            touchPosition.geoLocation ?: viewModel.defaultAnnotationLocation,
                        )
                    } else {
                        val annotation = viewModel.createPOIAnnotation(
                            annotationsController = annotationsControllerNonNull,
                            location = touchPosition.geoLocation
                                ?: viewModel.defaultAnnotationLocation,
                            styleKey = binding.etAnnotationStyle.text.toString(),
                            text = binding.etAnnotationText.text.toString(),
                        )
                        if (binding.etBubbleType.text.toString().isNotEmpty()) {
                            annotation.updateFloatValue(
                                binding.etAnnotationStyle.text.toString(),
                                binding.etBubbleType.text.toString().toFloat()
                            )
                        }

                        binding.mapView.getAnnotationsController()?.add(listOf(annotation))
                        annotationsCache.add(annotation)
                    }
                } catch (ex: Exception) {
                    TaLog.e(TAG, "${ex.message}", ex)
                }
            }
        }
    }

    private fun updateStyleTextValue(text: CharSequence) {
        binding.etAnnotationStyle.setText(text)
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

    private fun runInMain(run: () -> Unit): Job {
        return mainCoroutineScope.launch {
            run()
        }
    }

}