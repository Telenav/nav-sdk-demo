/*
 * Copyright © 2021 Telenav, Inc. All rights reserved. Telenav® is a registered trademark
 *  of Telenav, Inc.,Sunnyvale, California in the United States and may be registered in
 *  other countries. Other names may be trademarks of their respective owners.
 */

package com.telenav.sdk.demo.scenario.navigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.telenav.sdk.drivesession.NavigationSession
import com.telenav.sdk.drivesession.model.ManeuverInfo
import com.telenav.sdk.drivesession.model.NavigationEvent
import com.telenav.sdk.demo.R
import com.telenav.sdk.demo.main.TurnListItem
import com.telenav.sdk.demo.main.TurnListRecyclerViewAdapter
import com.telenav.sdk.demo.util.AndroidThreadUtils
import com.telenav.sdk.demo.util.ImageItems
import com.telenav.sdk.map.direction.model.BasicTurn
import com.telenav.sdk.map.direction.model.GuidanceLaneInfo
import java.math.BigDecimal
import java.math.RoundingMode

class TurnbyturnViewModel(val turnListAdapter: TurnListRecyclerViewAdapter) : ViewModel() {

    val timeToArrival = MutableLiveData<String>()
    val showNavigationDetails = MutableLiveData(false)
    val totalDistanceRemaining = MutableLiveData<String>()
    val tripTimeRemaining = MutableLiveData<String>()
    val distanceRemainingToNextTurn = MutableLiveData<String>()
    val turnDirectionDrawable = MutableLiveData<Int>()
    val nextTurnStreetName = MutableLiveData<String>()
    val turnListVisibility = MutableLiveData(false)

    val lanePatternCustomImages = MutableLiveData<List<ImageItems>>(listOf())
    val laneInfo = MutableLiveData<List<GuidanceLaneInfo>>(listOf())

    private var currentStepIndex = 0

    fun onNavigationEventUpdated(navEvent: NavigationEvent) {
        navEvent.travelEstToDestination?.let {
            if (!it.arrivalToStop.isNullOrEmpty()) {
                timeToArrival.postValue(it.arrivalToStop)
            }

        }

        navEvent.travelEstToDestination?.let {
            totalDistanceRemaining.postValue(getMilesOrFeet(it.distanceToStop))
            tripTimeRemaining.postValue(getTimeRemainingToArrival(it.timeToStop))
        }
        updateTurnListItem(navEvent)
        distanceRemainingToNextTurn.postValue(getMilesOrFeet(navEvent.distanceToTurn))
        navEvent.currentManeuver?.let {
            turnDirectionDrawable.postValue(getTurnDrawable(it.turnType))
            nextTurnStreetName.postValue(it.streetName)
            it.laneInfo?.let { laneInfoList -> laneInfo.postValue(laneInfoList) }
                    ?: laneInfo.postValue(listOf())
        }
    }

    private var navigationSession: NavigationSession? = null

    fun handleNavTurnList(navigationSession: NavigationSession?) {
        this.navigationSession = navigationSession
        navigationSession?.let { navSession ->
            navSession.maneuverList?.let {
                val turnList = getTurnListItem(navSession.maneuverList)
                turnListAdapter.setupData(turnList)
                turnListVisibility.postValue(turnList.isNotEmpty())
            }
        }
    }

    //private methods
    private fun updateTurnListItem(navEvent: NavigationEvent) {
        if (currentStepIndex != navEvent.stepIndex) {
            currentStepIndex = navEvent.stepIndex
            AndroidThreadUtils.runOnUiThread(Runnable {
                navigationSession?.let { navSession ->
                    navSession.maneuverList?.let {
                        val turnList = getTurnListItem(navSession.maneuverList)
                        turnListAdapter.setupData(turnList)
                        turnListVisibility.postValue(turnList.isNotEmpty())
                    }
                }
            })
        }
    }

    private fun getTurnListItem(maneuverList: List<ManeuverInfo>): List<TurnListItem> =
            maneuverList
                    .filter { it.stepIndex > currentStepIndex }
                    .map {
                        TurnListItem(
                                it.streetName,
                                getMilesOrFeet(it.lengthMeters),
                                getTurnDrawable(it.turnType)
                        )
                    }

    private fun getTimeRemainingToArrival(timeToStop: Int): String {
        return if (timeToStop > 60) {
            "${(timeToStop / 60)} min"
        } else {
            " 1 min"
        }
    }

    private fun getMilesOrFeet(distanceToStop: Double): String {
        val miles = distanceToStop * 0.00062137
        return if (miles > 0.1) {
            "${BigDecimal(miles).setScale(1, RoundingMode.HALF_EVEN)} mi"
        } else {
            "${BigDecimal(distanceToStop * 3.2808).setScale(0, RoundingMode.HALF_EVEN)} ft"
        }
    }

    private fun getTurnDrawable(turnType: Int): Int {
        return when (turnType) {
            BasicTurn.RIGHT -> R.drawable.ic_turn_right_white
            BasicTurn.LEFT -> R.drawable.ic_turn_left_white
            BasicTurn.CONTINUE -> R.drawable.ic_continue_straight
            BasicTurn.SLIGHT_LEFT -> R.drawable.ic_turn_slight_left_white
            BasicTurn.SLIGHT_RIGHT -> R.drawable.ic_turn_slight_right
            BasicTurn.STOP_RIGHT -> R.drawable.ic_stop_right
            BasicTurn.STOP_LEFT -> R.drawable.ic_stop_left
            else -> R.drawable.ic_continue_straight
        }
    }


}


