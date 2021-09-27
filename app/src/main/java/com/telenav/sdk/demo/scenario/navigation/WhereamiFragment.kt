/*
 *  Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.telenav.sdk.drivesession.listener.AlertEventListener
import com.telenav.sdk.drivesession.model.AlertEvent
import com.telenav.sdk.drivesession.model.StreetInfo
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.FragmentNavWhereamiBinding
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.android.synthetic.main.fragment_nav_whereami.*

/**
 * A simple [Fragment] for whereami and location provider
 * @author tang.hui on 2021/1/22
 */
class WhereamiFragment : BaseNavFragment(), AlertEventListener {

    private val viewModel: WhereamiViewModel by viewModels()
    var isSettingLocation: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentNavWhereamiBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_nav_whereami, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        driveSession.enableAlert(true)
        driveSession.eventHub.addAlertEventListener(this)
        navigationOn.observe(viewLifecycleOwner) {
            if (!it) routes?.clear()
        }
        setVehicleLocation.setOnClickListener {
            navigationSession?.stopNavigation()
            map_view.annotationsController().clear()
            map_view.routesController().clear()
            map_view.cameraController().disableFollowVehicle()
            navButton.setText(R.string.stop_navigation)
            navButton.isEnabled = false
            navigating = false
            isSettingLocation = true
        }
    }

    override fun onLongClick(location: Location?) {
        if (isSettingLocation) {
            /**
             * change the location of vehicle
             */
            location?.let { locationProvider.setLocation(it) }
            isSettingLocation = false
        } else {
            super.onLongClick(location)
        }
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        super.onStreetUpdated(curStreetInfo, drivingOffRoad)
        curStreetInfo.streetName?.let {
            viewModel.currentStreetName.postValue("current street:$it")
        }

        (curStreetInfo.adminInfo)?.let {
            viewModel.countryLiveData.postValue("current country: ${it.countryCode}")

            var cityName = it.city
            if (cityName.isNullOrEmpty()) {
                cityName = it.subCity
            }

            if (!cityName.isNullOrEmpty()) {
                viewModel.countyLiveData.postValue("current county: $cityName")
            }
        }

        if (drivingOffRoad) {
            viewModel.roadStateLiveData.postValue("off-road state:off-road")
        } else {
            viewModel.roadStateLiveData.postValue("off-road state:along road")
        }
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        super.onLocationUpdated(vehicleLocation)
        viewModel.onLocationUpdated(vehicleLocation)
    }

    override fun onAlertEventUpdated(alertEvent: AlertEvent) {
        val userPositionInfo = alertEvent.userPositionInfo
        userPositionInfo?.apply {
            var previousDistanceToVehicle = previousIntersection?.run { distanceToVehicle }
            var nextDistanceToVehicle = nextIntersection?.run { distanceToVehicle }
            var previousContent = previousIntersection?.currentRoadInfo?.name?.orthography?.content
            var nextContent = nextIntersection?.currentRoadInfo?.name?.orthography?.content
            var closetStreetName = closetStreetInfo?.name?.orthography?.content
            var closetStreetDistance = closetStreetInfo?.distanceToVehicle
            previousDistanceToVehicle.let { viewModel.previousStreetDistanceToVehicle.postValue("previousStreetDistance:${getDesDistance(it)}") }
            nextDistanceToVehicle.let { viewModel.nextStreetDistanceToVehicle.postValue("nextStreetDistance:${getDesDistance(it)}") }
            previousContent.let { viewModel.previousStreetName.postValue("previousStreet:${it ?: ""}") }
            nextContent.let { viewModel.nextStreetName.postValue("nextStreet:${it ?: ""}") }
            closetStreetName.let { viewModel.closetStreetName.postValue("closetStreet:${it ?: ""}") }
            closetStreetDistance.let { viewModel.closetStreetDistance.postValue("closetStreetDistance:${getDesDistance(it)}") }
        }
    }

    override fun onDestroyView() {
        driveSession.eventHub.removeAlertEventListener(this)
        super.onDestroyView()
    }

    private fun getDesDistance(meters: Int?): String? {
        return if (meters != null){
            "$meters meters"
        }else{
            ""
        }
    }

}