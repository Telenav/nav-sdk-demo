package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.telenav.map.api.Annotation
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.demo.util.BitmapUtils
import com.telenav.sdk.drivesession.listener.AlertEventListener
import com.telenav.sdk.drivesession.model.AlertEvent
import com.telenav.sdk.drivesession.model.alert.AlertItem
import com.telenav.sdk.drivesession.model.alert.CameraInfo
import com.telenav.sdk.drivesession.model.drg.BetterRouteContext
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentNavDrawPoiBinding
import com.telenav.sdk.map.SDK
import com.telenav.sdk.map.direction.model.Route
import kotlinx.android.synthetic.main.content_basic_navigation.*

/**
 * A simple [Fragment] Draw the POI in the navigation
 * @author wu.changzhong on 2021/7/13
 */
class NavDrawPoIFragment : BaseNavFragment(), AlertEventListener {
    private var destinationLocation: Location? = null
    private val cameraInfoMap: HashMap<Long, CameraInfo> = hashMapOf()
    private val drawPoiAnnotationMap: HashMap<Long, Annotation> = HashMap()
    private var lastCameraAlterItems: List<AlertItem> = listOf()
    private var userGraphic: Annotation.UserGraphic? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentNavDrawPoiBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_nav_draw_poi, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        driveSession.enableAlert(true)
        driveSession.eventHub.addAlertEventListener(this)
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)//high way can be seen clearly on DayMode
    }

    override fun onDestroyView() {
        driveSession.eventHub.removeAlertEventListener(this)
        super.onDestroyView()
    }

    override fun getDemonstrateSpeed(): Double {
        return 240.0 // set speed faster
    }

    override fun onLongClick(location: Location?) {
        if (destinationLocation == null) {
            destinationLocation = location
            destinationLocation?.latitude = 66.128973
            destinationLocation?.longitude = -18.897212
            // Location[ 66.077884,-18.708822 acc=??? t=?!? et=?!?]
            /**
             * set the location of vehicle at high way
             */
            location?.let { locationProvider.setLocation(it) }

        } else {
            location?.latitude = 66.077884
            location?.longitude = -18.708822
            super.onLongClick(location)
        }

    }

    override fun onAlertEventUpdated(alertEvent: AlertEvent) {
        Log.d(TAG, "onAlertEventUpdated aheadHighwayInfoItems:${alertEvent.aheadHighwayInfoItems}")
        val aheadAlertItems = alertEvent.aheadAlertItems
        val currentCameraAlterItems = aheadAlertItems?.filter {
            it.cameraInfo != null
        }

        lastCameraAlterItems?.forEach {
            val id = it.id
            val exist = isExistInCurrentAlertItem(id, currentCameraAlterItems)
            if (!exist) {
                map_view.annotationsController().remove(arrayListOf(drawPoiAnnotationMap[id]))
            }
        }

        currentCameraAlterItems?.let { it ->
            activity?.runOnUiThread {
                it.forEach { item ->
                    var id = item.id;
                    val cameraInfo = cameraInfoMap[id]
                    if (cameraInfo == null && item.cameraInfo != null) {
                        activity?.runOnUiThread {
                            // Set annotation at location
                            if (context != null) {
                                val factory = map_view.annotationsController().factory()
                                val location = Location("")
                                location.latitude = item?.location?.lat!!
                                location.longitude = item?.location!!.lon
                                if (userGraphic == null) {
                                    var bitmapFromVectorDrawable = BitmapUtils.getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_ico_cameracamera)
                                    userGraphic = Annotation.UserGraphic(bitmapFromVectorDrawable!!)
                                }
                                if (userGraphic != null) {
                                    var annotation = factory.create(requireContext(), userGraphic!!, location)
                                    annotation.displayText = Annotation.TextDisplayInfo.Centered("camera")
                                    annotation.style = Annotation.Style.ScreenAnnotationFlagNoCulling
                                    drawPoiAnnotationMap[id] = annotation
                                    map_view.annotationsController().add(arrayListOf(annotation))
                                    cameraInfoMap[id] = item.cameraInfo!!
                                }
                            }
                        }
                    }
                }
                lastCameraAlterItems = currentCameraAlterItems
            }
        }

    }

    override fun onNavigationRouteUpdated(route: Route, contextBetter : BetterRouteContext?) {
        super.onNavigationRouteUpdated(route, contextBetter)
        cameraInfoMap.clear()
        drawPoiAnnotationMap.clear()
        lastCameraAlterItems = listOf()
    }

    private fun isExistInCurrentAlertItem(id: Long, cameraAlertItems: List<AlertItem>?): Boolean {
        val alertItem = cameraAlertItems?.find { it.id == id }
        return alertItem != null
    }
}