package com.telenav.sdk.examples.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.examples.util.AdasMessageDecoder
import com.telenav.sdk.examples.util.SpeedLimitPoint
import com.telenav.sdk.drivesession.listener.ADASEventListener
import com.telenav.sdk.drivesession.listener.AlertEventListener
import com.telenav.sdk.drivesession.model.AlertEvent
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.drivesession.model.SpeedLimitType
import com.telenav.sdk.drivesession.model.adas.AdasMessage
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentAdasMessageBinding
import com.telenav.sdk.map.SDK


/**
 * This fragment shows how to:
 * 1. enable or disable ADAS feature
 * 2. listen to ADAS messages
 * 3. decoding ADAS messages to speed limit information
 * @author zhai.xiang on 2021/2/2
 */
class AdasMessageFragment : BaseNavFragment<FragmentAdasMessageBinding>(), AlertEventListener, ADASEventListener {

    private var destinationLocation: Location? = null
    private val viewModel: AdasMessageViewModel by viewModels()
    private val decoder: AdasMessageDecoder = AdasMessageDecoder()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdasMessageBinding {
        return FragmentAdasMessageBinding.inflate(inflater, container, false)
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.contentBasicNavigation
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        driveSession.eventHub.addAlertEventListener(this)
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        navigationOn.observe(viewLifecycleOwner) {
            viewModel.showNavigationDetails.postValue(it)
            if (!it) {
                destinationLocation = null
            }
        }
        binding.recyclerAdas.adapter = viewModel.adasAdapter
        binding.recylcerAlert.adapter = viewModel.alertAdapter
        binding.scAdas.setOnCheckedChangeListener { _, isChecked ->
            enableAdas(isChecked)
            listenAdasMessage(isChecked)
        }
    }

    /**
     * This method shows how to enable or disable adas
     */
    private fun enableAdas(on: Boolean) {
    }

    /**
     * This method shows to listen or cancel listen adas message
     */
    private fun listenAdasMessage(on: Boolean) {
        if (on) {
            driveSession.eventHub.addADASEventListener(this)
        } else {
            driveSession.eventHub.removeADASEventListener(this)
        }
    }

    override fun onDestroyView() {
        driveSession.eventHub.removeADASEventListener(this)
        driveSession.eventHub.removeAlertEventListener(this)
        super.onDestroyView()
    }

    override fun onADASEventUpdated(adasMessageList: List<AdasMessage>) {
        Log.i(TAG, "onADASEventUpdated: " + adasMessageList.size)
        activity?.runOnUiThread {
            viewModel.adasList.clear()
            viewModel.adasList.add("adas info")

            decodeAdasSpeedLimitMessage(adasMessageList).forEach { item ->
                val text = getSpeedLimit(item)
                if (!TextUtils.isEmpty(text)) {
                    viewModel.adasList.add(getSpeedLimit(item))
                }
            }

            viewModel.adasList.add("country code: ${decoder.getCountryCode()}")
            viewModel.adasAdapter.notifyDataSetChanged()
        }
    }

    /**
     * this method shows an example of decoding adas message.
     */
    private fun decodeAdasSpeedLimitMessage(adasMessageList: List<AdasMessage>): MutableList<SpeedLimitPoint> {
        val list = ArrayList<SpeedLimitPoint>()
        decoder.addMessageList(adasMessageList)
        list.addAll(decoder.decodeByType(SpeedLimitType.TIME))
        list.addAll(decoder.decodeByType(SpeedLimitType.RAINY))
        list.addAll(decoder.decodeByType(SpeedLimitType.SNOWY))
        list.addAll(decoder.decodeByType(SpeedLimitType.FOGGY))
        return list
    }

    private fun getSpeedLimit(point: SpeedLimitPoint): String {
        if (point.isNoSpeedData()) {
            return ""
        }

        val unitString = if (point.isMetric()) {
            "km/h"
        } else {
            "mile/h"
        }

        return "type: ${point.limitType}, speed: ${point.speed}${unitString}, distance: ${point.distance}, index : ${point.segmentIndex}"
    }

    override fun getDemonstrateSpeed(): Double {
        return 50.0
    }

    override fun onLongClick(location: Location?) {
        if (destinationLocation == null) {
            destinationLocation = location
            /**
             * set the location of vehicle at high way
             */
            location?.let { locationProvider.setLocation(it) }
        } else {
            super.onLongClick(location)
        }
    }

    override fun onAlertEventUpdated(alertEvent: AlertEvent) {
        alertEvent.aheadAlertItems?.let { it ->
            activity?.runOnUiThread {
                viewModel.alertList.clear()
                viewModel.alertList.add("alert info")

                it.forEach { item ->
                    val sb = StringBuilder()
                    sb.append("type: ${item.basicInfo.type}").append("\n")
                    sb.append("speedLimit: ${item.speedLimit}").append("\n")
                    sb.append("distanceToVehicle: ${item.basicInfo.distanceToVehicle}").append("\n")
                    sb.append("length: ${item.zoneInfo?.length} ").append("\n")
                    sb.append("enter: ${item.zoneInfo?.enteredDistance} ").append("\n")
                    viewModel.alertList.add(sb.toString())
                }

                viewModel.alertAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        super.onLocationUpdated(vehicleLocation, positionInfo)
        Log.i(TAG, "speed limit: ${positionInfo.currentRoad?.speedLimit?.value ?: -1}  ")
    }


}