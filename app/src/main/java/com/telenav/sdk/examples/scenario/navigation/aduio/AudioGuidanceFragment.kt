/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 * of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 * other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.examples.scenario.navigation.aduio

import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.telenav.map.api.Margins
import com.telenav.sdk.common.logging.TaLog
import com.telenav.sdk.common.model.LatLon
import com.telenav.sdk.examples.scenario.navigation.BaseNavFragment
import com.telenav.sdk.drivesession.listener.AudioInstructionEventListener
import com.telenav.sdk.drivesession.model.AudioInstruction
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.drivesession.model.PositionInfo
import com.telenav.sdk.examples.R
import com.telenav.sdk.examples.databinding.ContentBasicNavigationBinding
import com.telenav.sdk.examples.databinding.FragmentAudioGuidanceBinding
import com.telenav.sdk.guidance.audio.model.AudioPromptType
import com.telenav.sdk.map.direction.DirectionClient
import com.telenav.sdk.map.direction.model.*
import java.util.*

/**
 * A simple [Fragment] for AudioGuidance
 * @author wu.changzhong on 2021/09/30
 */
class AudioGuidanceFragment : BaseNavFragment<FragmentAudioGuidanceBinding>(), AudioInstructionEventListener,
    TextToSpeech.OnInitListener, MyAudioRequestTypeRecyclerViewAdapter.OnItemClickListener {

    private val audioRequestTypeList: MutableList<AudioRequestTypeInfo> = ArrayList()
    private var tts: TextToSpeech? = null

    init {
        audioRequestTypeList.add(AudioRequestTypeInfo("OFFROAD", AudioPromptType.OFF_ROAD))
        audioRequestTypeList.add(AudioRequestTypeInfo("DEVIATION", AudioPromptType.DEVIATION))
        audioRequestTypeList.add(AudioRequestTypeInfo("CALCULATING_ROUTE", AudioPromptType.CALCULATING_ROUTE))
        audioRequestTypeList.add(AudioRequestTypeInfo("START_NAVIGATION", AudioPromptType.START_NAVIGATION))
        audioRequestTypeList.add(AudioRequestTypeInfo("CALC_ROUTE_FAILED", AudioPromptType.CALCULATE_ROUTE_FAILED))
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAudioGuidanceBinding {
        return FragmentAudioGuidanceBinding.inflate(inflater, container, false)
    }

    override fun getBaseBinding(): ContentBasicNavigationBinding {
        return binding.includeContent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val requestAudioRequestTypeList =
            view.findViewById<RecyclerView>(R.id.request_audio_request_type_list)
        driveSession.eventHub.addAudioInstructionEventListener(this)
        tts = TextToSpeech(context, this)
        // Set the adapter
        if (requestAudioRequestTypeList is RecyclerView) {
            with(requestAudioRequestTypeList) {
                layoutManager = LinearLayoutManager(context)
                adapter = MyAudioRequestTypeRecyclerViewAdapter(audioRequestTypeList)
                (adapter as MyAudioRequestTypeRecyclerViewAdapter).setOnItemClickListener(this@AudioGuidanceFragment)
            }
        }
        navigationOn.observe(viewLifecycleOwner) {
            if (it) {
                driveSession.audioGuidanceManager.requestAudioData(AudioPromptType.START_NAVIGATION)
            }
        }
    }

    override fun onItemClick(view: View, audioRequestTypeInfo: AudioRequestTypeInfo) {
        showAudioGuidanceDialog(view, audioRequestTypeInfo)
    }

    private fun showAudioGuidanceDialog(view: View, audioRequestTypeInfo: AudioRequestTypeInfo) {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(audioRequestTypeInfo.typeDesc)
        builder.setPositiveButton("ok") { dialog, _ ->
            driveSession.audioGuidanceManager.requestAudioData(audioRequestTypeInfo.typeId)
            dialog.dismiss()
        }
        builder.setNegativeButton("cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onLocationUpdated(vehicleLocation: Location, positionInfo: PositionInfo) {
        super.onLocationUpdated(vehicleLocation, positionInfo)
        if (positionInfo.isOffRoad()) {
            driveSession.audioGuidanceManager.requestAudioData(AudioPromptType.OFF_ROAD)
        }
    }

    override fun onAudioInstructionUpdated(audioInstruction: AudioInstruction) {
        tts?.speak(audioInstruction.audioOrthographyString, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                TaLog.e("TTS", "not supported")
            }
        } else {
            TaLog.e("TTS", "initialization failed")
        }
    }

    override fun requestDirection(
        begin: Location,
        end: Location,
        wayPointList: MutableList<Waypoint>?
    ) {
        Log.d(
            "MapLogsForTestData",
            "MapLogsForTestData >>>> requestDirection begin: $begin + end $end"
        )

        val request: RouteRequest = RouteRequest.Builder(
            GeoLocation(begin),
            GeoLocation(LatLon(end.latitude, end.longitude))
        ).contentLevel(ContentLevel.FULL)
            .routeCount(2)
            .stopPoints(wayPointList)
            .build()
        val task = DirectionClient.Factory.hybridClient()
            .createRoutingTask(request, RequestMode.CLOUD_ONLY)
        driveSession.audioGuidanceManager.requestAudioData(AudioPromptType.CALCULATING_ROUTE)
        task.runAsync { response ->
            Log.d(
                TAG,
                "MapLogsForTestData >>>> requestDirection task status: ${response.response.status}"
            )
            if (response.response.status == DirectionErrorCode.OK && response.response.result.isNotEmpty()) {
                routes = response.response.result
                routeIds = binding.includeContent.mapView.routesController().add(routes).toMutableList()
                binding.includeContent.mapView.routesController().highlight(routeIds[0])
                val region = binding.includeContent.mapView.routesController().region(routeIds)
                binding.includeContent.mapView.cameraController().showRegion(region, Margins.Percentages(0.20, 0.20))
                highlightedRouteId = routeIds[0]
                activity?.runOnUiThread {
                    binding.includeContent.navButton.isEnabled = true
                    binding.includeContent.navButton.setText(R.string.start_navigation)
                }

            } else {
                activity?.runOnUiThread {
                    binding.includeContent.navButton.isEnabled = false
                }

                driveSession.audioGuidanceManager.requestAudioData(AudioPromptType.CALCULATE_ROUTE_FAILED)
            }
            task.dispose()
        }
    }

    override fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        super.onNavigationEventUpdated(navEvent)
        val deviated = navEvent.deviated
        if (deviated) {
            driveSession.audioGuidanceManager.requestAudioData(AudioPromptType.DEVIATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTTS()
    }

    fun stopTTS() {
        tts?.stop()
    }
}