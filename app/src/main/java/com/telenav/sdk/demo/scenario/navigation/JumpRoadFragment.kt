/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.telenav.map.api.controllers.Camera
import com.telenav.sdk.common.model.DayNightMode
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.util.DemoLocationProvider
import com.telenav.sdk.drivesession.DriveSession
import com.telenav.sdk.drivesession.listener.PositionEventListener
import com.telenav.sdk.drivesession.model.CandidateRoadInfo
import com.telenav.sdk.drivesession.model.RoadCalibrator
import com.telenav.sdk.drivesession.model.StreetInfo
import com.telenav.sdk.map.SDK
import kotlinx.android.synthetic.main.content_basic_navigation.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * This fragment shows how to use jump road.
 * @author zhai.xiang on 2021/2/2
 */
class JumpRoadFragment : Fragment(), PositionEventListener {
    private lateinit var viewModel : JumpRoadViewModel
    private var roadCalibrator: RoadCalibrator? = null
    private var candidateRoadDialogFragment: JumpRoadDialogFragment? = null
    val driveSession: DriveSession = DriveSession.Factory.createInstance()
    var vehicleLocation: Location? = null
    private var locations:List<Location> = ArrayList()
    val locationProvider: DemoLocationProvider = DemoLocationProvider()
    private var job: Job? = null

    init {
        driveSession.injectLocationProvider(locationProvider)
        driveSession.eventHub?.let {
            it.addPositionEventListener(this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_jump_road, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SDK.getInstance().updateDayNightMode(DayNightMode.DAY)
        viewModel = ViewModelProvider(requireActivity(), ViewModelProvider.NewInstanceFactory()).get(JumpRoadViewModel::class.java)
        viewModel.selectedRoad.observe(owner = viewLifecycleOwner) {
            selectCandidateRoad(it)
        }
        map_view.initialize(savedInstanceState){
            map_view.featuresController().traffic().setEnabled()
            map_view.featuresController().landmarks().setEnabled()
            map_view.featuresController().buildings().setEnabled()
            map_view.featuresController().terrain().setEnabled()
            map_view.featuresController().globe().setDisabled()
            map_view.featuresController().compass().setEnabled()
            map_view.featuresController().scaleBar().setEnabled()
            map_view.layoutController().setVerticalOffset(-0.5)
            map_view.cameraController().position = Camera.Position.Builder().setLocation(locationProvider.lastKnownLocation).build()
        }
        setupButtons()
        startNavButton.isEnabled = true
    }

    override fun onLocationUpdated(vehicleLocation: Location) {
        this.vehicleLocation = vehicleLocation
        map_view.vehicleController().setLocation(vehicleLocation)
    }

    override fun onStreetUpdated(curStreetInfo: StreetInfo, drivingOffRoad: Boolean) {
        Log.i("TAG", "onStreetUpdated: "+ curStreetInfo.streetName)
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onCandidateRoadDetected(roadCalibrator: RoadCalibrator) {
        this.roadCalibrator = roadCalibrator
        CoroutineScope(Dispatchers.Main).launch {
            val roads = roadCalibrator.getCandidateRoads()
            if (roads.isNotEmpty()){
                viewModel.candidateRoads.postValue(roads)
                showRoadsDialog()
            }else{
                Toast.makeText(activity, "Clear candidates", Toast.LENGTH_SHORT).show()
                hideRoadsDialog()
            }
        }
    }

    private fun showRoadsDialog() {
        candidateRoadDialogFragment?.dismiss()
        candidateRoadDialogFragment = JumpRoadDialogFragment()
        candidateRoadDialogFragment?.show(childFragmentManager, "CandidateRoad")
    }

    private fun hideRoadsDialog(){
        candidateRoadDialogFragment?.dismiss()
    }

    private fun selectCandidateRoad(road : CandidateRoadInfo){
        road.uuid?.let {
            roadCalibrator?.setRoad(it)
        }
    }

    override fun onDestroyView() {
        driveSession.eventHub?.removePositionEventListener(this)
        driveSession.dispose()
        super.onDestroyView()
    }

    private fun setZoomLevel(level: Float) {
        val newLevel = min(max(1f, level), 17f)
        map_view.cameraController().position = Camera.Position.Builder().setZoomLevel(newLevel).build()
    }

    private fun setupButtons() {
        iv_camera_fix.setOnClickListener {
            val newPosition = Camera.Position.Builder().setLocation(vehicleLocation).build()
            map_view.cameraController().position = newPosition
            iv_camera_fix.setImageResource(R.drawable.ic_gps_fixed_24)
        }

        iv_zoom_in.setOnClickListener {
            val currentLevel = map_view.cameraController().position!!.zoomLevel
            setZoomLevel(currentLevel - 1)
        }

        iv_zoom_out.setOnClickListener {
            val currentLevel = map_view.cameraController().position!!.zoomLevel
            setZoomLevel(currentLevel + 1)
        }

        startNavButton.setOnClickListener {
            // enableFollowVehicleMode
            map_view.cameraController().enableFollowVehicleMode(Camera.FollowVehicleMode.HeadingUp)

            activity?.runOnUiThread {
                startNavButton.isEnabled = false
                stopNavButton.isEnabled = true
            }

            job = CoroutineScope(Dispatchers.IO).launch {
                loadgps()
            }
        }

        stopNavButton.setOnClickListener {
            map_view.cameraController().disableFollowVehicle()
            CoroutineScope(Dispatchers.IO).launch {
                job?.cancelAndJoin()
            }
            activity?.runOnUiThread {
                startNavButton.isEnabled = true
                stopNavButton.isEnabled = false
            }
        }

    }

    private suspend fun loadgps() {
        val gpsfile = getResources().openRawResource(R.raw.gps2)
        val reader = BufferedReader(
                InputStreamReader(gpsfile))
        var str: String? = null
        while (true) {
            str = reader.readLine()
            if (str != null) {
                val scanner = Scanner(str).useDelimiter(",");
                var str = scanner.next()
                var lat = str.toDouble()
                str = scanner.next()
                val lon = str.toDouble()
                str = scanner.next()
                val speed = str.toFloat()
                str = scanner.next()
                val bearing = str.toFloat()
                var location = Location("test")
                location.latitude = lat
                location.longitude = lon
                location.speed = speed
                location.bearing = bearing

                locations += location
            } else {
                break
            }
        }
        gpsfile.close()
        var index = 0;
        while (true) {
            if (index >= locations.size) {
                index = 0
            }
            locationProvider.setLocation(locations[index])
            index++
            delay(250)
        }
    }
}