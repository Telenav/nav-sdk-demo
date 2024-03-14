/*
 * Copyright © 2018 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.search

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.telenav.map.api.Annotation
import com.telenav.map.api.MapViewInitConfig
import com.telenav.map.api.controllers.AnnotationsController
import com.telenav.map.api.touch.TouchPosition
import com.telenav.map.api.touch.TouchType
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.examples.util.BitmapUtils
import com.telenav.sdk.entity.api.EntityService
import com.telenav.sdk.entity.model.base.EntityType
import com.telenav.sdk.entity.model.search.*
import com.telenav.sdk.entity.utils.EntityJsonConverter
import com.telenav.sdk.examples.R
import com.telenav.sdk.map.SDK
import kotlinx.android.synthetic.main.fragment_search_rgc.mapView
import kotlinx.android.synthetic.main.layout_action_bar.*
import kotlinx.coroutines.*


class SearchRgcFragment : Fragment() {
    private val annotationList = ArrayList<Annotation>()
    private var searchJob: Deferred<EntitySearchResponse?>? = null
    private var annotationController: AnnotationsController? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_rgc, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tv_title.text = getString(R.string.title_activity_search_rgc)
        iv_back.setOnClickListener {
            findNavController().navigateUp()
        }

        mapViewInit(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun mapViewInit(savedInstanceState: Bundle?) {
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        val mapViewConfig = MapViewInitConfig(
            context = requireContext().applicationContext,
            lifecycleOwner = viewLifecycleOwner,
            readyListener = {
                annotationController = it.annotationsController()
            }
        )
        mapView.initialize(mapViewConfig)

        mapView.setOnTouchListener { touchType: TouchType, position: TouchPosition ->
            if (touchType == TouchType.LongClick) {
                startSearch(position.geoLocation!!)
            }
        }

        mapView.setOnAnnotationTouchListener { touchType, position, touchedAnnotations ->
            if (touchType == TouchType.Click) {
                annotationList.removeAll(touchedAnnotations.map { it.annotation })
                annotationController?.remove(touchedAnnotations.map { it.annotation })
            }
        }
    }

    private fun CoroutineScope.searchRgc(location: Location) = async(Dispatchers.Default) {
        val searchOptions = SearchOptions.builder()
            .setIntent(SearchOptions.Intent.REVERSE_GEOCODING)
            .build()
        val entityClient = EntityService.getClient()
        try {
            return@async entityClient.searchRequest()
                .setSearchOptions(searchOptions)
                .setLocation(location.latitude, location.longitude)
                .execute()
        } catch (e: Throwable) {
            e.printStackTrace()
            return@async null
        }
    }

    private fun startSearch(location: Location) {
        CoroutineScope(Dispatchers.Main).launch {
            if (searchJob != null && !searchJob!!.isActive) {
                searchJob?.cancelAndJoin()
            }

            searchJob = searchRgc(location)
            searchJob?.await()?.let {
                onSearchSuccess(it)
            }
        }
    }

    private fun onSearchSuccess(response: EntitySearchResponse) {
        Log.i("sdk", EntityJsonConverter.toPrettyJson(response))
        val factory = annotationController!!.factory()

        response.results?.let { list ->
            annotationList.addAll(
                list.map { entity ->
                    val layout: View = LayoutInflater.from(requireActivity()).inflate(R.layout.layout_annotation_rgc,
                        null, false)
                    layout.findViewById<TextView>(R.id.tvSource).text = response.responseType?.name
                        ?: ""
                    layout.findViewById<TextView>(R.id.tvType).text = entity.type?.name
                        ?: "Type UnKnown"
                    val location = Location("")
                    if (entity.type == EntityType.ADDRESS) {
                        layout.findViewById<TextView>(R.id.tvAddress).text = entity.address?.formattedAddress
                            ?: ""
                        location.longitude = entity.address?.geoCoordinates?.longitude ?: 0.0
                        location.latitude = entity.address?.geoCoordinates?.latitude ?: 0.0
                    } else if (entity.type == EntityType.PLACE) {
                        layout.findViewById<TextView>(R.id.tvAddress).text = entity.place?.address?.formattedAddress
                            ?: ""
                        location.longitude = entity.place?.address?.geoCoordinates?.longitude
                            ?: 0.0
                        location.latitude = entity.place?.address?.geoCoordinates?.latitude
                            ?: 0.0
                    }

                    val bitmap = BitmapUtils.createBitmapFromView(layout)
                    return@map factory.create(requireContext(), Annotation.UserGraphic(bitmap), location).apply {
                        this.style = Annotation.Style.ScreenAnnotationFlagNoCulling
                        this.iconY = -0.5
                    }
                }
            )

            annotationController?.add(annotationList)
        }
    }

}