/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.telenav.map.api.Annotation
import com.telenav.sdk.demo.R
import kotlinx.android.synthetic.main.content_basic_navigation.*

/**
 * A simple [Fragment] for the flow of stop point
 * @author tang.hui on 2021/1/25
 */
class StopPointFragment : BaseNavFragment() {

    private var destinationLocation: Location? = null
    private var wayPointList = mutableListOf<Location>()
    private var wayPointAnnotationList = mutableListOf<Annotation>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nav_stop_point, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationOn.observe(viewLifecycleOwner) {
            if (!it) {
                destinationLocation = null
                wayPointList.clear()
                wayPointAnnotationList.clear()
            }
        }
    }

    override fun onLongClick(location: Location?) {
        if (location == null) {
            return
        }
        val annotation: Annotation?
        if (destinationLocation == null) {
            destinationLocation = location
            val factory = map_view.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.map_pin_green_icon_unfocused, location)
            annotation?.displayText = Annotation.TextDisplayInfo.Centered("Destination")
            map_view.annotationsController().add(arrayListOf(annotation))
            vehicleLocation?.let { requestDirection(it, location) }
        } else {
            val factory = map_view.annotationsController().factory()
            annotation = factory.create(requireContext(), R.drawable.add_location, location)
            annotation?.displayText = Annotation.TextDisplayInfo.Centered("wayPoint" + (wayPointList.size + 1))
            map_view.annotationsController().add(arrayListOf(annotation))
            activity?.runOnUiThread {
                AlertDialog.Builder(StopPointFragment@ this.requireContext())
                        .setCancelable(false)
                        .setIcon(R.drawable.add_location)
                        .setTitle("New Annotation")
                        .setPositiveButton("add wayPoint") { dialog, whichButton ->
                            dialog.dismiss()
                            wayPointList.add(wayPointList.size, location)
                            wayPointAnnotationList.add(annotation)
                            if (vehicleLocation != null) {
                                requestDirection(vehicleLocation!!, destinationLocation!!, wayPointList)
                            }
                        }
                        .setNegativeButton("new destination") { dialog, whichButton ->
                            dialog.dismiss()
                            destinationLocation = null
                            wayPointList.clear()
                            wayPointAnnotationList.clear()
                            map_view.annotationsController().clear()
                            onLongClick(location)
                        }
                        .setNeutralButton("cancel") { dialog, whichButton ->
                            dialog.dismiss()
                            map_view.annotationsController().remove(arrayListOf(annotation))
                        }
                        .show()
            }
        }

    }

    override fun onNavigationStopReached(stopIndex: Int, stopLocation: Int) {
        super.onNavigationStopReached(stopIndex, stopLocation)
        if (stopIndex >= 0 && stopIndex < wayPointAnnotationList.size) {
            map_view.annotationsController().remove(arrayListOf(wayPointAnnotationList[stopIndex]))
        }
    }


}